/**
 🌐 Модуль NetworkMonitor.
 Відстежує стан мережі та керує перепідключенням при втраті зв'язку.
 Особливо важливо для мобільних пристроїв (WiFi ↔ 4G).
 */
const NetworkMonitor = {
    iceCheckInterval: null,
    reconnectAttempts: 0,
    maxReconnectAttempts: 10,

    /**
     🔄 Перевірка стану всіх з'єднань та перепідключення при необхідності.
     Викликається при відновленні мережі (подія 'online').
     */
    checkAndReconnect() {
        Logger.log("🔍 Перевірка стану з'єднань...", "INFO");

        // 1. Перевіряємо WebSocket сигналізацію
        if (!WebSocketSignaling.isConnected) {
            Logger.log("🔄 Перепідключення WebSocket сигналізації...", "INFO");
            WebSocketSignaling.connect(JanusConnection.currentSessionToken);
        }

        // 2. Перевіряємо Janus WebSocket
        if (!JanusConnection.isJanusReady) {
            Logger.log("🔄 Перепідключення до Janus...", "INFO");
            JanusConnection.initJanusConnection().then(() => {
                // 3. Якщо Janus підключився, перевіряємо ICE
                if (RoomManager.currentRoomId) {
                    this.restartIce();
                }
            }).catch(err => {
                Logger.log(`❌ Помилка перепідключення Janus: ${err}`, "ERROR");
            });
        } else {
            // Janus підключений, але перевіряємо ICE
            this.restartIce();
        }
    },

    /**
     🔄 ICE Restart — критично при зміні IP (WiFi → 4G).
     Перестворює ICE candidates без повного перепідключення.
     */
    restartIce() {
        if (!JanusConnection.audioBridge) {
            Logger.log("⚠️ AudioBridge хендл відсутній", "WARN");
            return;
        }

        const pc = JanusConnection.audioBridge.webrtcStuff?.pc;
        if (!pc) {
            Logger.log("⚠️ PeerConnection відсутній", "WARN");
            return;
        }

        Logger.log("🔄 ICE Restart: перестворення ICE candidates...", "INFO");

        // Викликаємо ICE restart через Janus API
        JanusConnection.audioBridge.send({
            message: {
                request: "configure",
                muted: false,
                restart_ice: true  // ← КЛЮЧОВИЙ ПАРАМЕТР
            },
            success: () => {
                Logger.log("✅ ICE Restart ініційовано", "SUCCESS");
                this.reconnectAttempts = 0;
            },
            error: (err) => {
                Logger.log(`❌ Помилка ICE Restart: ${err}`, "ERROR");
            }
        });
    },

    /**
     📊 Постійний моніторинг ICE стану.
     Запускається після успішного підключення до кімнати.
     Перевіряє стан ICE кожні 3 секунди і ініціює перепідключення при необхідності.
     */
    startIceMonitoring() {
        // Зупиняємо попередній моніторинг якщо є
        if (this.iceCheckInterval) {
            clearInterval(this.iceCheckInterval);
        }

        this.iceCheckInterval = setInterval(() => {
            const pc = JanusConnection.audioBridge?.webrtcStuff?.pc;
            if (!pc) return;

            const state = pc.iceConnectionState;

            if (state === 'failed' || state === 'disconnected') {
                Logger.log(`⚠️ ICE стан: ${state}. Спроба перепідключення...`, "WARN");

                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnectAttempts++;
                    this.restartIce();
                } else {
                    Logger.log(`❌ Досягнуто максимум спроб перепідключення (${this.maxReconnectAttempts})`, "ERROR");
                    clearInterval(this.iceCheckInterval);
                }
            } else if (state === 'connected' || state === 'completed') {
                // З'єднання відновлено
                if (this.reconnectAttempts > 0) {
                    Logger.log(`✅ ICE з'єднання відновлено після ${this.reconnectAttempts} спроб`, "SUCCESS");
                }
                this.reconnectAttempts = 0;
            }
        }, 3000); // Перевірка кожні 3 секунди

        Logger.log("📊 ICE моніторинг активовано", "INFO");
    },

    /**
     🛑 Зупинка моніторингу (при виході з кімнати або закритті сторінки).
     */
    stopIceMonitoring() {
        if (this.iceCheckInterval) {
            clearInterval(this.iceCheckInterval);
            this.iceCheckInterval = null;
        }
        this.reconnectAttempts = 0;
        Logger.log(" ICE моніторинг зупинено", "INFO");
    }
};