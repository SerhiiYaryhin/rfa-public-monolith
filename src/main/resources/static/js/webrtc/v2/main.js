/**
 🚀 Головна точка входу для WebRTC голосового чату (версія v3).
 Виконує послідовну ініціалізацію при завантаженні сторінки.
 */
document.addEventListener("DOMContentLoaded", async () => {
    Logger.log("🚀 Сторінку завантажено. Ініціалізація...", "INFO");

    UIRenderer.disableRoomButtons();

    // 🎯 НОВЕ: Завантаження збережених налаштувань RTP Forward з localStorage
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

    const tokenReady = await JanusConnection.initUserSession();
    if (!tokenReady) return;

    WebSocketSignaling.connect(JanusConnection.currentSessionToken);

    try {
        await JanusConnection.initJanusConnection();
        Logger.log("🟢 Система готова до роботи!", "SUCCESS");
        await RoomManager.switchRoom(CONFIG.DEFAULT_ROOM_ID);
    } catch (error) {
        Logger.log(`❌ Критична помилка ініціалізації: ${error}`, "ERROR");
    }

    UIRenderer.loadRooms();
    setInterval(() => UIRenderer.loadRooms(), CONFIG.ROOMS_REFRESH_INTERVAL);

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

window.addEventListener("beforeunload", () => {
    if (JanusConnection.janusInstance) {
        JanusConnection.janusInstance.destroy({cleanupHandles: true});
    }
    if (typeof NetworkMonitor !== 'undefined') {
        NetworkMonitor.stopIceMonitoring();
    }
});

window.RoomManager = RoomManager;
window.StreamManager = StreamManager;
window.Moderator = Moderator;
window.WebSocketSignaling = WebSocketSignaling;
window.NetworkMonitor = NetworkMonitor;