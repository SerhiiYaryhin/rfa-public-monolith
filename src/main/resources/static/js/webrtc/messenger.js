/**
 * 🏠 MainRoomAudio — автоматичне підключення до Janus AudioBridge
 * для кімнат станцій при завантаженні /chat.
 *
 * Логіка визначення типу кімнати:
 * - curRoomType === 'CHATROOM_TYPE_PUBLIC' → тільки текстовий чат, БЕЗ Janus
 * - Будь-який інший тип → текстовий чат + Janus AudioBridge
 *
 * ❌ Жодних hardcoded UUID! Все береться з Thymeleaf-змінних:
 *   curRoomType, curRoomJanusId, stationUuid
 */
const MainRoomAudio = {
    isInitialized: false,

    async init() {
        // Визначаємо, чи потребує ця кімната WebRTC
        const needsWebRtc = this._roomNeedsWebRtc(curRoomType);

        if (!needsWebRtc) {
            console.log(`📝 Кімната типу "${curRoomType}" — тільки текстовий чат. Janus не активується.`);
            return;
        }

        if (!curRoomJanusId) {
            console.warn(`⚠️ Кімната типу "${curRoomType}" потребує WebRTC, але curRoomJanusId не переданий з сервера.`);
            return;
        }

        if (this.isInitialized) return;
        this.isInitialized = true;

        console.log(`🎙️ Активуємо Janus для кімнати типу "${curRoomType}" (Janus ID: ${curRoomJanusId}, Station: ${stationUuid})`);

        try {
            // Перевіряємо доступність WebRTC модулів
            if (typeof JanusConnection === 'undefined') {
                console.warn('⚠️ JanusConnection не завантажено. Пропускаємо автопідключення.');
                return;
            }

            // Отримуємо токен
            if (!JanusConnection.currentSessionToken) {
                const tokenReady = await JanusConnection.initUserSession();
                if (!tokenReady) throw new Error('Не вдалося отримати токен');
            }

            // Підключаємося до Janus WS
            if (!JanusConnection.isJanusReady) {
                await JanusConnection.initJanusConnection();
            }

            // Підключаємо WebSocket сигналізацію для команд модератора
            if (typeof WebSocketSignaling !== 'undefined' && !WebSocketSignaling.isConnected) {
                WebSocketSignaling.connect(JanusConnection.currentSessionToken);
            }

            // Перемикаємося в аудіокімнату головної кімнати станції
            if (typeof RoomManager !== 'undefined') {
                await RoomManager.switchRoom(curRoomJanusId);
            }

            // Запускаємо ICE моніторинг
            if (typeof NetworkMonitor !== 'undefined') {
                NetworkMonitor.startIceMonitoring();
            }

            console.log(`✅ Janus AudioBridge активовано для кімнати ${curRoomJanusId}`);

        } catch (error) {
            console.error('❌ Помилка автопідключення до Janus:', error);
        }
    },

    /**
     * Визначає, чи потребує кімната WebRTC на основі її типу.
     * Публічні кімнати — тільки текст. Всі інші — текст + аудіо.
     */
    _roomNeedsWebRtc(roomType) {
        if (!roomType) return false;
        const textOnlyTypes = ['CHATROOM_TYPE_PUBLIC'];
        return !textOnlyTypes.includes(roomType);
    },

    /**
     * Відключення від Janus (при виході зі сторінки або перемиканні на текстову кімнату)
     */
    disconnect() {
        if (!this.isInitialized) return;

        console.log('🔌 Відключення від Janus AudioBridge...');

        if (typeof NetworkMonitor !== 'undefined') {
            NetworkMonitor.stopIceMonitoring();
        }

        if (typeof JanusConnection !== 'undefined' && JanusConnection.audioBridge) {
            try {
                JanusConnection.audioBridge.detach();
            } catch (e) {
                console.warn('Detach error:', e);
            }
            JanusConnection.audioBridge = null;
            JanusConnection.isJanusReady = false;
        }

        if (typeof AudioMonitor !== 'undefined') {
            AudioMonitor.stop();
        }

        this.isInitialized = false;
    }
};

// Експорт у глобальну область
window.MainRoomAudio = MainRoomAudio;

/**
 * 🎙️ GreenRoomUI — керує модальним вікном Green Room / Broadcast
 * для гостя в messenger.html
 */
const GreenRoomUI = {
    isActive: false,
    currentJanusRoomId: null,
    isMuted: false,
    previousChatRoom: null,

    /**
     * Відкрити модальне вікно та підключитися до Janus AudioBridge
     */
    async enter(janusRoomId, roomName, autoMute) {
        if (this.isActive) return;
        this.isActive = true;
        this.currentJanusRoomId = janusRoomId;
        this.isMuted = !!autoMute;
        this.previousChatRoom = (typeof curroom !== 'undefined') ? curroom : null;

        // Оновлюємо UI модалки
        const overlay = document.getElementById('greenRoomOverlay');
        const title = document.getElementById('grTitle');
        const subtitle = document.getElementById('grSubtitle');
        const status = document.getElementById('grStatus');

        overlay.classList.add('active');
        title.textContent = roomName || '🎙️ Аудіокімната';
        subtitle.textContent = autoMute
            ? 'Ви в прямому ефірі. Мікрофон вимкнено. Модератор надасть вам слово.'
            : 'Підготовка до ефіру. Перевірте мікрофон.';
        status.textContent = '⏳ Підключення до аудіокімнати...';
        status.className = 'gr-status';

        this.updateMicButton();

        // ✅ Перевіряємо, чи доступні WebRTC модулі
        if (typeof JanusConnection === 'undefined') {
            status.textContent = '⚠️ WebRTC модулі не завантажені. Аудіо недоступне.';
            status.className = 'gr-status error';
            console.warn('JanusConnection is not defined. WebRTC modules not loaded.');
            return;
        }

        try {
            // Ініціалізуємо Janus якщо ще не ініціалізовано
            if (!JanusConnection.currentSessionToken) {
                const tokenReady = await JanusConnection.initUserSession();
                if (!tokenReady) throw new Error('Не вдалося отримати токен');
            }

            if (!JanusConnection.isJanusReady) {
                await JanusConnection.initJanusConnection();
            }

            // Підключаємо WebSocket сигналізацію для команд модератора
            if (typeof WebSocketSignaling !== 'undefined' && !WebSocketSignaling.isConnected) {
                WebSocketSignaling.connect(JanusConnection.currentSessionToken);
            }

            status.textContent = '✅ Підключено до аудіокімнати';
            status.className = 'gr-status connected';

            // Перемикаємося в потрібну кімнату Janus
            if (typeof RoomManager !== 'undefined') {
                await RoomManager.switchRoom(janusRoomId);
            }

            // Якщо autoMute — вимикаємо мікрофон
            if (autoMute && JanusConnection.audioBridge) {
                JanusConnection.audioBridge.send({
                    message: { request: 'configure', muted: true }
                });
                this.isMuted = true;
                this.updateMicButton();
            }

            // Запускаємо ICE моніторинг
            if (typeof NetworkMonitor !== 'undefined') {
                NetworkMonitor.startIceMonitoring();
            }

        } catch (error) {
            console.error('GreenRoomUI enter error:', error);
            status.textContent = '❌ Помилка підключення: ' + error.message;
            status.className = 'gr-status error';
        }
    },

    /**
     * Залишити аудіокімнату та повернутися в текстовий чат
     */
    leaveRoom() {
        if (!this.isActive) return;

        console.log('🚪 Вихід з аудіокімнати...');

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

        // Закриваємо модалку
        document.getElementById('greenRoomOverlay').classList.remove('active');
        this.isActive = false;
        this.currentJanusRoomId = null;

        // Повертаємося в попередню текстову кімнату
        if (this.previousChatRoom && typeof selectroom === 'function') {
            selectroom(this.previousChatRoom, 'Основна');
        }

        if (typeof showEfirNotification === 'function') {
            showEfirNotification('Ви повернулися в текстовий чат', 'info');
        }
    },

    /**
     * Toggle мікрофона в аудіокімнаті
     */
    toggleMic() {
        if (typeof JanusConnection === 'undefined' || !JanusConnection.audioBridge) {
            console.warn('AudioBridge not ready');
            return;
        }

        this.isMuted = !this.isMuted;
        JanusConnection.audioBridge.send({
            message: { request: 'configure', muted: this.isMuted }
        });

        this.updateMicButton();
        console.log(`🎙️ Mic ${this.isMuted ? 'OFF' : 'ON'}`);
    },

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

// Експорт у глобальну область
window.GreenRoomUI = GreenRoomUI;