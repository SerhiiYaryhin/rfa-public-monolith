/**
 * ============================================================================
 * 📡 messenger.js — Керування аудіо-підключенням для чату гостя
 * ============================================================================
 *
 * Цей модуль містить два ключових об'єкти:
 *
 * 1. MainRoomAudio — автоматичне підключення до Janus AudioBridge головної
 *    кімнати станції при завантаженні сторінки /chat. Працює тільки для
 *    кімнат типу STATION/GREEN/BROADCAST/STAFF. Публічні текстові кімнати
 *    (CHATROOM_TYPE_PUBLIC) ігноруються.
 *
 * 2. GreenRoomUI — керування модальним вікном Green Room / Broadcast.
 *    Відкривається, коли модератор переміщує гостя через STOMP-команду
 *    MOVE_YOURSELF. Блокує інтерфейс, підключає Janus AudioBridge до
 *    цільової кімнати, дозволяє керувати мікрофоном та повернутися назад.
 *
 * ⚠️ ВАЖЛИВО: Цей файл має бути підключений ПЕРЕД chat.js у messenger.html,
 *    бо chat.js використовує GreenRoomUI.enter() у обробнику onEfirControl().
 *
 * Залежності (мають бути завантажені ДО цього файлу):
 *   - janus.js (бібліотека Janus Gateway)
 *   - config.js, logger.js, janus-connection.js, room-manager.js,
 *     websocket-signaling.js, audio-monitor.js, network-monitor.js
 *
 * Глобальні змінні Thymeleaf (передаються з сервера):
 *   - curuuid        : UUID поточного користувача
 *   - curroom        : UUID поточної текстової кімнати
 *   - stationUuid    : UUID станції (для підписок hand.status)
 *   - curRoomType    : Тип кімнати (EChatRoomType.name)
 *   - curRoomJanusId : Числовий ID кімнати Janus (= MessageRoom.id)
 */

// ============================================================================
// 🏠 MainRoomAudio — Автопідключення до головної кімнати станції
// ============================================================================

const MainRoomAudio = {

    /**
     * Прапорець стану: чи вже ініціалізовано підключення до Janus.
     * Запобігає повторному підключенню при виклику init() кілька разів.
     * @type {boolean}
     */
    isInitialized: false,

    /**
     * 🚀 Головна точка входу. Викликається з DOMContentLoaded у messenger.html.
     *
     * Логіка:
     *   1. Перевіряє тип кімнати через curRoomType (Thymeleaf-змінна).
     *      Якщо CHATROOM_TYPE_PUBLIC → пропускає (тільки текст).
     *   2. Перевіряє наявність curRoomJanusId (числовий ID для Janus).
     *   3. Ініціалізує Janus WS сесію, отримує токен, підключає AudioBridge.
     *   4. Перемикається в аудіокімнату через RoomManager.switchRoom().
     *   5. Запускає ICE-моніторинг для виявлення втрати зв'язку.
     *
     * @returns {Promise<void>}
     */
    async init() {
        // Крок 1: Визначаємо, чи потребує ця кімната WebRTC
        const needsWebRtc = this._roomNeedsWebRtc(curRoomType);
        if (!needsWebRtc) {
            console.log(`📝 Кімната типу "${curRoomType}" — тільки текстовий чат. Janus не активується.`);
            return;
        }

        // Крок 2: Перевіряємо наявність числового Janus Room ID
        if (!curRoomJanusId) {
            console.warn(`⚠️ Кімната типу "${curRoomType}" потребує WebRTC, але curRoomJanusId не переданий з сервера.`);
            return;
        }

        // Крок 3: Запобігаємо повторній ініціалізації
        if (this.isInitialized) {
            console.log('ℹ️ MainRoomAudio вже ініціалізовано. Пропускаємо.');
            return;
        }
        this.isInitialized = true;

        console.log(`🎙️ Активуємо Janus для кімнати типу "${curRoomType}" (Janus ID: ${curRoomJanusId}, Station: ${stationUuid})`);

        try {
            // Крок 4: Перевіряємо доступність WebRTC модулів
            // (може бути undefined, якщо JS-файли не завантажилися через MIME-type)
            if (typeof JanusConnection === 'undefined') {
                console.warn('⚠️ JanusConnection не завантажено. Пропускаємо автопідключення.');
                return;
            }

            // Крок 5: Отримуємо токен авторизації для Janus
            // Токен = UUID користувача, реєструється на бекенді через /api/v1q/webrtc/stream/token
            if (!JanusConnection.currentSessionToken) {
                const tokenReady = await JanusConnection.initUserSession();
                if (!tokenReady) throw new Error('Не вдалося отримати токен від бекенду');
            }

            // Крок 6: Встановлюємо WebSocket-з'єднання з Janus Gateway
            if (!JanusConnection.isJanusReady) {
                await JanusConnection.initJanusConnection();
            }

            // Крок 7: Підключаємо STOMP-сигналізацію для команд модератора
            // (switch_room, mute тощо через /topic/webrtc.control.{userUuid})
            if (typeof WebSocketSignaling !== 'undefined' && !WebSocketSignaling.isConnected) {
                WebSocketSignaling.connect(JanusConnection.currentSessionToken);
            }

            // Крок 8: Перемикаємося в аудіокімнату головної кімнати станції
            // RoomManager.switchRoom() викликає REST /connectclienttoroom,
            // який евакуює користувача з інших кімнат Janus і повертає display name
            if (typeof RoomManager !== 'undefined') {
                await RoomManager.switchRoom(curRoomJanusId);
            }

            // Крок 9: Запускаємо постійний ICE-моніторинг
            // Перевіряє стан PeerConnection кожні 3 сек, автоматично
            // виконує ICE restart при переході WiFi ↔ 4G
            if (typeof NetworkMonitor !== 'undefined') {
                NetworkMonitor.startIceMonitoring();
            }

            console.log(`✅ Janus AudioBridge активовано для кімнати ${curRoomJanusId}`);

        } catch (error) {
            console.error('❌ Помилка автопідключення до Janus:', error);
            // Не блокуємо роботу текстового чату навіть якщо Janus не підключився
        }
    },

    /**
     * Визначає, чи потребує кімната WebRTC на основі її типу.
     *
     * Публічні кімнати (CHATROOM_TYPE_PUBLIC) — тільки текстовий чат.
     * Всі інші типи (STATION, GREEN, BROADCAST, STAFF) — текст + аудіо.
     *
     * @param {string} roomType - Назва типу кімнати з EChatRoomType enum
     * @returns {boolean} true якщо потрібен Janus, false якщо тільки текст
     * @private
     */
    _roomNeedsWebRtc(roomType) {
        if (!roomType) return false;
        const textOnlyTypes = ['CHATROOM_TYPE_PUBLIC'];
        return !textOnlyTypes.includes(roomType);
    },

    /**
     * 🔄 Повторне підключення до головної кімнати Janus.
     *
     * Викликається з GreenRoomUI.leaveRoom(), коли гість повертається
     * з Green Room / Broadcast назад у головну кімнату.
     *
     * На відміну від init():
     *   - НЕ перевіряє isInitialized (бо ми хочемо перепідключитися)
     *   - НЕ вимагає повторного отримання токена (він вже є)
     *   - Примусово викликає RoomManager.switchRoom(curRoomJanusId)
     *
     * @returns {Promise<void>}
     */
    async reconnectToMainRoom() {
        // Перевіряємо, чи головна кімната взагалі потребує WebRTC
        const needsWebRtc = this._roomNeedsWebRtc(curRoomType);
        if (!needsWebRtc || !curRoomJanusId) {
            console.log('📝 Головна кімната не потребує WebRTC або curRoomJanusId відсутній.');
            return;
        }

        console.log(`🔄 Перепідключення до Janus головної кімнати (ID: ${curRoomJanusId})...`);

        try {
            if (typeof JanusConnection === 'undefined') {
                console.warn('⚠️ JanusConnection не доступний для перепідключення.');
                return;
            }

            // Токен вже має бути отриманий під час першого init(),
            // але перевіримо для безпеки
            if (!JanusConnection.currentSessionToken) {
                const tokenReady = await JanusConnection.initUserSession();
                if (!tokenReady) throw new Error('Не вдалося отримати токен при перепідключенні');
            }

            // Janus WS може бути ще живий після GreenRoomUI.leaveRoom()
            // (leaveRoom робить detach плагіна, але не знищує сесію).
            // Якщо ні — перепідключаємо.
            if (!JanusConnection.isJanusReady) {
                await JanusConnection.initJanusConnection();
            }

            // Перемикаємося назад у головну аудіокімнату
            if (typeof RoomManager !== 'undefined') {
                await RoomManager.switchRoom(curRoomJanusId);
            }

            // Відновлюємо ICE-моніторинг
            if (typeof NetworkMonitor !== 'undefined') {
                NetworkMonitor.startIceMonitoring();
            }

            console.log(`✅ Перепідключено до Janus головної кімнати ${curRoomJanusId}`);

        } catch (error) {
            console.error('❌ Помилка перепідключення до Janus:', error);
        }
    },

    /**
     * 🔌 Повне відключення від Janus AudioBridge.
     *
     * Викликається при:
     *   - Закритті сторінки (beforeunload)
     *   - Переході на іншу сторінку порталу
     *
     * Очищає всі ресурси: PeerConnection, AudioContext, ICE monitoring.
     */
    disconnect() {
        if (!this.isInitialized) return;

        console.log('🔌 Відключення від Janus AudioBridge (MainRoomAudio)...');

        // Зупиняємо ICE-моніторинг
        if (typeof NetworkMonitor !== 'undefined') {
            NetworkMonitor.stopIceMonitoring();
        }

        // Detach AudioBridge плагіна (закриває PeerConnection)
        if (typeof JanusConnection !== 'undefined' && JanusConnection.audioBridge) {
            try {
                JanusConnection.audioBridge.detach();
            } catch (e) {
                console.warn('Detach error:', e);
            }
            JanusConnection.audioBridge = null;
            JanusConnection.isJanusReady = false;
        }

        // Зупиняємо візуалізацію аудіо
        if (typeof AudioMonitor !== 'undefined') {
            AudioMonitor.stop();
        }

        this.isInitialized = false;
    }
};

// Експорт у глобальну область видимості
// (потрібен для виклику з inline-скриптів messenger.html та з chat.js)
window.MainRoomAudio = MainRoomAudio;


// ============================================================================
// 🎙️ GreenRoomUI — Модальне вікно Green Room / Broadcast
// ============================================================================

const GreenRoomUI = {

    /**
     * Чи відкрите зараз модальне вікно.
     * Запобігає повторному відкриттю при дублюванні STOMP-команд.
     * @type {boolean}
     */
    isActive: false,

    /**
     * Числовий ID Janus-кімнати, до якої зараз підключений гість.
     * @type {number|null}
     */
    currentJanusRoomId: null,

    /**
     * Стан мікрофона в модальному вікні.
     * true = вимкнено (muted), false = увімкнено.
     * @type {boolean}
     */
    isMuted: false,

    /**
     * UUID текстової кімнати, де гість був ДО переміщення в Green Room.
     * Зберігається в enter() і використовується в leaveRoom() для повернення.
     *
     * ⚠️ КРИТИЧНО: Має бути збережений ДО виклику selectroom(),
     * бо selectroom() змінює глобальну змінну curroom!
     *
     * @type {string|null}
     */
    previousChatRoom: null,

    /**
     * 🚪 Відкрити модальне вікно та підключитися до Janus AudioBridge.
     *
     * Викликається з chat.js → onEfirControl(MOVE_YOURSELF).
     *
     * Порядок дій:
     *   1. Зберігає previousChatRoom (ПОКИ curroom ще = головна кімната!)
     *   2. Показує модальне вікно з анімацією
     *   3. Ініціалізує Janus (якщо ще не готовий)
     *   4. Перемикається в цільову Janus-кімнату через RoomManager
     *   5. Якщо autoMute=true — вимикає мікрофон через Janus API
     *   6. Запускає ICE-моніторинг
     *
     * @param {number} janusRoomId - Числовий ID цільової Janus-кімнати
     * @param {string} roomName    - Відображуване ім'я ("🟡 Green Room" / "🔴 Прямий ефір")
     * @param {boolean} autoMute   - Чи вимкнути мікрофон автоматично (true для Broadcast)
     * @returns {Promise<void>}
     */
    async enter(janusRoomId, roomName, autoMute) {
        // Запобігаємо повторному відкриттю
        if (this.isActive) {
            console.warn('⚠️ GreenRoomUI вже активний. Ігноруємо повторний виклик.');
            return;
        }

        this.isActive = true;
        this.currentJanusRoomId = janusRoomId;
        this.isMuted = !!autoMute;

        // ✅ КРОК 1: Зберігаємо ПОПЕРЕДНЮ текстову кімнату
        // Це МАЄ відбутися ДО selectroom(), який змінює curroom!
        // curroom тут ще містить UUID головної кімнати станції.
        this.previousChatRoom = (typeof curroom !== 'undefined') ? curroom : null;
        console.log(`💾 Збережено previousChatRoom: ${this.previousChatRoom}`);

        // ✅ КРОК 2: Оновлюємо UI модального вікна
        const overlay = document.getElementById('greenRoomOverlay');
        const title = document.getElementById('grTitle');
        const subtitle = document.getElementById('grSubtitle');
        const status = document.getElementById('grStatus');

        if (overlay) overlay.classList.add('active');
        if (title) title.textContent = roomName || '🎙️ Аудіокімната';
        if (subtitle) {
            subtitle.textContent = autoMute
                ? 'Ви в прямому ефірі. Мікрофон вимкнено. Модератор надасть вам слово.'
                : 'Підготовка до ефіру. Перевірте мікрофон.';
        }
        if (status) {
            status.textContent = '⏳ Підключення до аудіокімнати...';
            status.className = 'gr-status';
        }

        this.updateMicButton();

        // ✅ КРОК 3: Перевіряємо доступність WebRTC модулів
        if (typeof JanusConnection === 'undefined') {
            if (status) {
                status.textContent = '⚠️ WebRTC модулі не завантажені. Аудіо недоступне.';
                status.className = 'gr-status error';
            }
            console.warn('JanusConnection is not defined. WebRTC modules not loaded.');
            return;
        }

        try {
            // ✅ КРОК 4: Ініціалізуємо Janus (якщо ще не ініціалізовано)
            if (!JanusConnection.currentSessionToken) {
                const tokenReady = await JanusConnection.initUserSession();
                if (!tokenReady) throw new Error('Не вдалося отримати токен');
            }

            if (!JanusConnection.isJanusReady) {
                await JanusConnection.initJanusConnection();
            }

            // ✅ КРОК 5: Підключаємо WebSocket сигналізацію для команд модератора
            if (typeof WebSocketSignaling !== 'undefined' && !WebSocketSignaling.isConnected) {
                WebSocketSignaling.connect(JanusConnection.currentSessionToken);
            }

            // Оновлюємо статус у модалці
            if (status) {
                status.textContent = '✅ Підключено до аудіокімнати';
                status.className = 'gr-status connected';
            }

            // ✅ КРОК 6: Перемикаємося в потрібну Janus-кімнату
            // RoomManager.switchRoom() викличе REST /connectclienttoroom,
            // який евакуює користувача з попередньої кімнати Janus
            if (typeof RoomManager !== 'undefined') {
                await RoomManager.switchRoom(janusRoomId);
            }

            // ✅ КРОК 7: Якщо autoMute — вимикаємо мікрофон через Janus API
            if (autoMute && JanusConnection.audioBridge) {
                JanusConnection.audioBridge.send({
                    message: { request: 'configure', muted: true }
                });
                this.isMuted = true;
                this.updateMicButton();
            }

            // ✅ КРОК 8: Запускаємо ICE моніторинг
            if (typeof NetworkMonitor !== 'undefined') {
                NetworkMonitor.startIceMonitoring();
            }

        } catch (error) {
            console.error('GreenRoomUI enter error:', error);
            if (status) {
                status.textContent = '❌ Помилка підключення: ' + error.message;
                status.className = 'gr-status error';
            }
        }
    },

    /**
     * 🚪 Залишити аудіокімнату та повернутися в текстовий чат.
     *
     * Викликається при натисканні кнопки "Повернутися в чат" у модалці.
     *
     * Порядок дій:
     *   1. Зупиняє ICE моніторинг
     *   2. Detach AudioBridge плагіна (закриває PeerConnection)
     *   3. Зупиняє AudioMonitor (візуалізацію)
     *   4. Закриває модальне вікно
     *   5. Перемикає текстовий чат на previousChatRoom (головна кімната)
     *   6. ✅ Перепідключає Janus до головної кімнати через MainRoomAudio.reconnectToMainRoom()
     *   7. Показує сповіщення
     */
    leaveRoom() {
        if (!this.isActive) return;

        console.log('🚪 Вихід з аудіокімнати Green Room / Broadcast...');

        // Крок 1: Зупиняємо ICE моніторинг
        if (typeof NetworkMonitor !== 'undefined') {
            NetworkMonitor.stopIceMonitoring();
        }

        // Крок 2: Detach AudioBridge плагіна
        if (typeof JanusConnection !== 'undefined' && JanusConnection.audioBridge) {
            try {
                JanusConnection.audioBridge.detach();
            } catch (e) {
                console.warn('Detach error:', e);
            }
            JanusConnection.audioBridge = null;
            JanusConnection.isJanusReady = false;
        }

        // Крок 3: Зупиняємо візуалізацію аудіо
        if (typeof AudioMonitor !== 'undefined') {
            AudioMonitor.stop();
        }

        // Крок 4: Закриваємо модальне вікно
        const overlay = document.getElementById('greenRoomOverlay');
        if (overlay) overlay.classList.remove('active');

        this.isActive = false;
        this.currentJanusRoomId = null;

        // Крок 5: Повертаємося в ПОПЕРЕДНЮ текстову кімнату
        // previousChatRoom було збережено в enter() ДО зміни curroom
        const targetRoom = this.previousChatRoom;
        if (targetRoom && typeof selectroom === 'function') {
            console.log(`↩️ Повернення в текстову кімнату: ${targetRoom}`);
            selectroom(targetRoom, 'Основна');
        } else {
            console.warn('⚠️ previousChatRoom не збережено або selectroom недоступний');
        }

        // Крок 6: ✅ Перепідключаємо Janus до головної кімнати
        // Щоб гість продовжував чути ефір/спілкування після повернення
        if (typeof MainRoomAudio !== 'undefined') {
            MainRoomAudio.reconnectToMainRoom();
        }

        // Крок 7: Сповіщення
        if (typeof showEfirNotification === 'function') {
            showEfirNotification('Ви повернулися в текстовий чат', 'info');
        }
    },

    /**
     * 🎤 Toggle мікрофона в аудіокімнаті.
     *
     * Надсилає команду configure/muted через Janus AudioBridge plugin.
     * Це змінює стан мікрофона НА СТОРОНІ JANUS (не локально в браузері).
     * Модератор також бачить цей стан у панелі керування.
     */
    toggleMic() {
        if (typeof JanusConnection === 'undefined' || !JanusConnection.audioBridge) {
            console.warn('AudioBridge not ready — неможливо переключити мікрофон');
            return;
        }

        this.isMuted = !this.isMuted;

        // Надсилаємо команду мутування в Janus
        JanusConnection.audioBridge.send({
            message: { request: 'configure', muted: this.isMuted }
        });

        this.updateMicButton();
        console.log(`🎙️ Mic ${this.isMuted ? 'OFF 🔇' : 'ON 🔊'}`);
    },

    /**
     * 🎨 Оновлює вигляд кнопки мікрофона в модальному вікні.
     *
     * Зелена кнопка + іконка мікрофона = увімкнено
     * Червона кнопка + іконка перекресленого мікрофона = вимкнено
     */
    updateMicButton() {
        const btn = document.getElementById('grMicBtn');
        if (!btn) return;

        if (this.isMuted) {
            btn.className = 'gr-btn gr-btn-mic-off';
            btn.innerHTML = '<i class="fas fa-microphone-slash"></i> <span id="grMicText">Мікрофон OFF</span>';
        } else {
            btn.className = 'gr-btn gr-btn-mic-on';
            btn.innerHTML = '<i class="fas fa-microphone"></i> <span id="grMicText">Мікрофон ON</span>';
        }
    }
};

// Експорт у глобальну область видимості
window.GreenRoomUI = GreenRoomUI;