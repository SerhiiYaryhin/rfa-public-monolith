'use strict';

/**
 * ============================================================================
 * 🎛️ efir.js — Логіка панелі керування ефіром (Command Center)
 * ============================================================================
 *
 * Цей модуль відповідає за:
 *   1. STOMP-підписки модератора (batch-participants, presence, waiting guests)
 *   2. Рендеринг учасників та черги очікування
 *   3. Дії модератора: прийняти/відхилити гостя, контекстне меню
 *   4. Керування ефіром: підготувати, запустити, пауза, стоп
 *   5. ✅ ModeratorAudio — listen-only підключення до Janus AudioBridge
 *
 * ⚠️ ЗАЛЕЖНОСТІ (мають бути завантажені ДО цього файлу):
 *   - sockjs.min.js, stomp.js           → STOMP-клієнт
 *   - janus.js (CDN)                    → Бібліотека Janus Gateway
 *   - /js/webrtc/v2/*.js                → WebRTC модулі
 *   - /js/chat.js                       → Текстовий чат (sendpublic, selectroom тощо)
 *
 * ГЛОБАЛЬНІ ЗМІННІ THYMELEAF (передаються з сервера через inline-скрипт):
 *   - curuuid       : UUID поточного модератора
 *   - curusername   : Відображуване ім'я модератора
 *   - curroom       : UUID основної текстової кімнати
 *   - stationUuid   : UUID станції
 *   - rooms         : Об'єкт {main, green, broadcast, staff} → UUID кімнат
 *   - janusRoomIds  : Об'єкт {main, green, broadcast, staff} → числові ID Janus
 */

// ============================================================================
// 📌 ГЛОБАЛЬНІ ЗМІННІ СТАНУ
// ============================================================================

/** @type {string} Поточний тип активної кімнати (main/green/broadcast/staff) */
var currentRoomType = 'main';

/** @type {string|null} UUID обраного користувача для контекстного меню */
var selectedParticipant = null;

/** @type {Object} Кеш учасників по кімнатах: { roomUuid: [participants] } */
var participantsCache = {};

// --- Підписки STOMP ---
/** @type {Stomp.Subscription|null} */
var subBatchParticipants = null;
/** @type {Stomp.Subscription|null} */
var subPresenceUpdate = null;
/** @type {Stomp.Subscription|null} */
var subWaitingGuests = null;


// ============================================================================
// 🎧 ModeratorAudio — Listen-only підключення до Janus AudioBridge
// ============================================================================

/**
 * Контролер аудіо-підключення модератора.
 *
 * На відміну від гостьового MainRoomAudio/GreenRoomUI:
 *   - Не автозапускається при завантаженні
 *   - Підтримує ОДНЕ активне підключення (перемикається між кімнатами)
 *   - Listen-only режим (без мікрофона)
 *   - Вбудований UI в картки кімнат (не модалка)
 */
const ModeratorAudio = {

    /** Поточна активна кімната (тип: main/green/broadcast/staff) */
    activeRoomType: null,

    /** Чи ініціалізовано Janus session */
    isJanusInitialized: false,

    /**
     * 🔊 Почати прослуховування кімнати.
     * @param {string} roomType - Тип кімнати: 'main', 'green', 'broadcast', 'staff'
     */
    async startListening(roomType) {
        // Отримуємо числовий Janus Room ID з Thymeleaf-змінної
        const janusId = (typeof janusRoomIds !== 'undefined') ? janusRoomIds[roomType] : null;

        if (!janusId) {
            console.warn(`⚠️ Janus Room ID не знайдено для типу "${roomType}"`);
            this.updateStatus(roomType, 'error', 'Кімната не налаштована');
            return;
        }

        // Якщо вже слухаємо цю ж кімнату — нічого не робимо
        if (this.activeRoomType === roomType && this.isJanusInitialized) {
            console.log(`ℹ️ Вже слухаємо ${roomType}`);
            return;
        }

        // Якщо слухаємо іншу кімнату — спочатку відключаємося
        if (this.activeRoomType && this.activeRoomType !== roomType) {
            await this.stopListening(this.activeRoomType);
        }

        console.log(`🎧 Підключення до ${roomType} (Janus ID: ${janusId})...`);
        this.updateStatus(roomType, 'connecting', 'Підключення...');
        this.showAudioPanel(roomType);

        try {
            // Перевіряємо доступність WebRTC модулів
            if (typeof JanusConnection === 'undefined') {
                throw new Error('WebRTC модулі не завантажені');
            }

            // Отримуємо токен авторизації
            if (!JanusConnection.currentSessionToken) {
                const tokenReady = await JanusConnection.initUserSession();
                if (!tokenReady) throw new Error('Не вдалося отримати токен');
            }

            // Встановлюємо WebSocket-з'єднання з Janus
            if (!JanusConnection.isJanusReady) {
                await JanusConnection.initJanusConnection();
            }

            // Перемикаємося в аудіокімнату через RoomManager
            if (typeof RoomManager !== 'undefined') {
                await RoomManager.switchRoom(janusId);
            }

            this.activeRoomType = roomType;
            this.isJanusInitialized = true;
            this.updateStatus(roomType, 'connected', '✅ Підключено');
            this.updateListenButton(roomType, true);

            // Запускаємо ICE моніторинг
            if (typeof NetworkMonitor !== 'undefined') {
                NetworkMonitor.startIceMonitoring();
            }

            console.log(`✅ Модератор підключено до ${roomType} (Janus ID: ${janusId})`);

        } catch (error) {
            console.error(`❌ Помилка підключення до ${roomType}:`, error);
            this.updateStatus(roomType, 'error', '❌ ' + error.message);
            this.updateListenButton(roomType, false);
        }
    },

    /**
     * ⏹ Зупинити прослуховування кімнати.
     * @param {string} roomType - Тип кімнати
     */
    async stopListening(roomType) {
        if (!roomType) return;

        console.log(`⏹ Відключення від ${roomType}...`);

        // Зупиняємо ICE моніторинг
        if (typeof NetworkMonitor !== 'undefined') {
            NetworkMonitor.stopIceMonitoring();
        }

        // Detach AudioBridge plugin
        if (typeof JanusConnection !== 'undefined' && JanusConnection.audioBridge) {
            try {
                JanusConnection.audioBridge.detach();
            } catch (e) {
                console.warn('Detach error:', e);
            }
            JanusConnection.audioBridge = null;
            JanusConnection.isJanusReady = false;
        }

        // Зупиняємо AudioMonitor
        if (typeof AudioMonitor !== 'undefined') {
            AudioMonitor.stop();
        }

        this.updateStatus(roomType, 'disconnected', 'Не підключено');
        this.updateListenButton(roomType, false);
        this.hideAudioPanel(roomType);

        this.activeRoomType = null;
        this.isJanusInitialized = false;

        console.log(`✅ Відключено від ${roomType}`);
    },

    // ================================================================
    // UI HELPERS
    // ================================================================

    /** Показати аудіо-панель для кімнати */
    showAudioPanel(roomType) {
        const panel = document.getElementById(`audioPanel-${roomType}`);
        if (panel) panel.classList.add('listening');
    },

    /** Приховати аудіо-панель для кімнати */
    hideAudioPanel(roomType) {
        const panel = document.getElementById(`audioPanel-${roomType}`);
        if (panel) panel.classList.remove('listening');
    },

    /** Оновити текст статусу підключення */
    updateStatus(roomType, state, text) {
        const statusEl = document.getElementById(`status-${roomType}`);
        if (!statusEl) return;
        statusEl.textContent = text;
        statusEl.className = 'listen-status';
        if (state === 'connected') statusEl.classList.add('connected');
        if (state === 'error') statusEl.classList.add('error');
    },

    /** Оновити кнопку Слухати/Стоп */
    updateListenButton(roomType, isListening) {
        const panel = document.getElementById(`audioPanel-${roomType}`);
        if (!panel) return;
        const btn = panel.querySelector('.btn-listen');
        if (!btn) return;

        if (isListening) {
            btn.className = 'btn-listen btn-listen-stop';
            btn.innerHTML = '<i class="fas fa-stop"></i> Стоп';
            btn.onclick = () => ModeratorAudio.stopListening(roomType);
        } else {
            btn.className = 'btn-listen btn-listen-start';
            btn.innerHTML = '<i class="fas fa-headphones"></i> Слухати';
            btn.onclick = () => ModeratorAudio.startListening(roomType);
        }
    },

    /**
     * Оновлює VU-метр для конкретної кімнати.
     * @param {string} roomType - Тип кімнати
     * @param {number} level - Рівень звуку 0-100
     */
    updateVuMeter(roomType, level) {
        const fill = document.getElementById(`vu-${roomType}`);
        if (fill) {
            fill.style.width = Math.min(level, 100) + '%';
        }
    }
};

// Експорт у глобальну область
window.ModeratorAudio = ModeratorAudio;


// ============================================================================
// 🔌 STOMP-ПІДПИСКИ МОДЕРАТОРА
// ============================================================================

/**
 * Налаштовує STOMP-підписки специфічні для панелі модератора.
 * Викликається з DOMContentLoaded.
 * Має retry-логіку на випадок, якщо STOMP ще не підключений.
 */
function setupModeratorSubscriptions() {
    const trySubscribe = () => {
        if (!stompClient || !stompClient.connected) return false;

        // Підписка на batch-participants (список учасників усіх кімнат)
        if (!subBatchParticipants) {
            subBatchParticipants = stompClient.subscribe(
                '/topic/chat.rooms.batch.' + curuuid,
                onBatchParticipantsReceived
            );
        }

        // Підписка на presence-update (вхід/вихід учасників)
        if (!subPresenceUpdate) {
            subPresenceUpdate = stompClient.subscribe(
                '/topic/efir.room.presence',
                () => {
                    console.log('🔔 Presence update received, requesting batch...');
                    setTimeout(requestBatchParticipants, 150);
                }
            );
        }

        // Підписка на чергу очікування (hand raise)
        if (!subWaitingGuests) {
            subWaitingGuests = stompClient.subscribe(
                `/topic/efir.${stationUuid}.waiting`,
                onWaitingGuestsUpdate
            );
        }

        console.log('✅ Moderator subscriptions established');
        return true;
    };

    // Retry-логіка: намагаємося підписатися кожні 500мс протягом 30с
    if (!trySubscribe()) {
        const interval = setInterval(() => {
            if (trySubscribe()) clearInterval(interval);
        }, 500);
        setTimeout(() => clearInterval(interval), 30000);
    }

    // Періодичне оновлення учасників (кожні 10 секунд)
    setInterval(() => {
        if (stompClient && stompClient.connected) {
            requestBatchParticipants();
        }
    }, 10000);
}

/**
 * Надсилає запит на отримання учасників усіх кімнат станції.
 */
function requestBatchParticipants() {
    if (!stompClient || !stompClient.connected) return;

    const roomUuids = Object.values(rooms).filter(uuid => uuid && uuid !== 'null');
    if (roomUuids.length === 0) return;

    stompClient.send("/app/rooms/batch-participants", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({ roomUuids: roomUuids })
    }));
}

/**
 * Обробник відповіді batch-participants.
 * Оновлює кеш та лічильники кімнат.
 */
function onBatchParticipantsReceived(payload) {
    try {
        const data = JSON.parse(payload.body);
        const batchData = JSON.parse(data.body);

        participantsCache = batchData;

        updateRoomCount('main', batchData[rooms.main] || []);
        updateRoomCount('green', batchData[rooms.green] || []);
        updateRoomCount('broadcast', batchData[rooms.broadcast] || []);
        updateRoomCount('staff', batchData[rooms.staff] || []);

        renderActiveRoomParticipants();

    } catch (e) {
        console.error('Error parsing batch participants:', e);
    }
}

/**
 * Оновлює лічильник учасників у картці кімнати.
 */
function updateRoomCount(roomType, participants) {
    const countEl = document.getElementById(`count${roomType.charAt(0).toUpperCase() + roomType.slice(1)}`);
    if (countEl) countEl.textContent = participants.length;
}


// ============================================================================
// 👥 РЕНДЕРИНГ УЧАСНИКІВ
// ============================================================================

/**
 * Рендерить сітку учасників активної кімнати.
 */
function renderActiveRoomParticipants() {
    const grid = document.getElementById('participantGrid');
    const activeCountBadge = document.getElementById('activeRoomCount');
    if (!grid) return;

    const activeRoomUuid = rooms[currentRoomType];
    const participants = (activeRoomUuid && participantsCache[activeRoomUuid]) || [];

    if (activeCountBadge) activeCountBadge.textContent = participants.length;

    if (participants.length === 0) {
        grid.innerHTML = `
            <div class="participants-empty" style="grid-column: 1 / -1;">
                <i class="fas fa-user-slash"></i>
                Немає учасників
            </div>
        `;
        return;
    }

    grid.innerHTML = '';
    participants.forEach(u => {
        const chip = document.createElement('div');
        chip.className = 'participant-chip';
        chip.dataset.uuid = u.uuid;

        const initials = u.name
            ? u.name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2)
            : '??';

        chip.innerHTML = `
            <div class="participant-chip-avatar">${initials}</div>
            <div class="participant-chip-name" title="${u.name}">${u.name}</div>
            <button class="participant-chip-menu" 
                    onclick="event.stopPropagation(); showChipMenu(event, '${u.uuid}', '${escapeHtml(u.name)}')">
                <i class="fas fa-ellipsis-v"></i>
            </button>
        `;
        grid.appendChild(chip);
    });
}

/**
 * Безпечне екранування HTML-символів для вставки в onclick.
 */
function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}


// ============================================================================
// 🙋 ЧЕРГА ОЧІКУВАННЯ (WAITING GUESTS)
// ============================================================================

/**
 * Обробник оновлення черги очікування.
 */
function onWaitingGuestsUpdate(payload) {
    try {
        const guests = JSON.parse(payload.body);
        renderWaitingGuests(guests);
    } catch (e) {
        console.error('Error parsing waiting guests:', e);
    }
}

/**
 * Рендерить список гостей, які підняли руку.
 */
function renderWaitingGuests(guests) {
    const list = document.getElementById('waitingList');
    const section = document.getElementById('waitingSection');
    const countBadge = document.getElementById('waitingCount');

    if (!list || !section) return;

    if (countBadge) countBadge.textContent = guests.length;

    if (guests.length === 0) {
        section.style.display = 'none';
        return;
    }

    section.style.display = 'flex';
    list.innerHTML = '';

    guests.forEach(g => {
        const item = document.createElement('div');
        item.className = 'waiting-item';
        item.innerHTML = `
            <i class="fas fa-hand-paper" style="color: var(--warning); flex-shrink: 0;"></i>
            <div class="waiting-item-name" title="${g.name}">${g.name}</div>
            <div class="waiting-actions">
                <button class="btn-waiting btn-accept" onclick="acceptGuest('${g.uuid}')" title="Запросити в ефір">
                    <i class="fas fa-check"></i>
                </button>
                <button class="btn-waiting btn-decline" onclick="declineGuest('${g.uuid}')" title="Відхилити">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;
        list.appendChild(item);
    });
}


// ============================================================================
// 🎯 ДІЇ МОДЕРАТОРА
// ============================================================================

/**
 * Показує контекстне меню біля учасника.
 */
function showChipMenu(event, userUuid, userName) {
    event.stopPropagation();
    selectedParticipant = { uuid: userUuid, name: userName };
    const menu = document.getElementById('contextMenu');
    menu.style.display = 'block';

    const rect = event.currentTarget.getBoundingClientRect();
    menu.style.left = `${rect.left}px`;
    menu.style.top = `${rect.bottom + 5}px`;
}

/**
 * Прийняти гостя — переміщення в Green Room + опускання руки з причиною ACCEPTED.
 */
function acceptGuest(userUuid) {
    if (!rooms.green) {
        alert('Green Room не створено. Натисніть "Підготувати ефір".');
        return;
    }
    if (stompClient && stompClient.connected) {
        // 1. Опускаємо руку з причиною ACCEPTED
        stompClient.send("/app/hand/lower", {}, JSON.stringify({
            uuid: curuuid,
            body: JSON.stringify({
                stationUuid: stationUuid,
                targetUuid: userUuid,
                reason: 'ACCEPTED'
            })
        }));

        // 2. Переміщуємо в Green Room
        stompClient.send("/app/room/move", {}, JSON.stringify({
            uuid: curuuid,
            roomuuid: rooms.green,
            body: JSON.stringify({
                targetUserUuid: userUuid,
                autoMute: false
            })
        }));

        console.log(`✅ Гість ${userUuid} прийнятий в Green Room`);
    }
}

/**
 * Відхилити гостя — опускання руки з причиною DECLINED.
 */
function declineGuest(userUuid) {
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/hand/lower", {}, JSON.stringify({
            uuid: curuuid,
            body: JSON.stringify({
                stationUuid: stationUuid,
                targetUuid: userUuid,
                reason: 'DECLINED'
            })
        }));
        console.log(`❌ Гість ${userUuid} відхилений`);
    }
}

/**
 * Виконання дії з контекстного меню учасника.
 */
function contextAction(action) {
    if (!selectedParticipant) return;
    alert(`Дія "${action}" для ${selectedParticipant.name} буде реалізована`);
    document.getElementById('contextMenu').style.display = 'none';
}


// ============================================================================
// 🎬 КЕРУВАННЯ ЕФІРОМ
// ============================================================================

/**
 * Підготовка ефіру: створення службових кімнат.
 */
function prepareEfir() {
    const btn = document.getElementById('btnPrepare');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Підготовка...';

    fetch(`/api/v1/broadcast/prepare/${stationUuid}`, { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                location.reload();
            } else {
                alert('Помилка: ' + data.error);
                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-cog"></i> Підготувати ефір';
            }
        })
        .catch(err => {
            alert('Помилка мережі: ' + err.message);
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-cog"></i> Підготувати ефір';
        });
}

/** Запустити ефір */
function startEfir() {
    document.getElementById('liveIndicator').classList.add('active');
    document.getElementById('liveStatus').textContent = 'LIVE';
}

/** Пауза ефіру */
function pauseEfir() {
    // TODO: Реалізувати паузу
}

/** Зупинити ефір */
function stopEfir() {
    document.getElementById('liveIndicator').classList.remove('active');
    document.getElementById('liveStatus').textContent = 'OFFLINE';
}


// ============================================================================
// 🌍 ЕКСПОРТ У ГЛОБАЛЬНУ ОБЛАСТЬ ВИДИМОСТІ
// ============================================================================

window.setupModeratorSubscriptions = setupModeratorSubscriptions;
window.requestBatchParticipants = requestBatchParticipants;
window.renderActiveRoomParticipants = renderActiveRoomParticipants;
window.acceptGuest = acceptGuest;
window.declineGuest = declineGuest;
window.showChipMenu = showChipMenu;
window.contextAction = contextAction;
window.prepareEfir = prepareEfir;
window.startEfir = startEfir;
window.pauseEfir = pauseEfir;
window.stopEfir = stopEfir;
window.escapeHtml = escapeHtml;


// ============================================================================
// 🚀 ІНІЦІАЛІЗАЦІЯ ПРИ ЗАВАНТАЖЕННІ СТОРІНКИ
// ============================================================================

document.addEventListener('DOMContentLoaded', () => {

    // --- Кліки по картках кімнат (перемикання текстового чату) ---
    document.querySelectorAll('.room-item').forEach(item => {
        item.addEventListener('click', function() {
            const roomType = this.dataset.roomType;
            const roomUuid = this.dataset.roomUuid;
            const roomName = this.dataset.roomName;

            if (roomType && roomUuid) {
                document.querySelectorAll('.room-item').forEach(el => el.classList.remove('active'));
                this.classList.add('active');
                document.getElementById('chatTitle').innerText = roomName || 'Чат';
                currentRoomType = roomType;

                if (typeof selectroom === 'function') {
                    selectroom(roomUuid, roomName);
                }
                renderActiveRoomParticipants();
            }
        });
    });

    // --- Emoji Picker ---
    const picker = document.getElementById('picker');
    const input = document.getElementById('newmessage');
    const emojiToggle = document.getElementById('emojiToggle');

    if (emojiToggle) {
        emojiToggle.onclick = (e) => { e.stopPropagation(); picker.classList.toggle('visible'); };
    }
    if (picker) {
        picker.addEventListener('emoji-click', event => {
            input.value += event.detail.unicode;
            picker.classList.remove('visible');
            input.focus();
        });
    }

    // --- Закриття popup при кліку поза ними ---
    document.addEventListener('click', (e) => {
        if (picker) picker.classList.remove('visible');
        if (!e.target.closest('.context-menu') && !e.target.closest('.participant-chip-menu')) {
            document.getElementById('contextMenu').style.display = 'none';
        }
    });

    // --- STOMP-підписки модератора ---
    setupModeratorSubscriptions();

    // --- Початковий запит учасників (з затримкою для STOMP) ---
    setTimeout(() => {
        requestBatchParticipants();
    }, 1500);
});