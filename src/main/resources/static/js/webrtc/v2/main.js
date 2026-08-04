/**
 * 🚀 Головна точка входу для WebRTC голосового чату (версія v2).
 * 
 * Виконує послідовну ініціалізацію при завантаженні сторінки (DOMContentLoaded).
 */
document.addEventListener("DOMContentLoaded", async () => {
    Logger.log("🚀 Сторінку завантажено. Ініціалізація...", "INFO");
    
    // Тимчасово блокуємо елементи інтерфейсу до завершення підключення
    UIRenderer.disableRoomButtons();

    // 1. Завантажуємо UUID токен сесії від нашого Spring Boot бекенду
    const tokenReady = await JanusConnection.initUserSession();
    if (!tokenReady) return; // Зупиняємо старт при помилці авторизації

    // 2. Підключаємо WebSocket-сигналізацію (SockJS + STOMP) для команд модератора
    WebSocketSignaling.connect(JanusConnection.currentSessionToken);

    try {
        // 3. Ініціалізуємо WebSocket сесію з Janus Gateway та прикріплюємо плагін AudioBridge
        await JanusConnection.initJanusConnection();
        Logger.log("🟢 Система готова до роботи!", "SUCCESS");

        // 4. Автоматично заходимо в дефолтну кімнату (наприклад, 802)
        await RoomManager.switchRoom(CONFIG.DEFAULT_ROOM_ID);
    } catch (error) {
        Logger.log(`❌ Критична помилка ініціалізації: ${error}`, "ERROR");
    }

    // Перший рендеринг списку кімнат
    UIRenderer.loadRooms();
    
    // Встановлюємо інтервал для періодичного оновлення списку кімнат та учасників
    setInterval(() => UIRenderer.loadRooms(), CONFIG.ROOMS_REFRESH_INTERVAL);
});

/**
 * 🧹 Обробник події вивантаження сторінки (закриття вкладки).
 * Гарантує коректне закриття WebSocket сесії та очищення хендлів плагінів у Janus Gateway.
 */
window.addEventListener("beforeunload", () => {
    if (JanusConnection.janusInstance) {
        JanusConnection.janusInstance.destroy({cleanupHandles: true});
    }
});

// Експорт об'єктів-модулів у глобальний простір імен window 
// для можливості виклику функцій з HTML (onclick інлайн-обробники)
window.RoomManager = RoomManager;
window.StreamManager = StreamManager;
window.Moderator = Moderator;
window.WebSocketSignaling = WebSocketSignaling;