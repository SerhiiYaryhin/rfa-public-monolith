/**
 🚀 Головна точка входу для WebRTC голосового чату (версія v3).
 Виконує послідовну ініціалізацію при завантаженні сторінки.
 Додає обробники подій мережі для автоматичного перепідключення.
 */
document.addEventListener("DOMContentLoaded", async () => {
    Logger.log("🚀 Сторінку завантажено. Ініціалізація...", "INFO");

    // Тимчасово блокуємо елементи інтерфейсу до завершення підключення
    UIRenderer.disableRoomButtons();

    // 🎯 Завантаження збережених налаштувань RTP Forward з localStorage
    const savedHost = localStorage.getItem('rtp-forward-host');
    const savedPort = localStorage.getItem('rtp-forward-port');
    if (savedHost) {
        const hostEl = document.getElementById('forward-host');
        if (hostEl) hostEl.value = savedHost;
    }
    if (savedPort) {
        const portEl = document.getElementById('forward-port');
        if (portEl) portEl.value = savedPort;
    }

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

        // 🎯 Оновлюємо інформацію про поточну кімнату в панелі RTP Forward
        Moderator.updateCurrentRoomInfo();
    } catch (error) {
        Logger.log(`❌ Критична помилка ініціалізації: ${error}`, "ERROR");
    }

    // Перший рендеринг списку кімнат
    UIRenderer.loadRooms();

    // Встановлюємо інтервал для періодичного оновлення списку кімнат та учасників
    setInterval(() => UIRenderer.loadRooms(), CONFIG.ROOMS_REFRESH_INTERVAL);

    // 🎯 Обробники подій мережі для автоматичного перепідключення
    window.addEventListener('online', () => {
        Logger.log("🌐 Мережа відновлена", "SUCCESS");
        if (typeof NetworkMonitor !== 'undefined') {
            NetworkMonitor.checkAndReconnect();
        }
    });

    window.addEventListener('offline', () => {
        Logger.log("⚠️ Мережа втрачена", "WARN");
        Logger.updateConnectionStatus(false, "Немає мережі");
    });

    Logger.log("✅ Обробники мережі активовано", "INFO");
});

/**
 🧹 Обробник події вивантаження сторінки (закриття вкладки).
 */
window.addEventListener("beforeunload", () => {
    if (JanusConnection.janusInstance) {
        JanusConnection.janusInstance.destroy({cleanupHandles: true});
    }
    if (typeof NetworkMonitor !== 'undefined') {
        NetworkMonitor.stopIceMonitoring();
    }
});

// Експорт об'єктів-модулів у глобальний простір імен window
window.RoomManager = RoomManager;
window.StreamManager = StreamManager;
window.Moderator = Moderator;
window.WebSocketSignaling = WebSocketSignaling;
window.NetworkMonitor = NetworkMonitor;