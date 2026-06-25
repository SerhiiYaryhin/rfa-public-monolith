let janusInstance = null;
let audioBridge = null;
// Глобальний стан сесії користувача для Janus
let currentSessionToken = null;
let janusApiSecret = null;

// URL вашого Spring Boot бекенду (підставте правильний префікс вашого API)
const BACKEND_TOKEN_URL = "/api/v1/webrtc/stream/token";


const serverUrl = "wss://test.rfa.toloka.media/janus-ws"; // todo 260625 Замінити на правильне отримання



/**
 * Вивід логів на сторінку для відладки
 */
function log(message, type = 'INFO') {
    const consoleDiv = document.getElementById("logConsole");
    const time = new Date().toLocaleTimeString();
    let badgeClass = 'badge-info';
    if (type === 'SUCCESS') badgeClass = 'badge-success';
    if (type === 'ERROR') badgeClass = 'badge-error';
    if (type === 'DEBUG') badgeClass = 'badge-debug'; // Використовуйте 'DEBUG' для дебаг-повідомлень

    consoleDiv.innerHTML += `<div><span class="badge ${badgeClass}">${time}</span> ${message}</div>`;
    consoleDiv.scrollTop = consoleDiv.scrollHeight;
    console.log(`[${type}] ${message}`); // Додатковий вивід у консоль браузера
}

/**
 * Автоматично запитує у бекенда токен користувача та реєструє його сесію.
 */
async function initUserSession() {
    log("Ініціалізація сесії користувача, запит токена...", "INFO");
    try {
        const response = await fetch(BACKEND_TOKEN_URL, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
                // Якщо використовується Spring Security з JWT, додайте сюди Authorization заголовок
            }
        });

        if (response.status === 401) {
            log("Помилка: Користувач не авторизований на бекенді. Будь ласка, увійдіть в систему.", "ERROR");
            return;
        }

        if (!response.ok) {
            throw new Error(`Помилка сервера: ${response.status}`);
        }

        const data = await response.json();

        // Зберігаємо отримані дані в глобальні змінні
        currentSessionToken = data.token;
        janusApiSecret = data.api_secret; // Тимчасово, поки бекенд його повертає

        // Виведемо токен на екран, щоб бачити, що він завантажився:
        const tokenInput = document.getElementById("customToken");
        if (tokenInput) {
            tokenInput.value = currentSessionToken;
        }

        log(`Токен успішно отримано та активовано в Janus: ${currentSessionToken.substring(0, 8)}...`, "SUCCESS");

        // Тут можна розблокувати кнопки інтерфейсу (наприклад, "Увійти в кімнату")
        document.getElementById("joinRoomBtn").disabled = false;

    } catch (error) {
        log(`Не вдалося отримати токен від бекенду: ${error.message}`, "ERROR");
        // Пробуємо повторити через 5 секунд у разі мережевого збою
        setTimeout(initUserSession, 5000);
    }
}

/**
 * Перемикач полів авторизації
 */
function toggleAuthFields() {
    const authType = document.querySelector('input[name="authType"]:checked').value;
    const tokenContainer = document.getElementById("tokenFieldContainer");
    const secretContainer = document.getElementById("secretFieldContainer");
    const tokenInput = document.getElementById("customToken");
    const secretInput = document.getElementById("customSecret");

    // Скидання значень та стану полів
    tokenInput.value = "";
    tokenInput.disabled = true;
    secretInput.value = "janusrocks"; // Завжди встановлюємо дефолтний секрет

    tokenContainer.style.display = 'none';
    secretContainer.style.display = 'none';

    if (authType === 'token' || authType === 'both') {
        tokenContainer.style.display = 'block';
        tokenInput.placeholder = "Буде отримано автоматично при підключенні";
    }
    if (authType === 'manual_token') {
        tokenContainer.style.display = 'block';
        tokenInput.disabled = false;
        tokenInput.placeholder = "Введіть ваш токен тут";
    }
    if (authType === 'secret' || authType === 'both') {
        secretContainer.style.display = 'block';
    }
}

/**
 * Завантаження списку кімнат з API та відображення
 */
async function loadRooms() {
    log("Завантаження активних кімнат...", 'DEBUG');
    try {
        const response = await fetch('/api/v1/webrtc/stream/rooms');
        if (!response.ok) {
            throw new Error("HTTP status " + response.status);
        }
        const rooms = await response.json();

        const roomsListDiv = document.getElementById("roomsList");
        if (!rooms || rooms.length === 0) {
            roomsListDiv.innerHTML = `
                    <div class="no-rooms">
                        <div class="no-rooms-icon">📭</div>
                        <div class="no-rooms-text">Немає активних кімнат у Janus.</div>
                    </div>`;
            return;
        }

        let html = '';
        rooms.forEach(room => {
            const isStreaming = room.is_streaming; // Перевірка, чи є RTP стрім (індикатор LIVE)
            const roomId = room.id;
            const description = room.name;
            const stationId = room.station_id || ''; // Якщо є інфа про станцію, напр. з БД
            const rtpPort = room.rtp_port || ''; // Порт, на який йде трансляція
            const participants = room.participants || []; // Оголошення змінної participants

            let streamBadge = '';
            let roomClass = 'room-card';
            if (isStreaming) {
                streamBadge = `<span class="badge badge-live-pulse">🔴 LIVE</span>`;
                roomClass += ' room-card-live';
            }

            let participantsHtml = '';
            if (participants.length === 0) {
                participantsHtml = '<div class="no-users">Немає користувачів у кімнаті</div>';
            } else {
                participantsHtml = '<ul class="user-list">';
                participants.forEach(p => {
                    const isMuted = p.muted;
                    const isTalking = p.talking;
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

            html += `
                    <div class="${roomClass}">
                        <div class="room-header">
                            <div class="room-title-container">
                                <span class="room-name">${description}</span>
                                <span class="room-id">ID: ${roomId}</span>
                                ${stationId ? `<span class="station-tag">${stationId}</span>` : ''}
                                ${rtpPort ? `<span class="rtp-port-tag">RTP: ${rtpPort}</span>` : ''}
                            </div>
                            ${streamBadge}
                        </div>
                        <div class="room-body">
                            <div class="participants-title">Учасники (${participants.length}):</div>
                            ${participantsHtml}
                        </div>
                        <div class="room-actions">
                            ${isStreaming ? `<button class="btn-action-room btn-stop-stream" onclick="stopStreamFromRoom('${stationId}')">⏹️ Зупинити радіо</button>` : ''}
                            <button class="btn-action-room" onclick="startStreamToRoom(${roomId})">🔌 Стрім</button>
                            <button class="btn-action-room" onclick="connectRFAUserToRoom(${roomId})">🔌 Підключити користувача</button>
                        </div>
                    </div>`;
        });
        roomsListDiv.innerHTML = html;
    } catch (e) {
        log("Помилка завантаження моніторингу кімнат: " + e.message, 'ERROR');
        document.getElementById("roomsList").innerHTML = `
                <div class="error-box">
                    ⚠️ Помилка завантаження даних з API: ${e.message}
                </div>`;
    }
}

/**
 * Вибір кімнати та підключення
 */
function selectAndConnect(roomId) {
    document.getElementById("roomId").value = roomId;
    // Можливо, також встановити stationId, якщо він є в room об'єкті
    // document.getElementById("stationId").value = room.station_id;
    initiateRFAConnection();
}

/**
 * Підключення користувача чату RFA до кімнати
 * Додано ключове слово async перед функцією!
 */
async function connectRFAUserToRoom(roomId) {
    log(`Підключення активного користувача до audioroom з ID: ${roomId} ...`, 'DEBUG');

    try {
        // 1. Робимо POST запит до вашого Spring Boot API
        const response = await fetch('/api/v1/webrtc/stream/connectclienttoroom', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            // Передаємо в body наш JSON-об'єкт. Ключ "roomid" має збігатися з тим, що чекає Java
            body: JSON.stringify({ roomid: roomId })
        });

        if (!response.ok) {
            throw new Error("HTTP status " + response.status);
        }

        // 2. Отримуємо JSON-відповідь від RadioLiveService (success, token, roomid, display)
        const serverResponse = await response.json();

        if (serverResponse.success) {
            log(`Токен успішно зареєстровано в Janus: ${serverResponse.token}`, 'INFO');

            // 3. Передаємо отримані дані (токен, id кімнати, ім'я) в функцію ініціалізації WebRTC
            initiateRFAConnection(serverResponse.roomid, serverResponse.token, serverResponse.display);

        } else {
            // Якщо сервер повернув success: false (наприклад, користувач не залогінений)
            throw new Error(serverResponse.error || "Невідома помилка сервера");
        }

    } catch (e) {
        log("Помилка підключення активного користувача до audioroom: " + e.message, 'ERROR');
        document.getElementById("roomsList").innerHTML = `
                <div class="error-box">
                    ⚠️ Помилка авторизації в аудіокімнаті: ${e.message}
                </div>`;
    }
}

/**
 * Точка входу для підключення авторизованого RFA користувача.
 * Викликається після успішної реєстрації токена на бекенді.
 */
function initiateRFAConnection(roomId, token, display) {
    log(`Ініціалізація RFA з'єднання: Кімната=${roomId}, Користувач="${display}"`, 'INFO');

    // Блокуємо або оновлюємо інтерфейс за потреби
    const connectBtn = document.getElementById("connectBtn");
    if (connectBtn) connectBtn.disabled = true;

    // Конфігурація для Janus екземпляру
    let janusConfig = {
        server: serverUrl,
        token: token, // Передаємо отриманий UUID користувача як токен доступу до шлюзу
        success: function () {
            log("WebSocket підключено до Janus (авторизована сесія)", 'SUCCESS');
            // Прикріплюємося до плагіна, передаючи також відображуване ім'я користувача
            attachAudioBridgePlugin(roomId, token, undefined, display);
        },
        error: function (error) {
            log("Помилка авторизованої сесії Janus: " + error, 'ERROR');
            if (connectBtn) connectBtn.disabled = false;
        },
        destroyed: function () {
            log("Сесію Janus знищено.", 'INFO');
            if (connectBtn) connectBtn.disabled = false;
        }
    };

    // Очищаємо попереднє з'єднання, якщо воно було активне
    if (janusInstance) {
        log("Закриття попередньої сесії Janus...", 'DEBUG');
        janusInstance.destroy();
        janusInstance = null;
    }

    // Ініціалізація Janus бібліотеки
    Janus.init({
        debug: ["error", "warn"], // "all" для детального логування WebRTC в консолі
        callback: function () {
            janusInstance = new Janus(janusConfig);
        }
    });
}


/**
 * Ініціалізація з'єднання з Janus з вибором типу авторизації
 */
async function initiateConnection() {
    const roomId = parseInt(document.getElementById("roomId").value);
    if (isNaN(roomId)) {
        alert("Будь ласка, введіть Room ID!");
        return;
    }

    const authType = document.querySelector('input[name="authType"]:checked').value;
    let token = null;
    let apisecret = null; // Будемо використовувати це поле для api_secret

    document.getElementById("connectBtn").disabled = true;
    log("Розпочинаємо ініціалізацію з'єднання...", 'INFO');

    // 1. Отримання токена (якщо потрібно)
    if (authType === 'token' || authType === 'both') {
        log("Запит токена та api_secret в API...", 'DEBUG');
        try {
            const response = await fetch('/api/v1/webrtc/stream/token');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            token = data.token;
            apisecret = data.api_secret; // Отримуємо api_secret від бекенда
            document.getElementById("customToken").value = token;
            document.getElementById("customSecret").value = apisecret; // Відображаємо отриманий секрет
            log(`Токен отримано: ${token.substring(0, 10)}... (API Secret: ${apisecret.substring(0, 5)}...)`, 'SUCCESS');
        } catch (e) {
            log("Помилка отримання токена/api_secret: " + e.message, 'ERROR');
            document.getElementById("connectBtn").disabled = false;
            return;
        }
    } else if (authType === 'manual_token') {
        const fieldToken = document.getElementById("customToken").value.trim();
        if (fieldToken) {
            token = fieldToken;
            log(`Використовуємо вказаний вручну токен: ${token.substring(0, 10)}...`, 'INFO');
        } else {
            log("Помилка: Токен не вказано для ручного режиму!", 'ERROR');
            document.getElementById("connectBtn").disabled = false;
            return;
        }
    }

    // 2. Отримання секрету (якщо потрібно)
    if (authType === 'secret' || authType === 'both' || (authType === 'manual_token' && document.getElementById("customSecret").value.trim())) {
        const fieldSecret = document.getElementById("customSecret").value.trim();
        if (fieldSecret) {
            apisecret = fieldSecret;
        } else if (authType === 'secret' || authType === 'both') {
            log("Помилка: API Secret не вказано для обраного режиму!", 'ERROR');
            document.getElementById("connectBtn").disabled = false;
            return;
        }
    } else if (authType === 'none') {
        apisecret = undefined; // Явно встановлюємо undefined для режиму "Без паролів"
    }

    startJanusStream(roomId, token, apisecret);
}

/**
 * Запускає трансляцію радіостанції в обрану кімнату.
 */
async function startStreamToRoom(stationId) {
    //const stationId = document.getElementById("stationId").value.trim();
    //const streamUrl = document.getElementById("streamUrl").value.trim();
    //
    // if (!stationId) {
    //     alert("Будь ласка, введіть Station ID!");
    //     return;
    // }
    // if (!streamUrl) {
    //     alert("Будь ласка, введіть Stream URL!");
    //     return;
    // }

    log(`Запуск потоку: Station ID=${stationId}`, 'INFO');
    try {
        const response = await fetch('/api/v1/webrtc/stream/start', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: new URLSearchParams({stationId: stationId})
        });
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`HTTP error! status: ${response.status}, message: ${errorData.message}`);
        }
        const data = await response.json();
        log(`Потік запущено успішно! Room ID: ${data.room_id}, RTP Port: ${data.rtp_port}`, 'SUCCESS');
        document.getElementById("roomId").value = data.room_id; // Автозаповнення Room ID
        loadRooms(); // Оновити список кімнат
    } catch (e) {
        log("Помилка запуску потоку: " + e.message, 'ERROR');
    }
}

/**
 * Зупиняє трансляцію радіостанції.
 */
async function stopStreamFromRoom(stationIdToStop = null) {
    let currentStationId = stationIdToStop;
    if (!currentStationId) {
        currentStationId = document.getElementById("stationId").value.trim();
    }

    if (!currentStationId) {
        alert("Будь ласка, введіть Station ID або виберіть стрім для зупинки!");
        return;
    }

    log(`Зупинка потоку: Station ID=${currentStationId}`, 'INFO');
    try {
        const response = await fetch('/api/v1/webrtc/stream/stop', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: new URLSearchParams({stationId: currentStationId})
        });
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`HTTP error! status: ${response.status}, message: ${errorData.message}`);
        }
        const data = await response.json();
        log(`Потік зупинено успішно! Station ID: ${data.station_id}`, 'SUCCESS');
        loadRooms(); // Оновити список кімнат
    } catch (e) {
        log("Помилка зупинки потоку: " + e.message, 'ERROR');
    }
}


/**
 * Ініціалізує Janus.js та підключається до кімнати
 */
function startJanusStream(roomId, token, apisecret) {
    log("Ініціалізація Janus з параметрами: " +
        `Token: ${token ? token.substring(0, 10) + '...' : 'N/A'}, ` +
        `API Secret: ${apisecret ? apisecret.substring(0, 5) + '...' : 'N/A'}`, 'DEBUG');

    // Конфігурація для Janus екземпляру
    let janusConfig = {
        server: serverUrl,
        success: function () {
            log("WebSocket підключено до Janus", 'SUCCESS');
            // Після успішного підключення, прикріплюємося до плагіна
            attachAudioBridgePlugin(roomId, token, apisecret);
        },
        error: function (error) {
            log("Помилка сесії Janus: " + error, 'ERROR');
            document.getElementById("connectBtn").disabled = false;
        },
        destroyed: function () {
            log("Сесію Janus знищено.", 'INFO');
            document.getElementById("connectBtn").disabled = false;
        }
    };

    // Додаємо авторизаційні параметри для створення сесії
    if (token) janusConfig.token = token;
    if (apisecret) janusConfig.apisecret = apisecret;

    // Знищуємо попередній інстанс якщо він існує
    if (janusInstance) {
        log("Знищення попередньої сесії Janus...", 'DEBUG');
        janusInstance.destroy();
        janusInstance = null; // Скидаємо інстанс
    }

    // Глобальна ініціалізація Janus.js, потім створення інстансу
    Janus.init({
        debug: "all",
        callback: function () {
            janusInstance = new Janus(janusConfig);
        }
    });
}


/**
 * Прикріплення до плагіна AudioBridge та вхід у кімнату (ОНОВЛЕНО)
 * Додано 4-й аргумент customDisplay, щоб відображати реальне ім'я користувача
 */
/**
 * Прикріплення до плагіна AudioBridge та вхід у кімнату із запуск WebRTC Handshake
 */
function attachAudioBridgePlugin(roomId, token, apisecret, customDisplay = null) {
    let attachConfig = {
        plugin: "janus.plugin.audiobridge",
        success: function (pluginHandle) {
            audioBridge = pluginHandle;
            log("Плагін AudioBridge прикріплено.", 'SUCCESS');

            // Визначаємо фінальне ім'я користувача
            const finalDisplay = customDisplay ? customDisplay : ("Web-Tester-" + Janus.randomString(4));

            let join = {
                request: "join",
                room: roomId,
                display: finalDisplay
            };

            if (token) join.token = token;

            log(`Надсилаємо запит на вхід у кімнату: ${roomId} під ім'ям: "${finalDisplay}"`, 'INFO');
            audioBridge.send({message: join});
        },
        error: function (error) {
            log("Помилка прикріплення плагіна: " + error, 'ERROR');
            const connectBtn = document.getElementById("connectBtn");
            if (connectBtn) connectBtn.disabled = false;
        },
        onmessage: function (msg, jsep) {
            log("Отримано подію Janus від плагіна: " + JSON.stringify(msg), 'DEBUG');

            const event = msg["audiobridge"];

            // 1. Опрацювання успішного входу (joined)
            if (event && event === "joined") {
                // Беремо ім'я, яке повернув Janus, або наше локальне
                const myDisplay = msg["display"] || customDisplay || "Користувач";
                const myId = msg["id"];
                log(`Ви успішно увійшли в кімнату під ім'ям: "${myDisplay}". Ваш ID у Janus: ${myId}`, 'SUCCESS');

                // Якщо сервер разом із подією "joined" не прислав jsep (а він його не шле),
                // ми ПЕРШИМИ створюємо локальний WebRTC Offer для публікації мікрофона
                if (jsep === undefined || jsep === null) {
                    log("Ініціюємо створення локального WebRTC Offer (мікрофон)...", 'DEBUG');
                    audioBridge.createOffer({
                        media: { audioSend: true, audioRecv: true, video: false },
                        success: function (localJsep) {
                            log("Локальний SDP Offer сформовано. Надсилаємо конфігурацію в Janus...", 'INFO');
                            let configure = { request: "configure", muted: false };
                            audioBridge.send({ message: configure, jsep: localJsep });
                        },
                        error: function (error) {
                            log("Помилка створення локального WebRTC Offer: " + JSON.stringify(error), 'ERROR');
                        }
                    });
                }
            }

            // 2. Обробка WebRTC JSEP сигналінгу від сервера
            if (jsep) {
                log(`Обробка JSEP від сервера (Тип: ${jsep.type})...`, 'INFO');

                if (jsep.type === "answer") {
                    // КРИТИЧНЕ ВИПРАВЛЕННЯ: якщо прийшла відповідь на наш мікрофон,
                    // ми просто віддаємо її плагіну для завершення з'єднання!
                    audioBridge.handleRemoteJsep({
                        jsep: jsep,
                        success: function() {
                            log("Медіа-рукостискання (Answer) успішно завершено!", "SUCCESS");
                        },
                        error: function(err) {
                            log("Помилка реєстрації Answer: " + JSON.stringify(err), "ERROR");
                        }
                    });
                } else if (jsep.type === "offer") {
                    // Якщо ініціатором Offer виступає сервер (наприклад, для старих методів прослуховування)
                    log("Сервер прислав Offer. Генеруємо локальний Answer...", 'DEBUG');
                    audioBridge.createAnswer({
                        jsep: jsep,
                        media: { audioSend: true, audioRecv: true, video: false },
                        success: function (jsepAnswer) {
                            log("SDP Answer сформовано. Завершуємо Handshake...", 'INFO');
                            let configure = { request: "configure", muted: false };
                            audioBridge.send({ message: configure, jsep: jsepAnswer });
                        },
                        error: function (error) {
                            log("WebRTC createAnswer Error: " + JSON.stringify(error), 'ERROR');
                        }
                    });
                }
            }
        },
        onremotestream: function (stream) {
            log("УСПІХ: Отримано аудіопотік реального часу!", 'SUCCESS');
            const player = document.getElementById("audioPlayer");
            if (player) {
                Janus.attachMediaStream(player, stream);
                player.play().catch(e => {
                    log("Автопрогравання звуку заблоковано браузером. Клікніть мишкою по сторінці.", "WARN");
                });
            }
        },
        oncleanup: function () {
            log("З'єднання плагіна очищено.", 'INFO');
        }
    };

    // Додаємо авторизаційні параметри для прикріплення плагіна
    if (token) attachConfig.token = token;
    if (apisecret) attachConfig.apisecret = apisecret;

    janusInstance.attach(attachConfig);
}

function connectToAudioBridge(roomId, displayName) {
    if (!currentSessionToken) {
        log("Помилка: Сесія не ініціалізована. Токен відсутній.", "ERROR");
        return;
    }

    let attachConfig = {
        plugin: "janus.plugin.audiobridge",
        opaqueId: "audiobridge-" + Janus.randomString(12),
        success: function (pluginHandle) {
            audioBridge = pluginHandle;
            log("Плагін AudioBridge успішно прикріплено.", "SUCCESS");

            // Крок реєстрації в кімнаті Janus
            let register = {
                request: "join",
                room: parseInt(roomId),
                display: displayName
            };
            audioBridge.send({ message: register });
        },
        error: function (error) {
            log("Помилка прикріплення плагіна: " + error, "ERROR");
        },
        consentDialog: function (on) {
            log("Consent dialog: " + on, "DEBUG");
        },
        // ... ваші інші колбеки (onmessage, onremotestream тощо)
    };

    // КРИТИЧНО ВАЖЛИВО: Передаємо наш індивідуальний UUID-токен користувача
    attachConfig.token = currentSessionToken;

    if (janusApiSecret) {
        attachConfig.apisecret = janusApiSecret;
    }

    log(`Підключення до плагіна з токеном: ${currentSessionToken.substring(0, 8)}...`, "INFO");
    janusInstance.attach(attachConfig);
}

// Запускати функції при завантаженні сторінки
document.addEventListener("DOMContentLoaded", function () {
    toggleAuthFields(); // Встановлює початковий стан полів авторизації
    // 1. Запускаємо автоматичне отримання токена для поточного юзера
    initUserSession();
    loadRooms(); // Завантажує кімнати одразу
    setInterval(loadRooms, 5000); // Оновлює кімнати кожні 5 секунд
});
