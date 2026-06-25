let janusInstance = null;
let audioBridge = null;
let currentSessionToken = null;
let currentRoomId = null;
let currentDisplay = null;
let isJanusReady = false;
let pendingRoomId = null;
let isReconnecting = false;
let isSwitchingRoom = false;  // ✅ Запобігання повторним клікам
let iceCheckInterval = null;  // ✅ Зберігаємо ID інтервалу ICE

const BACKEND_API = "/api/v1q/webrtc/stream";
const JANUS_WS_URL = "wss://test.rfa.toloka.media/janus-ws";
const DEFAULT_ROOM_ID = 802;
const ROOMS_REFRESH_INTERVAL = 5000;

// ============================================================
// ЛОГУВАННЯ
// ============================================================
function log(message, type = 'INFO') {
    const consoleDiv = document.getElementById("logConsole");
    if (!consoleDiv) { console.log(`[${type}] ${message}`); return; }
    const time = new Date().toLocaleTimeString();
    const badgeClasses = {
        'INFO': 'badge-info', 'SUCCESS': 'badge-success',
        'ERROR': 'badge-error', 'WARN': 'badge-error', 'DEBUG': 'badge-debug'
    };
    const badgeClass = badgeClasses[type] || 'badge-info';
    consoleDiv.innerHTML += `<div><span class="badge ${badgeClass}">${time}</span> ${message}</div>`;
    consoleDiv.scrollTop = consoleDiv.scrollHeight;
    console.log(`[${type}] ${message}`);
}

function updateConnectionStatus(connected, text = null) {
    const indicator = document.getElementById("statusIndicator");
    const statusText = document.getElementById("statusText");
    if (indicator) indicator.className = connected ? "status-indicator status-connected" : "status-indicator status-disconnected";
    if (statusText) statusText.textContent = text || (connected ? "Підключено до Janus" : "Відключено");
}

// ============================================================
// 1. ОТРИМАННЯ ТОКЕНУ
// ============================================================
async function initUserSession() {
    log("🔑 Отримання токена користувача...", "INFO");
    try {
        const response = await fetch(`${BACKEND_API}/token`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        currentSessionToken = data.token;
        log(`✅ Токен отримано: ${currentSessionToken.substring(0, 8)}...`, "SUCCESS");
        return true;
    } catch (error) {
        log(`❌ Помилка отримання токена: ${error.message}`, "ERROR");
        updateConnectionStatus(false, "Помилка авторизації");
        setTimeout(initUserSession, 5000);
        return false;
    }
}

// ============================================================
// 2. ІНІЦІАЛІЗАЦІЯ JANUS
// ============================================================
function initJanusConnection() {
    return new Promise((resolve, reject) => {
        if (!currentSessionToken) return reject("Токен відсутній");
        log("🔌 Підключення до Janus WebSocket...", "INFO");

        Janus.init({
            debug: ["error", "warn"],
            callback: () => {
                janusInstance = new Janus({
                    server: JANUS_WS_URL,
                    token: currentSessionToken,
                    success: () => {
                        log("✅ WebSocket підключено", "SUCCESS");
                        attachAudioBridgePlugin().then(resolve).catch(reject);
                    },
                    error: (error) => {
                        log(`❌ Помилка WebSocket: ${error}`, "ERROR");
                        if (!isReconnecting) {
                            isReconnecting = true;
                            setTimeout(() => { isReconnecting = false; initJanusConnection().then(resolve).catch(reject); }, 5000);
                        } else { reject(error); }
                    },
                    destroyed: () => {
                        log("🔌 Сесію Janus знищено", "INFO");
                        updateConnectionStatus(false);
                        isJanusReady = false;
                    }
                });
            }
        });
    });
}

// ============================================================
// 3. ПРИКРІПЛЕННЯ ПЛАГІНА (ВИПРАВЛЕНО - без targetRoomId)
// ============================================================
function attachAudioBridgePlugin() {
    return new Promise((resolve, reject) => {
        log("🎙️ Прикріплення до AudioBridge...", "INFO");
        janusInstance.attach({
            plugin: "janus.plugin.audiobridge",
            token: currentSessionToken,
            opaqueId: "audiobridge-" + Janus.randomString(12),
            success: (pluginHandle) => {
                audioBridge = pluginHandle;
                isJanusReady = true;
                updateConnectionStatus(true, "Готово до роботи");
                log("✅ AudioBridge прикріплено та готовий", "SUCCESS");
                enableRoomButtons();
                resolve();

                // ✅ ВИПРАВЛЕНО: НЕ викликаємо switchRoom тут!
                // Просто заходимо в дефолтну кімнату або відкладену
                if (pendingRoomId !== null) {
                    const target = pendingRoomId;
                    pendingRoomId = null;
                    currentRoomId = null; // ✅ Скидаємо, щоб не було циклу
                    log(`🚀 Виконую відкладений перехід в кімнату ${target}...`, "INFO");
                    switchRoom(target);
                } else {
                    currentRoomId = null;
                    switchRoom(DEFAULT_ROOM_ID);
                }
            },
            error: (error) => {
                log(`❌ Помилка прикріплення плагіна: ${error}`, "ERROR");
                reject(error);
            },
            onremotetrack: (track, mid, on) => {
                log(`🔊 ${on ? 'Додано' : 'Видалено'} віддалений трек: ${track.kind} (mid=${mid})`, "SUCCESS");

                if (on && track.kind === 'audio') {
                    let audio = document.getElementById("audioPlayer");

                    if (!audio) {
                        audio = document.getElementById("remoteAudio-" + mid);
                        if (!audio) {
                            audio = document.createElement("audio");
                            audio.id = "remoteAudio-" + mid;
                            audio.autoplay = true;
                            audio.controls = true;
                            document.body.appendChild(audio);
                        }
                    }

                    const stream = new MediaStream([track]);
                    audio.srcObject = stream;

                    audio.play().catch(e => {
                        log(`⚠️ Автопрогравання заблоковано: ${e.message}`, "WARN");
                    });
                }
            },
            onlocaltrack: (track, mid) => {
                log(`🎤 Локальний трек додано: ${track.kind} (mid=${mid})`, "DEBUG");
            },
            onmessage: handleJanusMessage,
            oncleanup: () => log("🧹 З'єднання плагіна очищено", "INFO"),
            ondetached: () => log("⚠️ Плагін від'єднано сервером", "WARN")
        });
    });
}

// ============================================================
// 4. ПЕРЕХІД МІЖ КІМНАТАМИ (ВИПРАВЛЕНО - без detach/attach)
// ============================================================
async function switchRoom(newRoomId) {
    newRoomId = parseInt(newRoomId);
    if (isNaN(newRoomId)) return;

    // ✅ Запобігання повторним клікам під час переходу
    if (isSwitchingRoom) {
        log(`⏳ Вже виконується перехід, ігноруємо запит`, "WARN");
        return;
    }
    isSwitchingRoom = true;

    try {
        if (!isJanusReady || !audioBridge) {
            log(`⏳ Janus підключається. Кімнату ${newRoomId} буде обрано автоматично.`, "WARN");
            pendingRoomId = newRoomId;
            return;
        }

        log(`🔄 Перехід в кімнату ${newRoomId}...`, "INFO");

        // 1. POST на бекенд
        const response = await fetch(`${BACKEND_API}/connectclienttoroom`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({roomid: newRoomId})
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const serverResponse = await response.json();
        if (!serverResponse.success) throw new Error(serverResponse.error || "Помилка сервера");
        log(`✅ Бекенд підтвердив перехід. Display: "${serverResponse.display}"`, "SUCCESS");

        // 2. Якщо вже в кімнаті - очищаємо WebRTC і робимо leave
        if (currentRoomId !== null) {
            log(`👋 Вихід з кімнати ${currentRoomId}...`, "DEBUG");

            // ✅ Очищаємо WebRTC (але НЕ обнуляємо audioBridge!)
            cleanupWebRTCHandle();

            audioBridge.send({message: {request: "leave"}});
            await new Promise(r => setTimeout(r, 300));
        }

        // 3. Оновлюємо state
        currentRoomId = newRoomId;
        currentDisplay = serverResponse.display;

        // 4. Заходимо в нову кімнату (без detach/attach!)
        joinRoom(newRoomId, currentDisplay);

    } catch (error) {
        log(`❌ Помилка переходу: ${error.message}`, "ERROR");
    } finally {
        isSwitchingRoom = false;
    }
}

/**
 * ✅ НОВА ФУНКЦІЯ: Очищення WebRTC handle БЕЗ обнулення audioBridge
 */
function cleanupWebRTCHandle() {
    try {
        // ✅ Зупиняємо інтервал перевірки ICE
        if (iceCheckInterval) {
            clearInterval(iceCheckInterval);
            iceCheckInterval = null;
            log("🛑 Інтервал перевірки ICE зупинено", "DEBUG");
        }

        if (audioBridge && audioBridge.webrtcStuff) {
            // Закриваємо PeerConnection
            if (audioBridge.webrtcStuff.pc) {
                log("🧹 Закриваємо RTCPeerConnection...", "DEBUG");
                try {
                    audioBridge.webrtcStuff.pc.close();
                } catch (e) {
                    log(`⚠️ Помилка закриття PC: ${e.message}`, "WARN");
                }
                audioBridge.webrtcStuff.pc = null;
            }

            // Зупиняємо локальні треки (мікрофон)
            if (audioBridge.webrtcStuff.myStream) {
                log("🧹 Зупиняємо локальні треки...", "DEBUG");
                audioBridge.webrtcStuff.myStream.getTracks().forEach(track => {
                    track.stop();
                });
                audioBridge.webrtcStuff.myStream = null;
            }

            // Скидаємо webrtcStuff в початковий стан
            audioBridge.webrtcStuff = {};
            log("✅ WebRTC очищено", "DEBUG");
        }
    } catch (e) {
        log(`⚠️ Помилка очищення WebRTC (не критично): ${e.message}`, "WARN");
    }
}

function joinRoom(roomId, display) {
    log(`🚪 Заходимо в кімнату ${roomId} як "${display}"...`, "INFO");
    const joinMessage = {
        request: "join",
        room: roomId,
        display: display,
        muted: false
    };
    log(`📤 Відправка: ${JSON.stringify(joinMessage)}`, "DEBUG");
    audioBridge.send({message: joinMessage});
}

// ============================================================
// 5. ОБРОБКА ПОВІДОМЛЕНЬ JANUS (ВИПРАВЛЕНО)
// ============================================================
function handleJanusMessage(msg, jsep) {
    log(`📥 Отримано від Janus: ${JSON.stringify(msg).substring(0, 300)}...`, "DEBUG");
    const event = msg.audiobridge;

    if (msg.error_code || msg.error) {
        log(`❌ Помилка від Janus: ${msg.error_code} - ${msg.error}`, "ERROR");
        return;
    }

    // Обробляємо ТІЛЬКИ наш власний joined (з полем id)
    if (event === "joined" && msg.id) {
        const myId = msg.id;
        const myDisplay = msg.display || currentDisplay;
        log(`✅ Увійшли в кімнату ${currentRoomId}. Ваш ID: ${myId}, Display: "${myDisplay}"`, "SUCCESS");
        loadRooms();

        if (!jsep) {
            log("🎤 Створюємо WebRTC Offer (мікрофон)...", "DEBUG");
            const offerStartTime = Date.now();

            audioBridge.createOffer({
                media: {audioSend: true, audioRecv: true, video: false},
                success: (localJsep) => {
                    const elapsed = Date.now() - offerStartTime;
                    log(`📤 SDP Offer готовий (за ${elapsed}мс). Надсилаємо configure...`, "INFO");
                    audioBridge.send({
                        message: {request: "configure", muted: false},
                        jsep: localJsep
                    });
                },
                error: (err) => log(`❌ Offer Error: ${JSON.stringify(err)}`, "ERROR")
            });
        }
    }

    // Повідомлення про вхід іншого користувача
    if (event === "joined" && !msg.id) {
        log(`👤 Інший користувач увійшов в кімнату`, "INFO");
        loadRooms();
    }

    if (event === "left") { log(`👋 Хтось вийшов з кімнати`, "INFO"); loadRooms(); }
    if (event === "kicked") log(`⚠️ Вас видалено з кімнати`, "WARN");
    if (event === "destroyed") { currentRoomId = null; log(`💥 Кімнату знищено`, "WARN"); }

    if (jsep) {
        log(`📡 Отримано JSEP типу: ${jsep.type}`, "DEBUG");

        if (jsep.type === "answer") {
            audioBridge.handleRemoteJsep({
                jsep: jsep,
                success: () => {
                    log("✅ WebRTC Handshake завершено", "SUCCESS");

                    // ✅ Зупиняємо попередній інтервал якщо є
                    if (iceCheckInterval) {
                        clearInterval(iceCheckInterval);
                    }

                    // ✅ Перевіряємо ICE state кожні 2 секунди
                    let checkCount = 0;
                    iceCheckInterval = setInterval(() => {
                        // ✅ Безпечна перевірка - audioBridge може бути null
                        if (!audioBridge || !audioBridge.webrtcStuff || !audioBridge.webrtcStuff.pc) {
                            log("⚠️ PeerConnection відсутній, зупиняємо перевірку ICE", "WARN");
                            clearInterval(iceCheckInterval);
                            iceCheckInterval = null;
                            return;
                        }

                        const pc = audioBridge.webrtcStuff.pc;
                        log(`🔍 ICE: ${pc.iceConnectionState} (${++checkCount})`, "DEBUG");

                        if (pc.iceConnectionState === "connected" || pc.iceConnectionState === "completed") {
                            log("✅ ICE з'єднання встановлено!", "SUCCESS");
                            clearInterval(iceCheckInterval);
                            iceCheckInterval = null;
                        } else if (pc.iceConnectionState === "failed") {
                            log("❌ ICE FAILED! Потрібен TURN сервер", "ERROR");
                            clearInterval(iceCheckInterval);
                            iceCheckInterval = null;
                        }

                        if (checkCount >= 15) {
                            log("⚠️ ICE не встановився за 30 секунд", "WARN");
                            clearInterval(iceCheckInterval);
                            iceCheckInterval = null;
                        }
                    }, 2000);
                },
                error: (err) => log(`❌ Answer Error: ${JSON.stringify(err)}`, "ERROR")
            });
        } else if (jsep.type === "offer") {
            audioBridge.createAnswer({
                jsep: jsep,
                media: {audioSend: true, audioRecv: true, video: false},
                success: (jsepAnswer) => {
                    audioBridge.send({
                        message: {request: "configure", muted: false},
                        jsep: jsepAnswer
                    });
                },
                error: (err) => log(`❌ CreateAnswer Error: ${JSON.stringify(err)}`, "ERROR")
            });
        }
    }
}

// ============================================================
// 6. ЗАВАНТАЖЕННЯ КІМНАТ
// ============================================================
async function loadRooms() {
    if (!isJanusReady) return;
    try {
        const response = await fetch(`${BACKEND_API}/rooms`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        renderRooms(await response.json());
    } catch (error) {
        log(`❌ Помилка завантаження кімнат: ${error.message}`, "ERROR");
    }
}

function renderRooms(rooms) {
    const container = document.getElementById("roomsList");
    if (!container) return;
    if (!rooms || rooms.length === 0) {
        container.innerHTML = `<div class="no-rooms"><div class="no-rooms-icon">📭</div><div class="no-rooms-text">Немає активних кімнат</div></div>`;
        return;
    }
    container.innerHTML = rooms.map(room => {
        const isCurrent = currentRoomId === room.id;
        const btnClass = isCurrent ? 'btn-action-room' : 'btn-action-room btn-join-room';
        const btnText = isCurrent ? '✓ Ви тут' : '🚪 Увійти';
        const btnDisabled = isCurrent ? 'disabled' : '';
        const btnAction = isCurrent ? '' : `onclick="switchRoom(${room.id})"`;

        let participantsHtml = '<div class="no-users">Немає користувачів</div>';
        if (room.participants && room.participants.length > 0) {
            participantsHtml = '<ul class="user-list">' + room.participants.map(p => {
                let state = p.talking ? 'talking' : (p.muted ? 'muted' : 'active');
                let stateText = p.talking ? '📢 говорить' : (p.muted ? '🔇 вимкнено' : '🎙️ активний');
                return `<li class="user-item"><span class="user-avatar">👤</span><span class="user-name">${p.display || 'Анонім'} <span class="user-id">(${p.id})</span></span><span class="state-badge ${state}">${stateText}</span></li>`;
            }).join('') + '</ul>';
        }

        return `
        <div class="room-card ${room.stationstate ? 'room-card-live' : ''} ${isCurrent ? 'room-card-current' : ''}">
            <div class="room-header">
                <div class="room-title-container">
                    <span class="room-name">${room.name}</span>
                    <span class="room-id">ID: ${room.id}</span>
                </div>
                ${room.stationstate ? '<span class="badge badge-live-pulse">🔴 LIVE</span>' : ''}
            </div>
            <div class="room-body">
                <div class="participants-title">Учасники (${room.participants ? room.participants.length : 0}):</div>
                ${participantsHtml}
            </div>
            <div class="room-actions">
                <button class="${btnClass}" ${btnDisabled} ${btnAction}>${btnText}</button>
            </div>
        </div>`;
    }).join('');
}

function enableRoomButtons() { document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = false); }
function disableRoomButtons() { document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = true); }

// ============================================================
// 7. ІНІЦІАЛІЗАЦІЯ
// ============================================================
document.addEventListener("DOMContentLoaded", async () => {
    log("🚀 Сторінку завантажено. Ініціалізація...", "INFO");
    disableRoomButtons();
    const tokenReady = await initUserSession();
    if (!tokenReady) return;
    try {
        await initJanusConnection();
        log("🟢 Система готова до роботи!", "SUCCESS");
    } catch (error) {
        log(`❌ Критична помилка ініціалізації: ${error}`, "ERROR");
    }
    loadRooms();
    setInterval(loadRooms, ROOMS_REFRESH_INTERVAL);
});

window.addEventListener("beforeunload", () => {
    if (iceCheckInterval) clearInterval(iceCheckInterval);
    if (janusInstance) janusInstance.destroy({cleanupHandles: true});
});

window.switchRoom = switchRoom;
window.loadRooms = loadRooms;