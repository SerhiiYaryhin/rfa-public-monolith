'use strict';

/**
 * ============================================================================
 * 🎛️ efir.js — Логіка панелі керування ефіром + ModeratorAudio
 * ============================================================================
 *
 * Цей модуль відповідає за:
 *   1. STOMP-підписки модератора (batch-participants, presence, waiting guests)
 *   2. Рендеринг учасників та черги очікування
 *   3. Дії модератора: прийняти/відхилити гостя, контекстне меню
 *   4. Керування ефіром: підготувати, запустити, пауза, стоп
 *   5. ✅ ModeratorAudio — підключення модератора до кількох Janus кімнат
 *
 * ⚠️ ЗАЛЕЖНОСТІ (мають бути завантажені ДО цього файлу):
 *   - sockjs.min.js, stomp.js
 *   - janus.js (CDN) + webrtc-adapter
 *   - /js/webrtc/v2/*.js (WebRTC модулі)
 *   - /js/chat.js (текстовий чат)
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

/** @type {Object|null} UUID обраного користувача для контекстного меню */
var selectedParticipant = null;

/** @type {Object} Кеш учасників по кімнатах: { roomUuid: [participants] } */
var participantsCache = {};

// --- Підписки STOMP ---
var subBatchParticipants = null;
var subPresenceUpdate = null;
var subWaitingGuests = null;


// ============================================================================
// 🎧 ModeratorAudio — Multi-room контролер підключення модератора до Janus
// ============================================================================

/**
 * Контролер аудіо-підключення модератора до кімнат Janus.
 *
 * Унікальні особливості (порівняно з MainRoomAudio/GreenRoomUI гостя):
 *   - Підтримує КІЛЬКА одночасних підключень (кожна кімната = окрема сесія)
 *   - Мікрофон вимкнено за замовчуванням (модератор сам вирішує, коли говорити)
 *   - НЕ використовує RoomManager.switchRoom() (бо той робить detach інших)
 *   - Створює НЕЗАЛЕЖНУ сесію Janus для кожної кімнати через new Janus({...})
 *   - Вбудований UI в картки кімнат (не блокуюча модалка)
 *
 * ⚠️ ЧОМУ НЕ JanusConnection.session.attach():
 *   Модуль janus-connection.js НЕ експортує сирий об'єкт сесії як
 *   публічну властивість. Тому використовуємо глобальний Janus API напряму.
 */
const ModeratorAudio = {

    /**
     * Мапа активних підключень:
     *   roomType → { handle, janusSession, janusRoomId, audioElement, isMuted }
     * @type {Object<string, Object>}
     */
    activeConnections: {},

    /**
     * 🔊 Почати прослуховування кімнати модератором.
     *
     * Режим: audioSend: true + muted: true
     *   - Мікрофон технічно підключений (можна швидко увімкнути)
     *   - Але вимкнений за замовчуванням (модератор не заважає ефіру)
     *
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

        // Якщо вже слухаємо цю кімнату — нічого не робимо
        if (this.activeConnections[roomType]) {
            console.log(`ℹ️ Вже слухаємо ${roomType}`);
            return;
        }

        console.log(`🎧 Підключення до ${roomType} (Janus ID: ${janusId})...`);
        this.updateStatus(roomType, 'connecting', 'Підключення...');
        this.showAudioPanel(roomType);

        try {
            // Перевіряємо доступність глобальної бібліотеки Janus (CDN)
            if (typeof Janus === 'undefined') {
                throw new Error('Бібліотека Janus не завантажена');
            }

            // Перевіряємо доступність JanusConnection (для токена)
            if (typeof JanusConnection === 'undefined') {
                throw new Error('WebRTC модулі не завантажені');
            }

            // Отримуємо токен авторизації (якщо ще не отриманий)
            if (!JanusConnection.currentSessionToken) {
                const tokenReady = await JanusConnection.initUserSession();
                if (!tokenReady) throw new Error('Не вдалося отримати токен');
            }

            // ✅ Створюємо НЕЗАЛЕЖНУ сесію + handle для цієї кімнати
            // Кожна кімната = окрема сесія = справжній multi-room
            const result = await this._createModeratorHandle(janusId, roomType);

            // Зберігаємо підключення в мапу
            this.activeConnections[roomType] = {
                handle: result.pluginHandle,
                janusSession: result.janusSession,
                janusRoomId: janusId,
                audioElement: null,
                isMuted: true  // Мікрофон вимкнено за замовчуванням
            };

            this.updateStatus(roomType, 'connected', '✅ Підключено');
            this.updateListenButton(roomType, true);

            console.log(`✅ Модератор підключено до ${roomType} (Janus ID: ${janusId})`);

        } catch (error) {
            console.error(`❌ Помилка підключення до ${roomType}:`, error);
            this.updateStatus(roomType, 'error', '❌ ' + error.message);
            this.updateListenButton(roomType, false);
            this.hideAudioPanel(roomType);
        }
    },

    /**
     * ⏹ Зупинити прослуховування конкретної кімнати.
     * Інші активні підключення залишаються живими.
     *
     * @param {string} roomType - Тип кімнати
     */
    stopListening(roomType) {
        const conn = this.activeConnections[roomType];
        if (!conn) return;

        console.log(`⏹ Відключення від ${roomType}...`);

        // Detach plugin handle
        if (conn.handle) {
            try {
                conn.handle.detach();
            } catch (e) {
                console.warn(`Detach error for ${roomType}:`, e);
            }
        }

        // ✅ Знищуємо НЕЗАЛЕЖНУ сесію Janus
        if (conn.janusSession) {
            try {
                conn.janusSession.destroy();
            } catch (e) {
                console.warn(`Session destroy error for ${roomType}:`, e);
            }
        }

        // Видаляємо аудіо-елемент з DOM
        if (conn.audioElement) {
            try {
                conn.audioElement.pause();
                conn.audioElement.srcObject = null;
                conn.audioElement.remove();
            } catch (e) {
                console.warn(`Audio element cleanup error for ${roomType}:`, e);
            }
        }

        // Видаляємо з мапи активних підключень
        delete this.activeConnections[roomType];

        this.updateStatus(roomType, 'disconnected', 'Не підключено');
        this.updateListenButton(roomType, false);
        this.hideAudioPanel(roomType);
        this.updateVuMeter(roomType, 0);

        console.log(`✅ Відключено від ${roomType}`);
    },

    /**
     * ⏹ Зупинити ВСІ активні підключення.
     * Викликається при закритті сторінки або перенавігації.
     */
    stopAll() {
        const roomTypes = Object.keys(this.activeConnections);
        console.log(`⏹ Зупинка всіх підключень (${roomTypes.length})...`);
        roomTypes.forEach(rt => this.stopListening(rt));
    },

    /**
     * 🎤 Увімкнути/вимкнути мікрофон модератора в конкретній кімнаті.
     * Використовується, коли модератор хоче щось сказати гостям.
     *
     * @param {string} roomType - Тип кімнати
     */
    toggleMic(roomType) {
        const conn = this.activeConnections[roomType];
        if (!conn || !conn.handle) {
            console.warn(`⚠️ Немає активного підключення до ${roomType}`);
            return;
        }

        conn.isMuted = !conn.isMuted;

        // Надсилаємо команду зміни стану мікрофона в Janus
        conn.handle.send({
            message: { request: 'configure', muted: conn.isMuted }
        });

        // Оновлюємо UI кнопки мікрофона
        this.updateMicButton(roomType, conn.isMuted);

        console.log(`🎙️ Мікрофон модератора в ${roomType}: ${conn.isMuted ? 'OFF' : 'ON'}`);
    },

    /**
     * Створює незалежну сесію Janus + AudioBridge plugin handle для модератора.
     *
     * ✅ ВИКОРИСТОВУЄ ГЛОБАЛЬНИЙ Janus API (new Janus({...})), а не
     *    JanusConnection.session.attach(), оскільки останній недоступний.
     *
     * @param {number} janusRoomId - Числовий ID кімнати Janus
     * @param {string} roomType - Тип кімнати (для логування)
     * @returns {Promise<{pluginHandle: Object, janusSession: Object}>}
     * @private
     */
    async _createModeratorHandle(janusRoomId, roomType) {
        return new Promise((resolve, reject) => {

            // Визначаємо URL Janus WebSocket сервера
            const serverUrl = (typeof CONFIG !== 'undefined' && CONFIG.JANUS_WS_URL)
                ? CONFIG.JANUS_WS_URL
                : `wss://${window.location.hostname}/janus-ws`;

            // Отримуємо токен (вже має бути зареєстрований на бекенді)
            const token = JanusConnection.currentSessionToken;
            if (!token) {
                reject(new Error('Токен Janus не отримано'));
                return;
            }

            console.log(`🔌 Створення нової сесії Janus для ${roomType}...`);

            // Створюємо НЕЗАЛЕЖНУ сесію для цієї кімнати
            const janusSession = new Janus({
                server: serverUrl,
                token: token,

                success: () => {
                    console.log(`✅ Сесія створена для ${roomType}`);

                    // Приєднуємо AudioBridge плагін до цієї сесії
                    janusSession.attach({
                        plugin: 'janus.plugin.audiobridge',
                        opaqueId: `moderator-${roomType}-${Date.now()}`,

                        success: (pluginHandle) => {
                            console.log(`🔌 Handle створено для ${roomType}: ${pluginHandle.getId()}`);

                            // Надсилаємо запит на вхід у кімнату
                            // muted: true — мікрофон вимкнено за замовчуванням
                            // Модератор може увімкнути його через toggleMic()
                            pluginHandle.send({
                                message: {
                                    request: 'join',
                                    room: janusRoomId,
                                    display: `🎛️ Модератор ${curusername || ''}`.trim(),
                                    muted: true
                                },
                                success: () => {
                                    console.log(`✅ Join надіслано для ${roomType}`);
                                },
                                error: (err) => {
                                    console.error(`❌ Join error для ${roomType}:`, err);
                                }
                            });

                            resolve({ pluginHandle, janusSession });
                        },

                        error: (cause) => {
                            console.error(`❌ Помилка створення handle для ${roomType}:`, cause);
                            try { janusSession.destroy(); } catch (e) { /* ignore */ }
                            reject(new Error(cause));
                        },

                        onmessage: (msg, jsep) => {
                            const event = msg.audiobridge;

                            if (msg.error_code || msg.error) {
                                console.error(`❌ Помилка від Janus (${roomType}):`,
                                    msg.error_code, msg.error);
                                return;
                            }

                            if (event === 'joined') {
                                console.log(`✅ Модератор увійшов в кімнату ${roomType}`);
                                // Якщо разом з joined не прийшов JSEP — створюємо offer
                                if (!jsep) {
                                    pluginHandle.createOffer({
                                        media: { audioSend: true, audioRecv: true, video: false },
                                        success: (localJsep) => {
                                            pluginHandle.send({
                                                message: { request: 'configure', muted: true },
                                                jsep: localJsep
                                            });
                                        },
                                        error: (err) => {
                                            console.error(`❌ Offer error (${roomType}):`, err);
                                        }
                                    });
                                }
                            } else if (event === 'talking') {
                                // Хтось говорить у кімнаті — можна оновити VU-метр
                                ModeratorAudio.updateVuMeter(roomType, 60);
                            } else if (event === 'stopped_talking') {
                                ModeratorAudio.updateVuMeter(roomType, 10);
                            } else if (event === 'kicked') {
                                console.warn(`⚠️ Модератора видалено з кімнати ${roomType}`);
                                ModeratorAudio.stopListening(roomType);
                            }

                            // Обробка SDP (JSEP handshake)
                            if (jsep) {
                                if (jsep.type === 'answer') {
                                    pluginHandle.handleRemoteJsep({
                                        jsep: jsep,
                                        success: () => {
                                            console.log(`✅ SDP handshake завершено для ${roomType}`);
                                        },
                                        error: (err) => {
                                            console.error(`❌ SDP error для ${roomType}:`, err);
                                        }
                                    });
                                } else if (jsep.type === 'offer') {
                                    pluginHandle.createAnswer({
                                        jsep: jsep,
                                        media: { audioSend: true, audioRecv: true, video: false },
                                        success: (answerJsep) => {
                                            pluginHandle.send({
                                                message: { request: 'configure', muted: true },
                                                jsep: answerJsep
                                            });
                                        },
                                        error: (err) => {
                                            console.error(`❌ CreateAnswer error (${roomType}):`, err);
                                        }
                                    });
                                }
                            }
                        },

                        onlocalstream: (stream) => {
                            // Локальний стрім модератору не потрібен (не граємо себе)
                        },

                        onremotestream: (stream) => {
                            // ✅ Отримуємо аудіо-потік від кімнати
                            const audioEl = document.createElement('audio');
                            audioEl.autoplay = true;
                            audioEl.muted = false;
                            audioEl.srcObject = stream;
                            audioEl.style.display = 'none';
                            document.body.appendChild(audioEl);

                            // Зберігаємо посилання для cleanup
                            if (ModeratorAudio.activeConnections[roomType]) {
                                ModeratorAudio.activeConnections[roomType].audioElement = audioEl;
                            }

                            console.log(`🔊 Remote stream отримано для ${roomType}`);
                        },

                        oncleanup: () => {
                            console.log(`🧹 Cleanup handle для ${roomType}`);
                            const conn = ModeratorAudio.activeConnections[roomType];
                            if (conn && conn.audioElement) {
                                conn.audioElement.pause();
                                conn.audioElement.srcObject = null;
                                conn.audioElement.remove();
                            }
                        }
                    });
                },

                error: (cause) => {
                    console.error(`❌ Помилка створення сесії Janus для ${roomType}:`, cause);
                    reject(new Error(cause));
                }
            });
        });
    },

    // ================================================================
    // UI HELPERS
    // ================================================================

    /** Показати аудіо-панель (з VU-метром і статусом) для кімнати */
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

    /**
     * Оновлює зовнішню кнопку Слухати/Стоп.
     * ✅ Кнопка знаходиться ПОЗА прихованою панеллю, тому шукаємо за ID.
     */
    updateListenButton(roomType, isListening) {
        const btn = document.getElementById(`btn-listen-${roomType}`);
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
     * Оновлює кнопку мікрофона в аудіо-панелі.
     * @param {string} roomType - Тип кімнати
     * @param {boolean} isMuted - Стан мікрофона
     */
    updateMicButton(roomType, isMuted) {
        const btn = document.getElementById(`btn-mic-${roomType}`);
        if (!btn) return;

        if (isMuted) {
            btn.className = 'btn-mic btn-mic-off';
            btn.innerHTML = '<i class="fas fa-microphone-slash"></i> Мікрофон OFF';
        } else {
            btn.className = 'btn-mic btn-mic-on';
            btn.innerHTML = '<i class="fas fa-microphone"></i> Мікрофон ON';
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

// Експорт у глобальну область видимості
window.ModeratorAudio = ModeratorAudio;

// Cleanup при закритті сторінки
window.addEventListener('beforeunload', () => {
    ModeratorAudio.stopAll();
});


// ============================================================================
// 🔌 STOMP-ПІДПИСКИ МОДЕРАТОРА
// ============================================================================

/**
 * Налаштовує STOMP-підписки специфічні для панелі модератора.
 * Має retry-логіку на випадок, якщо STOMP ще не підключений.
 */
function setupModeratorSubscriptions() {
    const trySubscribe = () => {
        if (!stompClient || !stompClient.connected) return false;

        // Підписка на учасників усіх кімнат (оновлюється при змінах)
        if (!subBatchParticipants) {
            subBatchParticipants = stompClient.subscribe(
                '/topic/chat.rooms.batch.' + curuuid,
                onBatchParticipantsReceived
            );
        }

        // Підписка на події присутності (вхід/вихід учасників)
        if (!subPresenceUpdate) {
            subPresenceUpdate = stompClient.subscribe(
                '/topic/efir.room.presence',
                () => {
                    console.log('🔔 Presence update received, requesting batch...');
                    setTimeout(requestBatchParticipants, 150);
                }
            );
        }

        // Підписка на чергу очікування (гості, що підняли руку)
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

/** Надсилає запит на отримання учасників усіх кімнат станції */
function requestBatchParticipants() {
    if (!stompClient || !stompClient.connected) return;

    const roomUuids = Object.values(rooms).filter(uuid => uuid && uuid !== 'null');
    if (roomUuids.length === 0) return;

    stompClient.send("/app/rooms/batch-participants", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({ roomUuids: roomUuids })
    }));
}

/** Обробник відповіді batch-participants. Оновлює кеш та лічильники */
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

/** Оновлює лічильник учасників у картці кімнати */
function updateRoomCount(roomType, participants) {
    const countEl = document.getElementById(
        `count${roomType.charAt(0).toUpperCase() + roomType.slice(1)}`
    );
    if (countEl) countEl.textContent = participants.length;
}


// ============================================================================
// 👥 РЕНДЕРИНГ УЧАСНИКІВ
// ============================================================================

/** Рендерить сітку учасників активної кімнати */
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

/** Безпечне екранування для вставки в onclick-атрибути */
function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}


// ============================================================================
// 🙋 ЧЕРГА ОЧІКУВАННЯ (WAITING GUESTS)
// ============================================================================

/** Обробник оновлення черги очікування */
function onWaitingGuestsUpdate(payload) {
    try {
        const guests = JSON.parse(payload.body);
        renderWaitingGuests(guests);
    } catch (e) {
        console.error('Error parsing waiting guests:', e);
    }
}

/** Рендерить список гостей, які підняли руку */
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

/** Показує контекстне меню біля учасника */
function showChipMenu(event, userUuid, userName) {
    event.stopPropagation();
    selectedParticipant = { uuid: userUuid, name: userName };
    const menu = document.getElementById('contextMenu');
    menu.style.display = 'block';

    const rect = event.currentTarget.getBoundingClientRect();
    menu.style.left = `${rect.left}px`;
    menu.style.top = `${rect.bottom + 5}px`;
}

/** Прийняти гостя: опускаємо руку з причиною ACCEPTED + переміщуємо в Green Room */
function acceptGuest(userUuid) {
    if (!rooms.green) {
        alert('Green Room не створено. Натисніть "Підготувати ефір".');
        return;
    }
    if (stompClient && stompClient.connected) {
        // 1. Опускаємо руку з причиною ACCEPTED (сповістить гостя)
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

/** Відхилити гостя: опускаємо руку з причиною DECLINED */
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

/** Виконання дії з контекстного меню учасника */
function contextAction(action) {
    if (!selectedParticipant) return;
    alert(`Дія "${action}" для ${selectedParticipant.name} буде реалізована`);
    document.getElementById('contextMenu').style.display = 'none';
}


// ============================================================================
// 🎬 КЕРУВАННЯ ЕФІРОМ
// ============================================================================

/** Підготовка ефіру: створення службових кімнат */
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

/** Запустити ефір (оновлення індикатора) */
function startEfir() {
    document.getElementById('liveIndicator').classList.add('active');
    document.getElementById('liveStatus').textContent = 'LIVE';
}

/** Пауза ефіру */
function pauseEfir() { /* TODO */ }

/** Зупинити ефір (оновлення індикатора) */
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

    // Кліки по картках кімнат (перемикання текстового чату)
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

    // Emoji Picker
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

    // Закриття popup при кліку поза ними
    document.addEventListener('click', (e) => {
        if (picker) picker.classList.remove('visible');
        if (!e.target.closest('.context-menu') && !e.target.closest('.participant-chip-menu')) {
            document.getElementById('contextMenu').style.display = 'none';
        }
    });

    // STOMP-підписки модератора
    setupModeratorSubscriptions();

    // Початковий запит учасників (з затримкою для встановлення STOMP)
    setTimeout(() => { requestBatchParticipants(); }, 1500);
});