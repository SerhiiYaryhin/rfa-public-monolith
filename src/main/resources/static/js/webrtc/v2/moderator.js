/**
 🛠️ Модуль Moderator (версія v3).
 Надає функції для відправки запитів модерації на бекенд:
 - muteUser: вимкнення/увімкнення мікрофона гостя.
 - kickUser: примусове видалення користувача з голосового чату.
 - moveUser: переміщення користувача між кімнатами за допомогою WebSocket сигналізації.
 - forwardCurrentRoom / stopCurrentForward: керування RTP Forward для поточної кімнати.
 */
const Moderator = {
    // Словник активних трансляцій RTP Forward: roomId -> streamId
    activeForwards: {},

    /**
     🔇 Керування мікрофоном учасника (мутування/розмутування).
     */
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

    /**
     ❌ Вигнання учасника з голосової кімнати.
     */
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

    /**
     🔄 Переміщення учасника в іншу голосову кімнату.
     */
    async moveUser(userUuid, targetRoomId) {
        if (!userUuid) {
            Logger.log("️ Неможливо перемістити: UUID користувача відсутній", "WARN");
            return;
        }
        Logger.log(` Запит на переміщення користувача ${userUuid.substring(0, 8)}... в кімнату ${targetRoomId}`, "INFO");
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

    // ================================================================
    // 🎯 МЕТОДИ ДЛЯ ГЛОБАЛЬНОГО RTP FORWARD
    // ================================================================

    /**
     📡 Запуск RTP Forward для поточної кімнати (де сидить користувач).
     */
    async forwardCurrentRoom() {
        const roomId = RoomManager.currentRoomId;
        if (!roomId) {
            Logger.log("⚠️ Ви не знаходитесь в жодній кімнаті!", "WARN");
            alert("Спочатку увійдіть в кімнату!");
            return;
        }

        // Перевірка: чи не запущено вже forward для цієї кімнати
        if (this.activeForwards[roomId]) {
            Logger.log(`⚠️ RTP Forward для кімнати ${roomId} вже активний (Stream ID: ${this.activeForwards[roomId]})`, "WARN");
            alert("Трансляція вже активна для цієї кімнати!");
            return;
        }

        const hostEl = document.getElementById('forward-host');
        const portEl = document.getElementById('forward-port');
        if (!hostEl || !portEl) {
            Logger.log("❌ Елементи вводу не знайдено", "ERROR");
            return;
        }

        const host = hostEl.value.trim() || "127.0.0.1";
        const port = parseInt(portEl.value.trim());

        if (isNaN(port)) {
            Logger.log("⚠️ Порт має бути числом", "WARN");
            alert("Будь ласка, вкажіть коректний UDP порт!");
            return;
        }

        // Зберігаємо в localStorage для наступних сесій
        localStorage.setItem('rtp-forward-host', host);
        localStorage.setItem('rtp-forward-port', port);

        Logger.log(`📤 Запуск RTP Forward для кімнати ${roomId} на ${host}:${port}`, "INFO");

        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/moderate/forward/start?roomId=${roomId}&host=${host}&port=${port}`, {
                method: 'POST'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const res = await response.json();

            if (res.success && res.streamId) {
                Logger.log(`✅ RTP Forward запущено. Stream ID: ${res.streamId}`, "SUCCESS");
                this.activeForwards[roomId] = res.streamId;
                this.updateForwardUI(true, res.streamId);
                UIRenderer.loadRooms();
            } else {
                throw new Error(res.error || "Помилка запуску");
            }
        } catch (e) {
            Logger.log(`❌ Помилка запуску RTP Forward: ${e.message}`, "ERROR");
            alert(`Помилка: ${e.message}`);
        }
    },

    /**
     ⏹️ Зупинка RTP Forward для поточної кімнати.
     */
    async stopCurrentForward() {
        const roomId = RoomManager.currentRoomId;
        if (!roomId) {
            Logger.log("⚠️ Ви не знаходитесь в жодній кімнаті!", "WARN");
            return;
        }

        const streamId = this.activeForwards[roomId];
        if (!streamId) {
            Logger.log("️ Немає активного forward для цієї кімнати", "WARN");
            return;
        }

        Logger.log(`⏹️ Зупинка RTP Forward для кімнати ${roomId} (Stream ID: ${streamId})`, "INFO");

        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/moderate/forward/stop?roomId=${roomId}&streamId=${streamId}`, {
                method: 'POST'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const res = await response.json();

            if (res.success) {
                Logger.log(`✅ RTP Forward зупинено`, "SUCCESS");
                delete this.activeForwards[roomId];
                this.updateForwardUI(false);
                UIRenderer.loadRooms();
            } else {
                throw new Error(res.error);
            }
        } catch (e) {
            Logger.log(`❌ Помилка зупинки RTP Forward: ${e.message}`, "ERROR");
        }
    },

    /**
     🎨 Оновлення UI глобальної панелі RTP Forward.
     ВИПРАВЛЕНО: Правильна логіка disabled стану кнопок.
     */
    updateForwardUI(isActive, streamId = null) {
        const btnStart = document.getElementById('btn-start-forward');
        const btnStop = document.getElementById('btn-stop-forward');
        const indicator = document.getElementById('forward-indicator');
        const statusText = document.getElementById('forward-status-text');

        if (btnStart) {
            // Кнопка "Транслювати" НЕактивна, коли forward АКТИВНИЙ або коли немає поточної кімнати
            const noRoom = !RoomManager.currentRoomId;
            btnStart.disabled = isActive || noRoom;
        }
        if (btnStop) {
            // Кнопка "Зупинити" АКТИВНА тільки коли forward АКТИВНИЙ
            btnStop.disabled = !isActive;
        }

        if (indicator) {
            indicator.className = isActive
                ? 'status-indicator status-connected'
                : 'status-indicator status-disconnected';
        }
        if (statusText) {
            statusText.textContent = isActive
                ? `✅ Активно (Stream ID: ${streamId})`
                : 'Неактивно';
        }
    },

    /**
     🔄 Оновити інформацію про поточну кімнату в панелі RTP Forward.
     Викликається при зміні кімнати.
     */
    updateCurrentRoomInfo() {
        const infoEl = document.getElementById('current-room-info');
        if (!infoEl) return;

        const roomId = RoomManager.currentRoomId;
        if (roomId) {
            infoEl.textContent = `Поточна кімната: ${roomId}`;
            infoEl.style.color = '#27ae60';
            infoEl.style.fontWeight = '600';
        } else {
            infoEl.textContent = 'Поточна кімната: не обрана';
            infoEl.style.color = '#6c757d';
            infoEl.style.fontWeight = 'normal';
        }

        // 🎯 Оновлюємо стан кнопок залежно від наявності кімнати
        const isActive = roomId && this.activeForwards[roomId];
        this.updateForwardUI(!!isActive, isActive ? this.activeForwards[roomId] : null);
    }
};