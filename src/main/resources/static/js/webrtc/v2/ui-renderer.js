/**
 🎨 Модуль UIRenderer (версія v3).
 Відповідає за відображення інтерфейсу користувача та оновлення DOM-дерева.
 🎯 ВИПРАВЛЕНО: Прибрано генерацію полів RTP Forward з карток кімнат,
 щоб вони не зникали при періодичному оновленні.
 */
const UIRenderer = {
    /**
     📡 Асинхронно завантажує дані про кімнати з бекенду та ініціює їх рендеринг.
     */
    async loadRooms() {
        if (!JanusConnection.isJanusReady) return;
        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/rooms`);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            this.renderRooms(await response.json());

            // 🎯 Оновлюємо інформацію про поточну кімнату в панелі RTP Forward
            if (typeof Moderator !== 'undefined' && Moderator.updateCurrentRoomInfo) {
                Moderator.updateCurrentRoomInfo();
            }
        } catch (error) {
            Logger.log(`❌ Помилка завантаження кімнат: ${error.message}`, "ERROR");
        }
    },

    /**
     🎨 Динамічно рендерить HTML-код для всіх кімнат у DOM-контейнер.
     @param {Array} rooms Масив об'єктів кімнат, отриманий з сервера
     */
    renderRooms(rooms) {
        const container = document.getElementById("roomsList");
        if (!container) return;

        if (!rooms || rooms.length === 0) {
            container.innerHTML = `<div class="no-rooms">
                <div class="no-rooms-icon">📭</div>
                <div class="no-rooms-text">Немає активних кімнат</div>
            </div>`;
            return;
        }

        container.innerHTML = rooms.map(room => {
            const isCurrent = RoomManager.currentRoomId === room.id;
            const isStreaming = room.stationstate === true;

            const btnClass = isCurrent ? 'btn-action-room' : 'btn-action-room btn-join-room';
            const btnText = isCurrent ? '✓ Ви тут' : '🚪 Увійти';
            const btnDisabled = isCurrent ? 'disabled' : '';
            const btnAction = isCurrent ? '' : `onclick="RoomManager.switchRoom(${room.id})"`;
            const stationId = room.id;

            // Кнопки управління трансляцією радіо в кімнату (FFmpeg + Plain RTP)
            const streamButtons = `
                <button class="btn-action-room btn-start-stream" 
                        onclick="StreamManager.startStreamToRoom(${stationId})"
                        title="Запустити трансляцію">
                    ▶️ Запустити
                </button>
                <button class="btn-action-room btn-stop-stream" 
                        onclick="StreamManager.stopStreamFromRoom(${stationId})"
                        title="Зупинити трансляцію">
                    ⏹️ Зупинити
                </button>
            `;

            // Формуємо список учасників кімнати
            let participantsHtml = '<div class="no-users" style="padding:10px; color:#6c757d;">Немає користувачів</div>';
            if (room.participants && room.participants.length > 0) {
                participantsHtml = '<ul class="user-list">' + room.participants.map(p => {
                    let state = p.talking ? 'talking' : (p.muted ? 'muted' : 'active');
                    let stateText = p.talking ? '📢 говорить' : (p.muted ? '🔇 вимкнено' : '🎙️ активний');

                    const isMuted = p.muted === true;
                    const muteBtn = isMuted
                        ? `<button class="btn-user-action btn-unmute" onclick="Moderator.muteUser(${room.id}, ${p.id}, false)" title="Увімкнути мікрофон">🔊</button>`
                        : `<button class="btn-user-action btn-mute" onclick="Moderator.muteUser(${room.id}, ${p.id}, true)" title="Вимкнути мікрофон">🔇</button>`;

                    const kickBtn = `<button class="btn-user-action btn-kick" onclick="Moderator.kickUser(${room.id}, ${p.id})" title="Вигнати учасника">❌</button>`;

                    let moveHtml = '';
                    if (p.token) {
                        const otherRooms = rooms.filter(r => r.id !== room.id);
                        if (otherRooms.length > 0) {
                            const options = otherRooms.map(r => `<option value="${r.id}">${r.name}</option>`).join('');
                            moveHtml = `
                                <div class="user-move-inline" style="display:flex; gap:5px; margin-top:5px;">
                                    <select class="select-room-move" id="move-select-${p.id}">${options}</select>
                                    <button class="btn-user-action btn-move" onclick="Moderator.moveUser('${p.token}', document.getElementById('move-select-${p.id}').value)" title="Перемістити">➡️</button>
                                </div>
                            `;
                        }
                    }

                    return `
                        <li class="user-item">
                            <div class="user-info-row">
                                <span class="user-avatar">👤</span>
                                <span class="user-name">${p.display || 'Анонім'} <span class="user-id">(${p.id})</span></span>
                                <span class="state-badge ${state}">${stateText}</span>
                            </div>
                            <div class="user-mod-actions">
                                ${muteBtn}
                                ${kickBtn}
                                ${moveHtml}
                            </div>
                        </li>`;
                }).join('') + '</ul>';
            }

            // Червоний LIVE імпульсний індикатор
            const liveBadge = isStreaming
                ? '<span class="badge badge-live-pulse">🔴 LIVE</span>'
                : '';

            // 🎯 ВИПРАВЛЕНО: rtpForwardSection ВИДАЛЕНО звідси!
            // RTP Forward тепер керується глобальною панеллю зверху

            return `
            <div class="room-card ${isStreaming ? 'room-card-live' : ''} ${isCurrent ? 'room-card-current' : ''}">
                <div class="room-header">
                    <div class="room-title-container">
                        <span class="room-name">${room.name}</span>
                        <span class="room-id">ID: ${room.id}</span>
                    </div>
                    ${liveBadge}
                </div>
                <div class="room-body">
                    <div class="participants-title" style="font-weight:600; margin-bottom:8px;">
                        Учасники (${room.participants ? room.participants.length : 0}):
                    </div>
                    ${participantsHtml}
                </div>
                <div class="room-actions" style="margin-top:15px; border-top:1px solid #e9ecef; padding-top:10px;">
                    <button class="${btnClass}" ${btnDisabled} ${btnAction}>${btnText}</button>
                    ${streamButtons}
                </div>
            </div>`;
        }).join('');
    },

    /**
     ✅ Дозволяє клікати по кнопках кімнат.
     */
    enableRoomButtons() {
        document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = false);
    },

    /**
     ⛔ Тимчасово блокує всі кнопки дій під час виконання транзакцій переходу.
     */
    disableRoomButtons() {
        document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = true);
    }
};