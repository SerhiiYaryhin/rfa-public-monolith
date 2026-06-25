/**
 * RFA WebRTC Client - Оновлена версія з токенною авторизацією
 *
 * Архітектура:
 * - Один WebSocket до Janus на всю сесію користувача
 * - Токен = стабільний UUID користувача (отримується з бекенду при завантаженні)
 * - Перехід між кімнатами через leave + join (без перестворення сесії)
 * - Бекенд відповідає тільки за: токени, список кімнат, "евакуацію" з інших кімнат
 */

// ============================================================
// ГЛОБАЛЬНИЙ СТАН
// ============================================================
let janusInstance = null;
let audioBridge = null;
let currentSessionToken = null;
let currentRoomId = null;
let currentDisplay = null;
let isReconnecting = false;

// Конфігурація
// const BACKEND_API = "/api/v1/webrtc/stream";
const BACKEND_API = "/api/v1q/webrtc/stream"; // Новий endpoint
const JANUS_WS_URL = "wss://test.rfa.toloka.media/janus-ws"; // TODO: отримувати з бекенду
const DEFAULT_ROOM_ID = 1; // Кімната за замовчуванням при першому вході
const ROOMS_REFRESH_INTERVAL = 5000; // Оновлення списку кімнат (мс)

// ============================================================
// ЛОГУВАННЯ
// ============================================================
function log(message, type = 'INFO') {
    const consoleDiv = document.getElementById("logConsole");
    if (!consoleDiv) {
        console.log(`[${type}] ${message}`);
        return;
    }

    const time = new Date().toLocaleTimeString();
    const badgeClasses = {
        'INFO': 'badge-info',
        'SUCCESS': 'badge-success',
        'ERROR': 'badge-error',
        'DEBUG': 'badge-debug',
        'WARN': 'badge-error'
    };

    const badgeClass = badgeClasses[type] || 'badge-info';
    consoleDiv.innerHTML += `<div><span class="badge ${badgeClass}">${time}</span> ${message}</div>`;
    consoleDiv.scrollTop = consoleDiv.scrollHeight;
    console.log(`[${type}] ${message}`);
}

// ============================================================
// ІНДИКАТОР СТАТУСУ З'ЄДНАННЯ
// ============================================================
function updateConnectionStatus(connected, text = null) {
    const indicator = document.getElementById("statusIndicator");
    const statusText = document.getElementById("statusText");

    if (indicator) {
        indicator.className = connected
            ? "status-indicator status-connected"
            : "status-indicator status-disconnected";
    }
    if (statusText) {
        statusText.textContent = text || (connected ? "Підключено до Janus" : "Відключено");
    }
}

// ============================================================
// 1. ІНІЦІАЛІЗАЦІЯ СЕСІЇ КОРИСТУВАЧА
// ============================================================
/**
 * Отримує токен користувача з бекенду при завантаженні сторінки.
 * Токен = UUID користувача, вже зареєстрований в Janus на стороні бекенду.
 */
async function initUserSession() {
    log("🔑 Ініціалізація сесії: запит токена у бекенду...", "INFO");

    try {
        const response = await fetch(`${BACKEND_API}/token`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.status === 401) {
            log("❌ Користувач не авторизований. Увійдіть в систему.", "ERROR");
            updateConnectionStatus(false, "Не авторизовано");
            return false;
        }

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();

        // Зберігаємо тільки токен (api_secret більше НЕ передається клієнту!)
        currentSessionToken = data.token;

        log(`✅ Токен отримано: ${currentSessionToken.substring(0, 8)}...`, "SUCCESS");
        return true;

    } catch (error) {
        log(`❌ Помилка отримання токена: ${error.message}`, "ERROR");
        updateConnectionStatus(false, "Помилка авторизації");

        // Повторна спроба через 5 секунд
        setTimeout(initUserSession, 5000);
        return false;
    }
}

// ============================================================
// 2. ІНІЦІАЛІЗАЦІЯ З'ЄДНАННЯ З JANUS (ВИКЛИКАЄТЬСЯ ОДИН РАЗ)
// ============================================================
/**
 * Створює WebSocket-сесію з Janus та прикріплюється до AudioBridge плагіна.
 * Після успішного attach автоматично заходить в дефолтну кімнату.
 */
function initJanusConnection() {
    if (!currentSessionToken) {
        log("❌ Токен відсутній, неможливо ініціалізувати Janus", "ERROR");
        return;
    }

    log("🔌 Ініціалізація WebSocket з'єднання з Janus...", "INFO");

    Janus.init({
        debug: ["error", "warn"],
        callback: function() {
            janusInstance = new Janus({
                server: JANUS_WS_URL,
                token: currentSessionToken, // Токен передається на рівні сесії
                success: function() {
                    log("✅ WebSocket підключено до Janus", "SUCCESS");
                    updateConnectionStatus(true);

                    // Прикріплюємося до AudioBridge плагіна (ОДИН РАЗ на сесію)
                    attachAudioBridgePlugin();
                },
                error: function(error) {
                    log(`❌ Помилка з'єднання з Janus: ${error}`, "ERROR");
                    updateConnectionStatus(false, "Помилка з'єднання");

                    if (!isReconnecting) {
                        isReconnecting = true;
                        log("🔄 Спроба перепідключення через 5 секунд...", "WARN");
                        setTimeout(() => {
                            isReconnecting = false;
                            initJanusConnection();
                        }, 5000);
                    }
                },
                destroyed: function() {
                    log("🔌 Сесію Janus знищено", "INFO");
                    updateConnectionStatus(false);
                }
            });
        }
    });
}

// ============================================================
// 3. ПРИКРІПЛЕННЯ ДО ПЛАГІНА AUDIOBRIDGE
// ============================================================
/**
 * Прикріплюється до плагіна AudioBridge. Викликається один раз після створення сесії.
 */
function attachAudioBridgePlugin() {
    log("🎙️ Прикріплення до плагіна AudioBridge...", "INFO");

    janusInstance.attach({
        plugin: "janus.plugin.audiobridge",
        token: currentSessionToken,
        opaqueId: "audiobridge-" + Janus.randomString(12),

        success: function(pluginHandle) {
            audioBridge = pluginHandle;
            log("✅ Плагін AudioBridge прикріплено", "SUCCESS");

            // Автоматично заходимо в дефолтну кімнату
            switchRoom(DEFAULT_ROOM_ID);
        },

        error: function(error) {
            log(`❌ Помилка прикріплення плагіна: ${error}`, "ERROR");
        },

        onmessage: handleJanusMessage,
        onremotestream: handleRemoteStream,

        oncleanup: function() {
            log("🧹 З'єднання плагіна очищено", "INFO");
        },

        ondetached: function() {
            log("⚠️ Плагін від'єднано сервером", "WARN");
        }
    });
}

// ============================================================
// 4. ПЕРЕХІД МІЖ КІМНАТАМИ (ГОЛОВНА ФУНКЦІЯ)
// ============================================================
/**
 * Перемикає користувача в іншу кімнату.
 *
 * Flow:
 * 1. POST на бекенд → бекенд робить kick з усіх інших кімнат (евакуація)
 * 2. Якщо вже в кімнаті → send({request: "leave"})
 * 3. send({request: "join", room: newRoomId})
 */
async function switchRoom(newRoomId) {
    if (!audioBridge) {
        log("❌ AudioBridge не ініціалізовано, неможливо переключити кімнату", "ERROR");
        return;
    }

    newRoomId = parseInt(newRoomId);
    if (isNaN(newRoomId)) {
        log("❌ Некоректний ID кімнати", "ERROR");
        return;
    }

    log(`🔄 Перехід в кімнату ${newRoomId}...`, "INFO");

    try {
        // 1. Повідомляємо бекенд про бажання перейти в кімнату
        // Бекенд зробить kick з інших кімнат (евакуація)
        const response = await fetch(`${BACKEND_API}/connectclienttoroom`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ roomid: newRoomId })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const serverResponse = await response.json();

        if (!serverResponse.success) {
            throw new Error(serverResponse.error || "Невідома помилка сервера");
        }

        log(`✅ Бекенд підтвердив перехід. Display: "${serverResponse.display}"`, "SUCCESS");

        // 2. Якщо вже в кімнаті — спочатку leave
        if (currentRoomId !== null) {
            log(`👋 Виходимо з кімнати ${currentRoomId}...`, "DEBUG");
            audioBridge.send({ message: { request: "leave" } });
            // Невелика затримка, щоб Janus встиг обробити leave
            await new Promise(resolve => setTimeout(resolve, 200));
        }

        // 3. Заходимо в нову кімнату
        currentRoomId = newRoomId;
        currentDisplay = serverResponse.display;

        joinRoom(newRoomId, currentDisplay);

    } catch (error) {
        log(`❌ Помилка переходу в кімнату: ${error.message}`, "ERROR");
    }
}

/**
 * Відправляє запит join в AudioBridge плагін.
 */
function joinRoom(roomId, display) {
    log(`🚪 Заходимо в кімнату ${roomId} як "${display}"...`, "INFO");

    audioBridge.send({
        message: {
            request: "join",
            room: roomId,
            display: display
            // token передається автоматично з attach
        }
    });
}

// ============================================================
// 5. ОБРОБКА ПОВІДОМЛЕНЬ ВІД JANUS
// ============================================================
/**
 * Обробляє всі повідомлення від плагіна AudioBridge.
 */
function handleJanusMessage(msg, jsep) {
    const event = msg.audiobridge;

    // Успішний вхід в кімнату
    if (event === "joined") {
        const myId = msg.id;
        const myDisplay = msg.display || currentDisplay;
        log(`✅ Увійшли в кімнату ${currentRoomId}. Ваш ID: ${myId}, Display: "${myDisplay}"`, "SUCCESS");

        // Створюємо WebRTC Offer для мікрофона
        if (!jsep) {
            log("🎤 Створюємо WebRTC Offer (мікрофон)...", "DEBUG");
            audioBridge.createOffer({
                media: {
                    audioSend: true,
                    audioRecv: true,
                    video: false
                },
                success: function(localJsep) {
                    log("📤 SDP Offer готовий, надсилаємо configure...", "INFO");
                    audioBridge.send({
                        message: {
                            request: "configure",
                            muted: false
                        },
                        jsep: localJsep
                    });
                },
                error: function(error) {
                    log(`❌ Помилка створення Offer: ${JSON.stringify(error)}`, "ERROR");
                }
            });
        }

        // Оновлюємо список кімнат після входу
        loadRooms();
    }

    // Вихід з кімнати
    if (event === "left") {
        log(`👋 Вийшли з кімнати ${currentRoomId}`, "INFO");
    }

    // Користувача кікнули
    if (event === "kicked") {
        log(`⚠️ Вас було видалено з кімнати`, "WARN");
    }

    // Кімнату знищено
    if (event === "destroyed") {
        log(`💥 Кімнату ${currentRoomId} знищено`, "WARN");
        currentRoomId = null;
    }

    // Обробка WebRTC JSEP (SDP signaling)
    if (jsep) {
        log(`📡 Отримано JSEP типу: ${jsep.type}`, "DEBUG");

        if (jsep.type === "answer") {
            // Відповідь сервера на наш Offer — просто реєструємо
            audioBridge.handleRemoteJsep({
                jsep: jsep,
                success: function() {
                    log("✅ WebRTC handshake завершено", "SUCCESS");
                },
                error: function(err) {
                    log(`❌ Помилка реєстрації Answer: ${JSON.stringify(err)}`, "ERROR");
                }
            });
        } else if (jsep.type === "offer") {
            // Сервер надіслав Offer — генеруємо Answer
            audioBridge.createAnswer({
                jsep: jsep,
                media: {
                    audioSend: true,
                    audioRecv: true,
                    video: false
                },
                success: function(jsepAnswer) {
                    audioBridge.send({
                        message: {
                            request: "configure",
                            muted: false
                        },
                        jsep: jsepAnswer
                    });
                },
                error: function(error) {
                    log(`❌ Помилка createAnswer: ${JSON.stringify(error)}`, "ERROR");
                }
            });
        }
    }
}

// ============================================================
// 6. ОБРОБКА АУДІОПОТОКУ
// ============================================================
/**
 * Викликається, коли від сервера починає надходити аудіопотік.
 */
function handleRemoteStream(stream) {
    log("🔊 Отримано аудіопотік реального часу!", "SUCCESS");

    const player = document.getElementById("audioPlayer");
    if (player) {
        Janus.attachMediaStream(player, stream);
        player.play().catch(e => {
            log(`⚠️ Автопрогравання заблоковано: ${e.message}. Клікніть по сторінці.`, "WARN");
        });
    }
}

// ============================================================
// 7. ЗАВАНТАЖЕННЯ СПИСКУ КІМНАТ
// ============================================================
/**
 * Завантажує список активних кімнат з бекенду та оновлює UI.
 */
async function loadRooms() {
    try {
        const response = await fetch(`${BACKEND_API}/rooms`);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const rooms = await response.json();
        renderRooms(rooms);

    } catch (error) {
        log(`❌ Помилка завантаження кімнат: ${error.message}`, "ERROR");
        const roomsListDiv = document.getElementById("roomsList");
        if (roomsListDiv) {
            roomsListDiv.innerHTML = `<div class="error-box">⚠️ Помилка: ${error.message}</div>`;
        }
    }
}

/**
 * Рендерить список кімнат в UI.
 */
function renderRooms(rooms) {
    const roomsListDiv = document.getElementById("roomsList");
    if (!roomsListDiv) return;

    if (!rooms || rooms.length === 0) {
        roomsListDiv.innerHTML = `
            <div class="no-rooms">
                <div class="no-rooms-icon">📭</div>
                <div class="no-rooms-text">Немає активних кімнат</div>
            </div>`;
        return;
    }

    let html = '';
    rooms.forEach(room => {
        const roomId = room.id;
        const description = room.name;
        const stationId = room.stationId || '';
        const isStreaming = room.stationstate || false;
        const participants = room.participants || [];
        const isCurrentRoom = (currentRoomId === roomId);

        let streamBadge = '';
        let roomClass = 'room-card';
        if (isStreaming) {
            streamBadge = `<span class="badge badge-live-pulse">🔴 LIVE</span>`;
            roomClass += ' room-card-live';
        }
        if (isCurrentRoom) {
            roomClass += ' room-card-current';
            streamBadge += ` <span class="badge" style="background:#667eea;color:white;">📍 Ви тут</span>`;
        }

        let participantsHtml = '';
        if (participants.length === 0) {
            participantsHtml = '<div class="no-users">Немає користувачів</div>';
        } else {
            participantsHtml = '<ul class="user-list">';
            participants.forEach(p => {
                const isTalking = p.talking;
                const isMuted = p.muted;
                let stateBadge = '';
                if (isTalking) {
                    stateBadge = '<span class="state-badge talking">📢 говорить</span>';
                } else if (isMuted) {
                    stateBadge = '<span class="state-badge muted">🔇 вимкнено</span>';
                } else {
                    stateBadge = '<span class="state-badge active">🎙️ активний</span>';
                }

                participantsHtml += `
                    <li class="user-item">
                        <span class="user-avatar">👤</span>
                        <span class="user-name">${p.display || 'Анонім'} <span class="user-id">(${p.id})</span></span>
                        ${stateBadge}
                    </li>`;
            });
            participantsHtml += '</ul>';
        }

        // Кнопка переходу в кімнату (замість старого connectRFAUserToRoom)
        const joinButton = isCurrentRoom
            ? `<button class="btn-action-room" disabled style="opacity:0.5;">✓ Ви в кімнаті</button>`
            : `<button class="btn-action-room btn-join-room" onclick="switchRoom(${roomId})">🚪 Увійти</button>`;

        html += `
            <div class="${roomClass}">
                <div class="room-header">
                    <div class="room-title-container">
                        <span class="room-name">${description}</span>
                        <span class="room-id">ID: ${roomId}</span>
                        ${stationId ? `<span class="station-tag">${stationId}</span>` : ''}
                    </div>
                    ${streamBadge}
                </div>
                <div class="room-body">
                    <div class="participants-title">Учасники (${participants.length}):</div>
                    ${participantsHtml}
                </div>
                <div class="room-actions">
                    ${joinButton}
                    ${isStreaming ? `<button class="btn-action-room btn-stop-stream" onclick="stopStreamFromRoom('${stationId}')">⏹️ Зупинити</button>` : ''}
                </div>
            </div>`;
    });

    roomsListDiv.innerHTML = html;
}

// ============================================================
// 8. АДМІНСЬКІ ДІЇ (ЗАПУСК/ЗУПИНКА СТРИМІВ)
// ============================================================
/**
 * Запускає трансляцію радіостанції в кімнату.
 */
async function startStreamToRoom(stationId) {
    log(`▶️ Запуск стріму: ${stationId}`, "INFO");

    try {
        const response = await fetch(`${BACKEND_API}/start?stationId=${encodeURIComponent(stationId)}`, {
            method: 'POST'
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || `HTTP ${response.status}`);
        }

        const data = await response.json();
        log(`✅ Стрім запущено! Room: ${data.room_id}, Port: ${data.rtp_port}`, "SUCCESS");
        loadRooms();

    } catch (error) {
        log(`❌ Помилка запуску стріму: ${error.message}`, "ERROR");
    }
}

/**
 * Зупиняє трансляцію радіостанції.
 */
async function stopStreamFromRoom(stationId) {
    if (!stationId) {
        log("❌ Station ID не вказано", "ERROR");
        return;
    }

    log(`⏹️ Зупинка стріму: ${stationId}`, "INFO");

    try {
        const response = await fetch(`${BACKEND_API}/stop?stationId=${encodeURIComponent(stationId)}`, {
            method: 'POST'
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || `HTTP ${response.status}`);
        }

        log(`✅ Стрім зупинено`, "SUCCESS");
        loadRooms();

    } catch (error) {
        log(`❌ Помилка зупинки стріму: ${error.message}`, "ERROR");
    }
}

// ============================================================
// 9. ОБРОБКА ПОДІЙ СТОРІНКИ
// ============================================================
document.addEventListener("DOMContentLoaded", async function() {
    log("🚀 Сторінку завантажено, починаємо ініціалізацію...", "INFO");

    // 1. Отримуємо токен користувача з бекенду
    const tokenReady = await initUserSession();
    if (!tokenReady) return;

    // 2. Ініціалізуємо з'єднання з Janus (ОДИН РАЗ)
    initJanusConnection();

    // 3. Завантажуємо список кімнат одразу
    loadRooms();

    // 4. Автооновлення списку кімнат кожні 5 секунд
    setInterval(loadRooms, ROOMS_REFRESH_INTERVAL);
});

// Обробка закриття сторінки — коректне завершення сесії
window.addEventListener("beforeunload", function() {
    if (janusInstance) {
        log("🔌 Закриття сесії Janus...", "DEBUG");
        janusInstance.destroy({ cleanupHandles: true });
    }
});

// ============================================================
// ЕКСПОРТ ФУНКЦІЙ ДЛЯ ВИКОРИСТАННЯ В HTML (onclick)
// ============================================================
window.switchRoom = switchRoom;
window.loadRooms = loadRooms;
window.startStreamToRoom = startStreamToRoom;
window.stopStreamFromRoom = stopStreamFromRoom;