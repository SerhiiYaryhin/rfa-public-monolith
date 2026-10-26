'use strict';

/**
 * ============================================================================
 * 🎛️ efir.js — Логіка панелі керування ефіром + ModeratorAudio (v7)
 * ============================================================================
 */

// ============================================================================
// 📌 ГЛОБАЛЬНІ ЗМІННІ СТАНУ
// ============================================================================

var currentRoomType = 'main';
var selectedParticipant = null;
var participantsCache = {};
var subBatchParticipants = null;
var subPresenceUpdate = null;
var subWaitingGuests = null;


// ============================================================================
// 🎧 ModeratorAudio — Одна активна аудіокімната модератора
// ============================================================================

/**
 * Контролер аудіо-підключення модератора до кімнат Janus.
 *
 * Архітектурні принципи:
 *   - ТІЛЬКИ ОДНА активна аудіокімната за раз
 *   - Повністю переиспользовує наявні модулі:
 *     JanusConnection (токен/сесія) + RoomManager (join/SDP/ICE)
 *   - Перемикач: компактна іконка 🎧 в рядку кімнати
 *   - Незалежний від текстового чату
 *   - Для Broadcast: мікрофон зам'ючений за замовчуванням
 */
const ModeratorAudio = {

    /** Тип кімнати, яку зараз слухає модератор (або null) */
    activeRoomType: null,

    /** Стан мікрофона модератора */
    isMuted: true,

    /**
     * 🎧 Toggle прослуховування кімнати.
     * Логіка:
     *   - Ця ж кімната активна → від'єднання
     *   - Інша кімната активна → перехід (RoomManager.switchRoom автоматично виходить)
     *   - Нічого не активне → приєднання
     */
    async toggleListening(roomType) {
        if (this.activeRoomType === roomType) {
            await this.stopListening();
        } else {
            await this.startListening(roomType);
        }
    },

    /**
     * Приєднання до аудіокімнати через наявні модулі.
     */
    async startListening(roomType) {
        const janusId = (typeof janusRoomIds !== 'undefined') ? janusRoomIds[roomType] : null;
        if (!janusId) {
            console.warn(`⚠️ Janus Room ID не знайдено для "${roomType}"`);
            this.updateStatus(roomType, 'error', 'Кімната не налаштована');
            return;
        }

        console.log(`🎧 Підключення до ${roomType} (Janus ID: ${janusId})...`);
        this.updateIcon(roomType, 'connecting');
        this.updateStatus(roomType, 'connecting', 'Підключення...');

        try {
            if (typeof JanusConnection === 'undefined' || typeof RoomManager === 'undefined') {
                throw new Error('WebRTC модулі не завантажені');
            }

            // Крок 1: Токен
            if (!JanusConnection.currentSessionToken) {
                const ok = await JanusConnection.initUserSession();
                if (!ok) throw new Error('Не вдалося отримати токен');
            }

            // Крок 2: Сесія + AudioBridge
            if (!JanusConnection.isJanusReady || !JanusConnection.audioBridge) {
                await JanusConnection.initJanusConnection();
            }

            // Крок 3: Вхід у кімнату через RoomManager
            // switchRoom() автоматично виходить з попередньої кімнати
            await RoomManager.switchRoom(janusId);

            // Крок 4: Для Broadcast — авто-мьют
            this.isMuted = (roomType === 'broadcast');
            if (this.isMuted) {
                setTimeout(() => {
                    if (JanusConnection.audioBridge) {
                        JanusConnection.audioBridge.send({
                            message: { request: 'configure', muted: true }
                        });
                    }
                }, 500);
            }

            // Скидаємо UI попередньої кімнати
            if (this.activeRoomType && this.activeRoomType !== roomType) {
                this.updateIcon(this.activeRoomType, 'inactive');
                this.hideAudioPanel(this.activeRoomType);
            }

            this.activeRoomType = roomType;
            this.updateIcon(roomType, 'active');
            this.updateStatus(roomType, 'connected', '✅ Підключено');
            this.showAudioPanel(roomType);
            this.updateMicButton(roomType);

            console.log(`✅ Модератор підключено до ${roomType}`);

        } catch (error) {
            console.error(`❌ Помилка підключення до ${roomType}:`, error);
            this.updateIcon(roomType, 'inactive');
            this.updateStatus(roomType, 'error', '❌ ' + error.message);
            this.hideAudioPanel(roomType);
        }
    },

    /**
     * ⏹ Від'єднання від поточної аудіокімнати.
     */
    async stopListening() {
        if (!this.activeRoomType) return;
        const roomType = this.activeRoomType;
        console.log(`⏹ Відключення від ${roomType}...`);

        if (typeof NetworkMonitor !== 'undefined') {
            NetworkMonitor.stopIceMonitoring();
        }
        if (typeof JanusConnection !== 'undefined' && JanusConnection.audioBridge) {
            try { JanusConnection.audioBridge.detach(); } catch (e) { console.warn('Detach:', e); }
            JanusConnection.audioBridge = null;
            JanusConnection.isJanusReady = false;
        }
        if (typeof AudioMonitor !== 'undefined') AudioMonitor.stop();

        // ✅ Скидаємо currentRoomId для коректного наступного switchRoom
        if (typeof RoomManager !== 'undefined') {
            RoomManager.currentRoomId = null;
        }

        this.updateIcon(roomType, 'inactive');
        this.updateStatus(roomType, 'disconnected', 'Не підключено');
        this.hideAudioPanel(roomType);
        this.activeRoomType = null;
        this.isMuted = true;

        console.log(`✅ Відключено від ${roomType}`);
    },

    /**
     * 🎤 Toggle мікрофона модератора
     */
    toggleMic(roomType) {
        if (typeof JanusConnection === 'undefined' || !JanusConnection.audioBridge) {
            console.warn('AudioBridge не готовий');
            return;
        }
        this.isMuted = !this.isMuted;
        JanusConnection.audioBridge.send({
            message: { request: 'configure', muted: this.isMuted }
        });
        this.updateMicButton(roomType);
        console.log(`🎙️ Мікрофон модератора: ${this.isMuted ? 'OFF' : 'ON'}`);
    },

    // ================================================================
    // UI HELPERS
    // ================================================================

    /** Оновлює іконку аудіо в рядку кімнати */
    updateIcon(roomType, state) {
        const btn = document.getElementById(`btn-audio-${roomType}`);
        if (!btn) return;
        btn.className = 'btn-audio-icon';
        if (state === 'active') {
            btn.classList.add('active');
            btn.title = 'Від\'єднатися від аудіокімнати';
            btn.innerHTML = '<i class="fas fa-headphones"></i>';
        } else if (state === 'connecting') {
            btn.classList.add('connecting');
            btn.title = 'Підключення...';
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        } else {
            btn.title = 'Слухати аудіокімнату';
            btn.innerHTML = '<i class="fas fa-headphones"></i>';
        }
    },

    /** Оновлює кнопку мікрофона в аудіо-панелі */
    updateMicButton(roomType) {
        const btn = document.getElementById(`btn-mic-${roomType}`);
        if (!btn) return;
        if (this.isMuted) {
            btn.className = 'btn-mic btn-mic-off';
            btn.innerHTML = '<i class="fas fa-microphone-slash"></i>';
            btn.title = 'Увімкнути мікрофон';
        } else {
            btn.className = 'btn-mic btn-mic-on';
            btn.innerHTML = '<i class="fas fa-microphone"></i>';
            btn.title = 'Вимкнути мікрофон';
        }
    },

    showAudioPanel(roomType) {
        const panel = document.getElementById(`audioPanel-${roomType}`);
        if (panel) panel.classList.add('listening');
    },

    hideAudioPanel(roomType) {
        const panel = document.getElementById(`audioPanel-${roomType}`);
        if (panel) panel.classList.remove('listening');
    },

    updateStatus(roomType, state, text) {
        const statusEl = document.getElementById(`status-${roomType}`);
        if (!statusEl) return;
        statusEl.textContent = text;
        statusEl.className = 'listen-status';
        if (state === 'connected') statusEl.classList.add('connected');
        if (state === 'error') statusEl.classList.add('error');
    },

    updateVuMeter(roomType, level) {
        const fill = document.getElementById(`vu-${roomType}`);
        if (fill) fill.style.width = Math.min(level, 100) + '%';
    }
};

window.ModeratorAudio = ModeratorAudio;

window.addEventListener('beforeunload', () => {
    if (ModeratorAudio.activeRoomType) {
        ModeratorAudio.stopListening();
    }
});


// ============================================================================
// 🔌 STOMP-ПІДПИСКИ МОДЕРАТОРА
// ============================================================================

function setupModeratorSubscriptions() {
    const trySubscribe = () => {
        if (!stompClient || !stompClient.connected) return false;

        if (!subBatchParticipants) {
            subBatchParticipants = stompClient.subscribe(
                '/topic/chat.rooms.batch.' + curuuid,
                onBatchParticipantsReceived
            );
        }

        if (!subPresenceUpdate) {
            subPresenceUpdate = stompClient.subscribe(
                '/topic/efir.room.presence',
                () => {
                    console.log('🔔 Presence update received, requesting batch...');
                    setTimeout(requestBatchParticipants, 150);
                }
            );
        }

        if (!subWaitingGuests) {
            subWaitingGuests = stompClient.subscribe(
                `/topic/efir.${stationUuid}.waiting`,
                onWaitingGuestsUpdate
            );
        }

        console.log('✅ Moderator subscriptions established');
        return true;
    };

    if (!trySubscribe()) {
        const interval = setInterval(() => {
            if (trySubscribe()) clearInterval(interval);
        }, 500);
        setTimeout(() => clearInterval(interval), 30000);
    }

    setInterval(() => {
        if (stompClient && stompClient.connected) {
            requestBatchParticipants();
        }
    }, 10000);
}

function requestBatchParticipants() {
    if (!stompClient || !stompClient.connected) return;
    const roomUuids = Object.values(rooms).filter(uuid => uuid && uuid !== 'null');
    if (roomUuids.length === 0) return;
    stompClient.send("/app/rooms/batch-participants", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({ roomUuids: roomUuids })
    }));
}

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

function updateRoomCount(roomType, participants) {
    const countEl = document.getElementById(
        `count${roomType.charAt(0).toUpperCase() + roomType.slice(1)}`
    );
    if (countEl) countEl.textContent = participants.length;
}


// ============================================================================
// 👥 РЕНДЕРИНГ УЧАСНИКІВ
// ============================================================================

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
            </div>`;
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
            </button>`;
        grid.appendChild(chip);
    });
}

function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}


// ============================================================================
// 🙋 ЧЕРГА ОЧІКУВАННЯ
// ============================================================================

function onWaitingGuestsUpdate(payload) {
    try {
        const guests = JSON.parse(payload.body);
        renderWaitingGuests(guests);
    } catch (e) {
        console.error('Error parsing waiting guests:', e);
    }
}

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
                <button class="btn-waiting btn-accept"
                        onclick="acceptGuest('${g.uuid}')" title="Запросити в ефір">
                    <i class="fas fa-check"></i>
                </button>
                <button class="btn-waiting btn-decline"
                        onclick="declineGuest('${g.uuid}')" title="Відхилити">
                    <i class="fas fa-times"></i>
                </button>
            </div>`;
        list.appendChild(item);
    });
}


// ============================================================================
// 🎯 ДІЇ МОДЕРАТОРА
// ============================================================================

function showChipMenu(event, userUuid, userName) {
    event.stopPropagation();
    selectedParticipant = { uuid: userUuid, name: userName };
    const menu = document.getElementById('contextMenu');
    menu.style.display = 'block';
    const rect = event.currentTarget.getBoundingClientRect();
    menu.style.left = `${rect.left}px`;
    menu.style.top = `${rect.bottom + 5}px`;
}

function acceptGuest(userUuid) {
    if (!rooms.green) {
        alert('Green Room не створено. Натисніть "Підготувати ефір".');
        return;
    }
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/hand/lower", {}, JSON.stringify({
            uuid: curuuid,
            body: JSON.stringify({
                stationUuid: stationUuid,
                targetUuid: userUuid,
                reason: 'ACCEPTED'
            })
        }));
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

function contextAction(action) {
    if (!selectedParticipant) return;
    alert(`Дія "${action}" для ${selectedParticipant.name} буде реалізована`);
    document.getElementById('contextMenu').style.display = 'none';
}


// ============================================================================
// 🎬 КЕРУВАННЯ ЕФІРОМ
// ============================================================================

function prepareEfir() {
    const btn = document.getElementById('btnPrepare');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Підготовка...';
    fetch(`/api/v1/broadcast/prepare/${stationUuid}`, { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            if (data.success) location.reload();
            else {
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

function startEfir() {
    document.getElementById('liveIndicator').classList.add('active');
    document.getElementById('liveStatus').textContent = 'LIVE';
}

function pauseEfir() { /* TODO */ }

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
// 🚀 ІНІЦІАЛІЗАЦІЯ
// ============================================================================

document.addEventListener('DOMContentLoaded', () => {

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

    document.addEventListener('click', (e) => {
        if (picker) picker.classList.remove('visible');
        if (!e.target.closest('.context-menu') && !e.target.closest('.participant-chip-menu')) {
            document.getElementById('contextMenu').style.display = 'none';
        }
    });

    setupModeratorSubscriptions();

    setTimeout(() => { requestBatchParticipants(); }, 1500);
});