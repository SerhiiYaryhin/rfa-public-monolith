/**
 * 🎨 Модуль UIRenderer (версія v2).
 * 
 * Відповідає за відображення інтерфейсу користувача та оновлення DOM-дерева:
 * - Завантаження списку активних кімнат та учасників з нашого REST API.
 * - Генерація HTML-коду карток кімнат, кнопок дій, елементів модерації та RTP Forward.
 * - Керування доступністю кнопок переходу в інтерфейсі.
 */
const UIRenderer = {
    /**
     * Асинхронно завантажує дані про кімнати з бекенду та ініціює їх рендеринг.
     */
    async loadRooms() {
        // Оновлюємо дані лише якщо підключення до Janus успішно ініційовано
        if (!JanusConnection.isJanusReady) return;
        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/rooms`);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            
            // Рендеримо отримані кімнати
            this.renderRooms(await response.json());
        } catch (error) {
            Logger.log(`❌ Помилка завантаження кімнат: ${error.message}`, "ERROR");
        }
    },

    /**
     * Динамічно рендерить HTML-код для всіх кімнат у DOM-контейнер.
     * 
     * @param {Array} rooms Масив об'єктів кімнат, отриманий з сервера
     */
    renderRooms(rooms) {
        const container = document.getElementById("roomsList");
        if (!container) return;

        // Порожній стан, якщо кімнат немає
        if (!rooms || rooms.length === 0) {
            container.innerHTML = `
                <div class="no-rooms">
                    <div class="no-rooms-icon">📭</div>
                    <div class="no-rooms-text">Немає активних кімнат</div>
                </div>`;
            return;
        }

        // Генеруємо HTML для кожної кімнати
        container.innerHTML = rooms.map(room => {
            // Перевіряємо, чи є дана кімната поточною для клієнта
            const isCurrent = RoomManager.currentRoomId === room.id;
            
            // Перевіряємо, чи транслюється зараз радіо в цю кімнату
            const isStreaming = room.stationstate === true;

            // Налаштовуємо кнопки входу/переходу
            const btnClass = isCurrent ? 'btn-action-room' : 'btn-action-room btn-join-room';
            const btnText = isCurrent ? '✓ Ви тут' : '🚪 Увійти';
            const btnDisabled = isCurrent ? 'disabled' : '';
            const btnAction = isCurrent ? '' : `onclick="RoomManager.switchRoom(${room.id})"`;

            const stationId = room.id;
            
            // Кнопки управління трансляцією радіо в кімнату (FFmpegPlain RTP)
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
            let participantsHtml = '<div class="no-users">Немає користувачів</div>';
            if (room.participants && room.participants.length > 0) {
                participantsHtml = '<ul class="user-list">' + room.participants.map(p => {
                    // Кольорове кодування бейджів статусу учасника (говорить/мовчить/muted)
                    let state = p.talking ? 'talking' : (p.muted ? 'muted' : 'active');
                    let stateText = p.talking ? '📢 говорить' : (p.muted ? '🔇 вимкнено' : '🎙️ активний');
                    
                    // Кнопки модерації мікрофона учасника (mute/unmute)
                    const isMuted = p.muted === true;
                    const muteBtn = isMuted
                        ? `<button class="btn-user-action btn-unmute" onclick="Moderator.muteUser(${room.id}, ${p.id}, false)" title="Увімкнути мікрофон">🔊</button>`
                        : `<button class="btn-user-action btn-mute" onclick="Moderator.muteUser(${room.id}, ${p.id}, true)" title="Вимкнути мікрофон">🔇</button>`;
                    
                    // Кнопка модерації kick (вигнання)
                    const kickBtn = `<button class="btn-user-action btn-kick" onclick="Moderator.kickUser(${room.id}, ${p.id})" title="Вигнати учасника">❌</button>`;
                    
                    // Селектор вибору кімнати для переміщення учасника модератором (переміщення працює через UUID/token)
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

                    // Шаблон учасника
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

            // Червоний LIVE імпульсний індикатор, якщо в кімнату йде трансляція ефіру
            const liveBadge = isStreaming
                ? '<span class="badge badge-live-pulse">🔴 LIVE</span>'
                : '';

            // Рендеринг панелі ретрансляції RTP Forward
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

            // Збираємо повну картку кімнати
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

    /**
     * Дозволяє клікати по кнопках кімнат.
     */
    enableRoomButtons() {
        document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = false);
    },

    /**
     * Тимчасово блокує всі кнопки дій під час виконання транзакцій переходу.
     */
    disableRoomButtons() {
        document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = true);
    }
};