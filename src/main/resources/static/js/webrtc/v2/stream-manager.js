/**
 * Управління трансляціями
 */
const StreamManager = {
    async startStreamToRoom(stationId) {
        if (!stationId) {
            Logger.log("❌ Station ID не вказано", "ERROR");
            return;
        }

        Logger.log(`▶️ Запуск стріму для станції ${stationId}...`, "INFO");

        try {
            const response = await fetch(
                `${CONFIG.BACKEND_API}/start?stationId=${encodeURIComponent(stationId)}`,
                { method: 'POST' }
            );

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP ${response.status}`);
            }

            const data = await response.json();
            Logger.log(`✅ Стрім запущено! Room: ${data.room_id}, Port: ${data.rtp_port}`, "SUCCESS");

            setTimeout(() => UIRenderer.loadRooms(), 2000);

        } catch (error) {
            Logger.log(`❌ Помилка запуску стріму: ${error.message}`, "ERROR");
        }
    },

    async stopStreamFromRoom(stationId) {
        if (!stationId) {
            Logger.log("❌ Station ID не вказано", "ERROR");
            return;
        }

        if (!confirm(`Зупинити трансляцію станції ${stationId}?`)) {
            return;
        }

        Logger.log(`⏹️ Зупинка стріму: ${stationId}`, "INFO");

        try {
            const response = await fetch(
                `${CONFIG.BACKEND_API}/stop?stationId=${encodeURIComponent(stationId)}`,
                { method: 'POST' }
            );

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP ${response.status}`);
            }

            Logger.log(`✅ Стрім зупинено`, "SUCCESS");
            setTimeout(() => UIRenderer.loadRooms(), 500);

        } catch (error) {
            Logger.log(`❌ Помилка зупинки стріму: ${error.message}`, "ERROR");
        }
    }
};