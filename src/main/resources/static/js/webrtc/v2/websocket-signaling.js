/**
 * WebSocket сигналізація для управління клієнтом WebRTC
 */
const WebSocketSignaling = {
    stompClient: null,
    isConnected: false,

    connect(userUuid) {
        if (!userUuid) {
            Logger.log("⚠️ WebSocket connect: UUID користувача порожній", "WARN");
            return;
        }
        if (this.isConnected) return;

        Logger.log("🔌 Підключення до WebSocket сигналізації...", "INFO");
        
        try {
            const socket = new SockJS('/rfachat');
            this.stompClient = Stomp.over(socket);
            this.stompClient.debug = null; // Вимикаємо вивід дебаг логів STOMP в консоль

            this.stompClient.connect({}, (frame) => {
                this.isConnected = true;
                Logger.log("✅ WebSocket сигналізацію підключено успішно", "SUCCESS");

                // Підписуємося на особистий канал керування модератора
                const channel = `/topic/webrtc.control.${userUuid}`;
                Logger.log(`📥 Підписка на канал керування: ${channel}`, "DEBUG");
                
                this.stompClient.subscribe(channel, (message) => {
                    try {
                        const data = JSON.parse(message.body);
                        Logger.log(`📥 Отримано команду керування: ${JSON.stringify(data)}`, "DEBUG");

                        if (data.action === "switch_room" && data.targetRoomId) {
                            Logger.log(`🔄 Команда модератора: перехід в кімнату ${data.targetRoomId}`, "INFO");
                            RoomManager.switchRoom(data.targetRoomId);
                        }
                    } catch (e) {
                        Logger.log(`❌ Помилка парсингу WebSocket команди: ${e.message}`, "ERROR");
                    }
                });
            }, (error) => {
                this.isConnected = false;
                Logger.log(`❌ Помилка підключення до WebSocket: ${error}`, "ERROR");
                // Спроба повторного підключення через 5 секунд
                setTimeout(() => this.connect(userUuid), 5000);
            });
        } catch (e) {
            this.isConnected = false;
            Logger.log(`❌ Не вдалося ініціалізувати SockJS: ${e.message}`, "ERROR");
            setTimeout(() => this.connect(userUuid), 5000);
        }
    }
};
