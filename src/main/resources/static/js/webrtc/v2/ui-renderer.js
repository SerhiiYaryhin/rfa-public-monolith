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
                    return `
                        <li class="user-item">
                            <span class="user-avatar">👤</span>
                            <span class="user-name">${p.display || 'Анонім'} <span class="user-id">(${p.id})</span></span>
                            <span class="state-badge ${state}">${stateText}</span>
                        </li>`;
                }).join('') + '</ul>';
            }

            const liveBadge = isStreaming
                ? '<span class="badge badge-live-pulse">🔴 LIVE</span>'
                : '';

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