/**
 * 📻 Модуль StreamManager (версія v2).
 * 
 * Керує запуском та зупинкою трансляції інтернет-радіостанцій
 * у відповідні кімнати Janus AudioBridge через виклики бекенд-ендпоінтів:
 * - startStreamToRoom: запуск радіо-стріму (RTP інжекції).
 * - stopStreamFromRoom: зупинка радіо-стріму.
 */
const StreamManager = {
    /**
     * Запуск трансляції радіо у вказану кімнату.
     * 
     * @param {string} stationId Ідентифікатор радіостанції (наприклад, 802)
     */
    async startStreamToRoom(stationId) {
        if (!stationId) {
            Logger.log("❌ Station ID не вказано", "ERROR");
            return;
        }

        Logger.log(`▶️ Запуск стріму для станції ${stationId}...`, "INFO");

        try {
            // Викликаємо POST ендпоінт запуску трансляції станції
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

            // Оновлюємо UI через 2 секунди, коли Janus підхопить Plain RTP та FFmpeg почне лити потік
            setTimeout(() => UIRenderer.loadRooms(), 2000);

        } catch (error) {
            Logger.log(`❌ Помилка запуску стріму: ${error.message}`, "ERROR");
        }
    },

    /**
     * Зупинка трансляції радіо у вказаній кімнаті.
     * 
     * @param {string} stationId Ідентифікатор радіостанції (наприклад, 802)
     */
    async stopStreamFromRoom(stationId) {
        if (!stationId) {
            Logger.log("❌ Station ID не вказано", "ERROR");
            return;
        }

        // Попереднє підтвердження перед зупинкою
        if (!confirm(`Зупинити трансляцію станції ${stationId}?`)) {
            return;
        }

        Logger.log(`⏹️ Зупинка стріму: ${stationId}`, "INFO");

        try {
            // Викликаємо POST ендпоінт зупинки трансляції станції
            const response = await fetch(
                `${CONFIG.BACKEND_API}/stop?stationId=${encodeURIComponent(stationId)}`,
                { method: 'POST' }
            );

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP ${response.status}`);
            }

            Logger.log(`✅ Стрім зупинено`, "SUCCESS");
            
            // Швидко оновлюємо UI для відображення змін
            setTimeout(() => UIRenderer.loadRooms(), 500);

        } catch (error) {
            Logger.log(`❌ Помилка зупинки стріму: ${error.message}`, "ERROR");
        }
    }
};