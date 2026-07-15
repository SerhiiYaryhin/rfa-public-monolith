/**
 * Управління модерацією кімнат та учасників
 */
const Moderator = {
    activeForwards: {}, // roomId -> streamId

    async muteUser(roomId, participantId, muted) {
        Logger.log(`🔇 Зміна мікрофона учасника ${participantId} в кімнаті ${roomId} на: ${muted ? 'вимкнено' : 'увімкнено'}`, "INFO");
        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/moderate/mute?roomId=${roomId}&participantId=${participantId}&muted=${muted}`, {
                method: 'POST'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const res = await response.json();
            if (res.success) {
                Logger.log(`✅ ${res.message}`, "SUCCESS");
                UIRenderer.loadRooms();
            } else {
                throw new Error(res.error);
            }
        } catch (e) {
            Logger.log(`❌ Помилка мутування: ${e.message}`, "ERROR");
        }
    },

    async kickUser(roomId, participantId) {
        if (!confirm(`Ви дійсно хочете вигнати учасника ${participantId}?`)) return;
        Logger.log(`❌ Вигнання учасника ${participantId} з кімнати ${roomId}`, "INFO");
        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/moderate/kick?roomId=${roomId}&participantId=${participantId}`, {
                method: 'POST'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const res = await response.json();
            if (res.success) {
                Logger.log(`✅ Учасника вигнано`, "SUCCESS");
                UIRenderer.loadRooms();
            } else {
                throw new Error(res.error);
            }
        } catch (e) {
            Logger.log(`❌ Помилка вигнання: ${e.message}`, "ERROR");
        }
    },

    async moveUser(userUuid, targetRoomId) {
        if (!userUuid) {
            Logger.log("⚠️ Неможливо перемістити: UUID користувача відсутній", "WARN");
            return;
        }
        Logger.log(`🔄 Запит на переміщення користувача ${userUuid.substring(0, 8)}... в кімнату ${targetRoomId}`, "INFO");
        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/moderate/move?userUuid=${userUuid}&targetRoomId=${targetRoomId}`, {
                method: 'POST'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const res = await response.json();
            if (res.success) {
                Logger.log(`✅ Команду переміщення надіслано`, "SUCCESS");
                UIRenderer.loadRooms();
            } else {
                throw new Error(res.error);
            }
        } catch (e) {
            Logger.log(`❌ Помилка переміщення: ${e.message}`, "ERROR");
        }
    },

    async startForward(roomId) {
        const hostEl = document.getElementById(`forward-host-${roomId}`);
        const portEl = document.getElementById(`forward-port-${roomId}`);
        if (!hostEl || !portEl) return;

        const host = hostEl.value.trim() || "127.0.0.1";
        const port = parseInt(portEl.value.trim());

        if (isNaN(port)) {
            Logger.log("⚠️ Порт має бути числом", "WARN");
            alert("Будь ласка, вкажіть коректний UDP порт!");
            return;
        }

        Logger.log(`📤 Запуск RTP forward з кімнати ${roomId} на ${host}:${port}`, "INFO");
        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/moderate/forward/start?roomId=${roomId}&host=${host}&port=${port}`, {
                method: 'POST'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const res = await response.json();
            if (res.success && res.streamId) {
                Logger.log(`✅ RTP Forward запущено. Stream ID: ${res.streamId}`, "SUCCESS");
                this.activeForwards[roomId] = res.streamId;
                UIRenderer.loadRooms();
            } else {
                throw new Error(res.error || "Помилка запуску");
            }
        } catch (e) {
            Logger.log(`❌ Помилка запуску RTP forward: ${e.message}`, "ERROR");
        }
    },

    async stopForward(roomId) {
        const streamId = this.activeForwards[roomId];
        if (!streamId) {
            Logger.log("⚠️ Немає активного forward для цієї кімнати в пам'яті", "WARN");
            return;
        }

        Logger.log(`⏹️ Зупинка RTP forward з кімнати ${roomId} (Stream ID: ${streamId})`, "INFO");
        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/moderate/forward/stop?roomId=${roomId}&streamId=${streamId}`, {
                method: 'POST'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const res = await response.json();
            if (res.success) {
                Logger.log(`✅ RTP Forward зупинено`, "SUCCESS");
                delete this.activeForwards[roomId];
                UIRenderer.loadRooms();
            } else {
                throw new Error(res.error);
            }
        } catch (e) {
            Logger.log(`❌ Помилка зупинки RTP forward: ${e.message}`, "ERROR");
        }
    }
};
