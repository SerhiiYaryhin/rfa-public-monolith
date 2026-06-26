/**
 * Точка входу
 */
document.addEventListener("DOMContentLoaded", async () => {
    Logger.log("🚀 Сторінку завантажено. Ініціалізація...", "INFO");
    UIRenderer.disableRoomButtons();

    const tokenReady = await JanusConnection.initUserSession();
    if (!tokenReady) return;

    try {
        await JanusConnection.initJanusConnection();
        Logger.log("🟢 Система готова до роботи!", "SUCCESS");

        // Автоматичний вхід в дефолтну кімнату
        await RoomManager.switchRoom(CONFIG.DEFAULT_ROOM_ID);
    } catch (error) {
        Logger.log(`❌ Критична помилка ініціалізації: ${error}`, "ERROR");
    }

    UIRenderer.loadRooms();
    setInterval(() => UIRenderer.loadRooms(), CONFIG.ROOMS_REFRESH_INTERVAL);
});

window.addEventListener("beforeunload", () => {
    if (JanusConnection.janusInstance) {
        JanusConnection.janusInstance.destroy({cleanupHandles: true});
    }
});

// Експорт для onclick handlers
window.RoomManager = RoomManager;
window.StreamManager = StreamManager;