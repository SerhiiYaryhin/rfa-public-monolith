/**
 * 🔌 Модуль WebSocket сигналізації для прийому віддалених команд модератора (версія v2).
 * 
 * Дозволяє серверу в реальному часі керувати підключенням клієнта 
 * (наприклад, переміщати його між кімнатами Janus).
 */
const WebSocketSignaling = {
    // Екземпляр клієнта STOMP
    stompClient: null,
    
    // Статус поточного підключення до брокера
    isConnected: false,

    /**
     * Встановлює підключення до WebSocket-сервера.
     * 
     * @param {string} userUuid Унікальний токен користувача (його UUID), 
     * що використовується як назва особистого каналу сигналізації.
     */
    connect(userUuid) {
        if (!userUuid) {
            Logger.log("⚠️ WebSocket connect: UUID користувача порожній", "WARN");
            return;
        }
        // Запобігаємо повторному підключенню при вже активному з'єднанні
        if (this.isConnected) return;

        Logger.log("🔌 Підключення до WebSocket сигналізації...", "INFO");
        
        try {
            // Ініціалізація з'єднання через SockJS та STOMP
            const socket = new SockJS('/rfachat');
            this.stompClient = Stomp.over(socket);
            
            // Вимикаємо шум дебаг-повідомлень бібліотеки STOMP в консоль браузера
            this.stompClient.debug = null; 

            // Підключення до брокера повідомлень RabbitMQ
            this.stompClient.connect({}, (frame) => {
                this.isConnected = true;
                Logger.log("✅ WebSocket сигналізацію підключено успішно", "SUCCESS");

                // Підписуємося на особистий канал керування користувача.
                // Будь-яке повідомлення, надіслане бекендом у цю тему, надійде сюди.
                const channel = `/topic/webrtc.control.${userUuid}`;
                Logger.log(`📥 Підписка на канал керування: ${channel}`, "DEBUG");
                
                this.stompClient.subscribe(channel, (message) => {
                    try {
                        // Парсимо JSON-команду від модератора
                        const data = JSON.parse(message.body);
                        Logger.log(`📥 Отримано команду керування: ${JSON.stringify(data)}`, "DEBUG");

                        // Обробка команди переходу в іншу кімнату
                        if (data.action === "switch_room" && data.targetRoomId) {
                            Logger.log(`🔄 Команда модератора: перехід в кімнату ${data.targetRoomId}`, "INFO");
                            // Викликаємо RoomManager для перепідключення до нової кімнати Janus
                            RoomManager.switchRoom(data.targetRoomId);
                        }
                    } catch (e) {
                        Logger.log(`❌ Помилка парсингу WebSocket команди: ${e.message}`, "ERROR");
                    }
                });
            }, (error) => {
                this.isConnected = false;
                Logger.log(`❌ Помилка підключення до WebSocket: ${error}`, "ERROR");
                
                // Автоматичне перепідключення через 5 секунд у разі обриву зв'язку
                setTimeout(() => this.connect(userUuid), 5000);
            });
        } catch (e) {
            this.isConnected = false;
            Logger.log(`❌ Не вдалося ініціалізувати SockJS: ${e.message}`, "ERROR");
            
            // Повторна спроба ініціалізації при критичній помилці
            setTimeout(() => this.connect(userUuid), 5000);
        }
    }
};
