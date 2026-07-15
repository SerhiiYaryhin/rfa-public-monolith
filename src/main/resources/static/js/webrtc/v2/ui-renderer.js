/**
 * Рендеринг UI
 */
const UIRenderer = {
    async loadRooms() {
        if (!JanusConnection.isJanusReady) return;
        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/rooms`);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            this.renderRooms(await response.json());
        } catch (error) {
            Logger.log(`❌ Помилка завантаження кімнат: ${error.message}`, "ERROR");
        }
    },

    renderRooms(rooms) {
        const container = document.getElementById("roomsList");
        if (!container) return;

        if (!rooms || rooms.length === 0) {
            container.innerHTML = `
                <div class="no-rooms">
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

            let participantsHtml = '<div class="no-users">Немає користувачів</div>';
            if (room.participants && room.participants.length > 0) {
                participantsHtml = '<ul class="user-list">' + room.participants.map(p => {
                    let state = p.talking ? 'talking' : (p.muted ? 'muted' : 'active');
                    let stateText = p.talking ? '📢 говорить' : (p.muted ? '🔇 вимкнено' : '🎙️ активний');
                    
                    // Кнопки модерації учасника
                    const isMuted = p.muted === true;
                    const muteBtn = isMuted
                        ? `<button class="btn-user-action btn-unmute" onclick="Moderator.muteUser(${room.id}, ${p.id}, false)" title="Увімкнути мікрофон">🔊</button>`
                        : `<button class="btn-user-action btn-mute" onclick="Moderator.muteUser(${room.id}, ${p.id}, true)" title="Вимкнути мікрофон">🔇</button>`;
                    
                    const kickBtn = `<button class="btn-user-action btn-kick" onclick="Moderator.kickUser(${room.id}, ${p.id})" title="Вигнати учасника">❌</button>`;
                    
                    // Переміщення між кімнатами
                    let moveHtml = '';
                    if (p.token) {
                        const otherRooms = rooms.filter(r => r.id !== room.id);
                        if (otherRooms.length > 0) {
                            const options = otherRooms.map(r => `<option value="${r.id}">${r.name}</option>`).join('');
                            moveHtml = `
                                <div class="user-move-inline">
                                    <select class="select-room-move" id="move-select-${p.id}">
                                        ${options}
                                    </select>
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

            const liveBadge = isStreaming
                ? '<span class="badge badge-live-pulse">🔴 LIVE</span>'
                : '';

            // RTP Forward секція
            const activeForwardId = Moderator.activeForwards[room.id];
            const rtpForwardSection = `
                <div class="rtp-forward-section">
                    <div class="rtp-forward-title">🎙️ Трансляція міксу назовні (RTP Forward):</div>
                    ${activeForwardId ? `
                        <div class="active-forward-info">
                            <span>📡 Активно (Stream ID: ${activeForwardId})</span>
                            <button class="btn-action-room btn-stop-forward" onclick="Moderator.stopForward(${room.id})">⏹️ Зупинити</button>
                        </div>
                    ` : `
                        <div class="forward-inputs">
                            <input type="text" id="forward-host-${room.id}" placeholder="Host (127.0.0.1)" value="127.0.0.1" class="input-forward">
                            <input type="number" id="forward-port-${room.id}" placeholder="Port (напр. 5004)" class="input-forward">
                            <button class="btn-action-room btn-start-forward" onclick="Moderator.startForward(${room.id})">▶️ Транслювати</button>
                        </div>
                    `}
                </div>
            `;

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
                    <div class="participants-title">Учасники (${room.participants ? room.participants.length : 0}):</div>
                    ${participantsHtml}
                    ${rtpForwardSection}
                </div>
                <div class="room-actions">
                    <button class="${btnClass}" ${btnDisabled} ${btnAction}>${btnText}</button>
                    ${streamButtons}
                </div>
            </div>`;
        }).join('');
    },

    enableRoomButtons() {
        document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = false);
    },

    disableRoomButtons() {
        document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = true);
    }
};