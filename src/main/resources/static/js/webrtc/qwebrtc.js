/**
 * ============================================================================
 * qwebrtc.js - Фронтенд-клієнт для WebRTC аудіокімнат на базі Janus
 * ============================================================================
 *
 * Цей файл реалізує клієнтську частину системи аудіоконференцій:
 *
 * 1. АРХІТЕКТУРА:
 *    ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
 *    │  Браузер    │ WS   │    Janus    │ AMQP │   Spring    │
 *    │ (qwebrtc.js)│◄────►│  (медіа-    │◄────►│    Boot     │
 *    │             │      │   сервер)   │      │  (бекенд)   │
 *    └─────────────┘      └─────────────┘      └─────────────┘
 *
 * 2. ОСНОВНІ ФУНКЦІЇ:
 *    - Отримання токена авторизації з бекенду
 *    - Встановлення WebSocket з'єднання з Janus
 *    - Прикріплення до плагіна AudioBridge
 *    - Вхід/вихід з аудіокімнат
 *    - Обробка WebRTC signaling (Offer/Answer/ICE)
 *    - Відтворення аудіопотоку від інших учасників
 *    - Перехід між кімнатами без перестворення сесії
 *
 * 3. КЛЮЧОВІ ОСОБЛИВОСТІ:
 *    - Один WebSocket на всю сесію користувача
 *    - Один handle до плагіна (не перестворюється при переході)
 *    - Автоматичне перепідключення при обриві зв'язку
 *    - Підтримка нового API Janus.js (onremotetrack замість onremotestream)
 *
 * ============================================================================
 */

// ============================================================================
// СЕКЦІЯ 1: ГЛОБАЛЬНІ ЗМІННІ СТАНУ
// ============================================================================

/**
 * Глобальні змінні, які зберігають стан сесії користувача.
 * Доступні для всіх функцій у файлі.
 */

/** @type {Janus} Головний об'єкт Janus-сессії (WebSocket з'єднання) */
let janusInstance = null;

/** @type {Object} Handle до плагіна AudioBridge (через нього йде вся комунікація) */
let audioBridge = null;

/** @type {string} UUID-токен користувача, отриманий з бекенду (/token) */
let currentSessionToken = null;

/** @type {number|null} ID кімнати, в якій зараз знаходиться користувач */
let currentRoomId = null;

/** @type {string|null} Відображуване ім'я користувача в кімнаті */
let currentDisplay = null;

/** @type {boolean} Прапорець готовності Janus до роботи (після успішного attach) */
let isJanusReady = false;

/** @type {number|null} ID кімнати, в яку потрібно перейти після підключення */
let pendingRoomId = null;

/** @type {boolean} Запобігає повторним спробам перепідключення одночасно */
let isReconnecting = false;

let isSwitchingRoom = false; // ✅ Запобігання повторним викликам

// ============================================================================
// СЕКЦІЯ 2: КОНФІГУРАЦІЯ
// ============================================================================

/**
 * Конфігураційні константи.
 * Змінюються тут, а не в коді — для зручності підтримки.
 */

/** Базовий URL бекенду (v2 API) */
const BACKEND_API = "/api/v1q/webrtc/stream";

/** WebSocket URL Janus медіасервера */
const JANUS_WS_URL = "wss://test.rfa.toloka.media/janus-ws";

/** Кімната за замовчуванням, в яку заходимо при старті */
const DEFAULT_ROOM_ID = 802;

/** Інтервал автооновлення списку кімнат (мс) */
const ROOMS_REFRESH_INTERVAL = 5000;

// ============================================================================
// СЕКЦІЯ 3: ЛОГУВАННЯ
// ============================================================================

/**
 * Виводить повідомлення в UI-консоль на сторінці та в браузерну консоль.
 *
 * @param {string} message - Текст повідомлення
 * @param {string} type - Тип повідомлення: 'INFO' | 'SUCCESS' | 'ERROR' | 'WARN' | 'DEBUG'
 *
 * Логіка:
 * 1. Якщо елемент #logConsole існує — виводимо туди з кольоровим бейджем
 * 2. Завжди дублюємо в console.log для розробників
 * 3. Автоматично скролимо консоль вниз до останнього запису
 */
function log(message, type = 'INFO') {
    const consoleDiv = document.getElementById("logConsole");

    // Fallback: якщо UI-консолі немає — пишемо тільки в браузерну консоль
    if (!consoleDiv) {
        console.log(`[${type}] ${message}`);
        return;
    }

    // Форматуємо час у форматі HH:MM:SS
    const time = new Date().toLocaleTimeString();

    // Маппінг типів повідомлень на CSS-класи для кольорових бейджів
    const badgeClasses = {
        'INFO': 'badge-info',        // Синій
        'SUCCESS': 'badge-success',  // Зелений
        'ERROR': 'badge-error',      // Червоний
        'WARN': 'badge-error',       // Червоний (попередження)
        'DEBUG': 'badge-debug'       // Сірий
    };
    const badgeClass = badgeClasses[type] || 'badge-info';

    // Додаємо новий рядок у консоль
    consoleDiv.innerHTML += `<div><span class="badge ${badgeClass}">${time}</span> ${message}</div>`;

    // Автоматичний скрол вниз
    consoleDiv.scrollTop = consoleDiv.scrollHeight;

    // Дублюємо в браузерну консоль
    console.log(`[${type}] ${message}`);
}

/**
 * Оновлює індикатор стану з'єднання (зелений/червоний кружечок + текст).
 *
 * @param {boolean} connected - true = підключено, false = відключено
 * @param {string|null} text - Довільний текст статусу (опціонально)
 */
function updateConnectionStatus(connected, text = null) {
    const indicator = document.getElementById("statusIndicator");
    const statusText = document.getElementById("statusText");

    if (indicator) {
        indicator.className = connected
            ? "status-indicator status-connected"     // Зелений
            : "status-indicator status-disconnected"; // Червоний
    }
    if (statusText) {
        statusText.textContent = text || (connected ? "Підключено до Janus" : "Відключено");
    }
}

// ============================================================================
// СЕКЦІЯ 4: ОТРИМАННЯ ТОКЕНА КОРИСТУВАЧА
// ============================================================================

/**
 * Отримує UUID-токен користувача з бекенду та зберігає його глобально.
 *
 * Flow:
 * 1. GET /api/v1q/webrtc/stream/token
 * 2. Бекенд бере UUID поточного залогіненого користувача
 * 3. Реєструє цей UUID в Janus через Admin API (add_token)
 * 4. Повертає {"token": "uuid-..."}
 * 5. Зберігаємо токен в currentSessionToken
 *
 * @returns {Promise<boolean>} true - успіх, false - помилка
 */
async function initUserSession() {
    log("🔑 Отримання токена користувача...", "INFO");

    try {
        // Відправляємо GET запит на бекенд
        const response = await fetch(`${BACKEND_API}/token`);

        // Перевірка HTTP статусу
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        // Парсимо JSON відповідь
        const data = await response.json();

        // Зберігаємо токен глобально
        currentSessionToken = data.token;

        log(`✅ Токен отримано: ${currentSessionToken.substring(0, 8)}...`, "SUCCESS");
        return true;

    } catch (error) {
        log(`❌ Помилка отримання токена: ${error.message}`, "ERROR");
        updateConnectionStatus(false, "Помилка авторизації");

        // Повторна спроба через 5 секунд (автоматичний retry)
        setTimeout(initUserSession, 5000);
        return false;
    }
}

// ============================================================================
// СЕКЦІЯ 5: ІНІЦІАЛІЗАЦІЯ З'ЄДНАННЯ З JANUS
// ============================================================================

/**
 * Встановлює WebSocket з'єднання з Janus медіасервером.
 *
 * Flow:
 * 1. Janus.init() - ініціалізує бібліотеку Janus.js
 * 2. new Janus({server: ...}) - створює WebSocket з'єднання
 * 3. При успіху → attachAudioBridgePlugin()
 * 4. При помилці → retry через 5 секунд (якщо не в процесі reconnect)
 *
 * @returns {Promise<void>} Resolve коли підключення готове
 */
function initJanusConnection() {
    return new Promise((resolve, reject) => {
        // Перевірка наявності токена
        if (!currentSessionToken) return reject("Токен відсутній");

        log("🔌 Підключення до Janus WebSocket...", "INFO");

        // Глобальна ініціалізація Janus.js (має бути викликана один раз)
        Janus.init({
            debug: ["error", "warn"],  // Рівень логування Janus.js
            callback: () => {
                // Створюємо екземпляр Janus-сессії
                janusInstance = new Janus({
                    server: JANUS_WS_URL,
                    token: currentSessionToken,  // Токен на рівні сесії

                    // ✅ Успішне підключення
                    success: () => {
                        log("✅ WebSocket підключено", "SUCCESS");
                        // Прикріплюємося до плагіна AudioBridge
                        attachAudioBridgePlugin().then(resolve).catch(reject);
                    },

                    // ❌ Помилка підключення
                    error: (error) => {
                        log(`❌ Помилка WebSocket: ${error}`, "ERROR");

                        // Автоматичний reconnect (тільки один раз)
                        if (!isReconnecting) {
                            isReconnecting = true;
                            setTimeout(() => {
                                isReconnecting = false;
                                initJanusConnection().then(resolve).catch(reject);
                            }, 5000);
                        } else {
                            reject(error);
                        }
                    },

                    // Сесію знищено (наприклад, сервером за таймаутом)
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

// ============================================================================
// СЕКЦІЯ 6: ПРИКРІПЛЕННЯ ДО ПЛАГІНА AUDIOBRIDGE
// ============================================================================

/**
 * Прикріплює handle до плагіна AudioBridge в Janus.
 *
 * Що таке "handle"?
 * - Handle — це "канал зв'язку" між клієнтом і конкретним плагіном
 * - Через handle відправляються всі команди (join, leave, configure)
 * - Через handle приходять всі події (joined, left, аудіотреки)
 *
 * Важливо: handle створюється ОДИН РАЗ на сесію, а не при кожному переході!
 *
 * @returns {Promise<void>} Resolve коли плагін готовий до роботи
 */
// ============================================================
// 3. ПРИКРІПЛЕННЯ ПЛАГІНА (ВИПРАВЛЕНО)
// ============================================================
function attachAudioBridgePlugin(autoJoinRoomId = null) {
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

                // ✅ ТІЛЬКИ якщо вказано autoJoinRoomId І ми НЕ в процесі переходу
                if (autoJoinRoomId !== null && !isSwitchingRoom) {
                    log(`🚀 Автоматичний вхід в кімнату ${autoJoinRoomId}...`, "INFO");
                    currentRoomId = null; // Скидаємо щоб switchRoom працював
                    switchRoom(autoJoinRoomId);
                } else if (pendingRoomId !== null && !isSwitchingRoom) {
                    const target = pendingRoomId;
                    pendingRoomId = null;
                    currentRoomId = null;
                    log(`🚀 Виконую відкладений перехід в кімнату ${target}...`, "INFO");
                    switchRoom(target);
                } else if (!isSwitchingRoom) {
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


// ============================================================================
// СЕКЦІЯ 7: ПЕРЕХІД МІЖ КІМНАТАМИ
// ============================================================================

/**
 * Перемикає користувача в іншу аудіокімнату.
 *
 * ⚠️ КРИТИЧНО: НЕ перестворюємо WebSocket або handle!
 * Замість цього:
 * 1. POST на бекенд → бекенд робить kick з інших кімнат
 * 2. send({request: "leave"}) → вихід зі старої кімнати
 * 3. send({request: "join", room: newRoomId}) → вхід в нову
 *
 * Це набагато швидше ніж detach/attach і зберігає стабільність.
 *
 * @param {number|string} newRoomId - ID нової кімнати
 */
// ============================================================
// 4. ПЕРЕХІД МІЖ КІМНАТАМИ (ВИПРАВЛЕНО - detach + attach)
// ============================================================
async function switchRoom(newRoomId) {
    newRoomId = parseInt(newRoomId);
    if (isNaN(newRoomId)) return;

    // ✅ Запобігання повторним викликам
    if (isSwitchingRoom) {
        log(`⏳ Вже виконується перехід, ігноруємо`, "WARN");
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

        // POST на бекенд
        const response = await fetch(`${BACKEND_API}/connectclienttoroom`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({roomid: newRoomId})
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const serverResponse = await response.json();
        if (!serverResponse.success) throw new Error(serverResponse.error || "Помилка сервера");
        log(`✅ Бекенд підтвердив перехід. Display: "${serverResponse.display}"`, "SUCCESS");

        if (currentRoomId !== null) {
            log(`👋 Вихід з кімнати ${currentRoomId}...`, "DEBUG");

            // ✅ КРИТИЧНО: Повне від'єднання плагіна для очищення WebRTC
            log("🔌 Від'єднуємо плагін AudioBridge...", "DEBUG");
            audioBridge.detach();
            audioBridge = null;
            isJanusReady = false;

            // Невелика затримка для завершення detach
            await new Promise(r => setTimeout(r, 500));

            // ✅ Повторне прикріплення плагіна БЕЗ автоматичного переходу
            log("🎙️ Прикріплюємо плагін заново...", "DEBUG");
            await attachAudioBridgePlugin(null); // null = не робити автоматичний join
        }

        // Оновлюємо стан
        currentRoomId = newRoomId;
        currentDisplay = serverResponse.display;

        // Вхід в нову кімнату
        joinRoom(newRoomId, currentDisplay);

    } catch (error) {
        log(`❌ Помилка переходу: ${error.message}`, "ERROR");
    } finally {
        isSwitchingRoom = false;
    }
}


/**
 * Відправляє команду join в AudioBridge для входу в кімнату.
 *
 * @param {number} roomId - ID кімнати
 * @param {string} display - Відображуване ім'я користувача
 */
function joinRoom(roomId, display) {
    log(`🚪 Заходимо в кімнату ${roomId} як "${display}"...`, "INFO");

    const joinMessage = {
        request: "join",
        room: roomId,
        display: display,
        muted: false  // Одразу готовий говорити
    };

    log(`📤 Відправка: ${JSON.stringify(joinMessage)}`, "DEBUG");
    audioBridge.send({message: joinMessage});
}

// ============================================================================
// СЕКЦІЯ 8: ОБРОБКА ПОВІДОМЛЕНЬ ВІД JANUS
// ============================================================================

/**
 * Головний обробник всіх повідомлень від плагіна AudioBridge.
 *
 * Викликається автоматично Janus.js коли приходить повідомлення через handle.
 *
 * @param {Object} msg - JSON повідомлення від Janus
 * @param {Object|null} jsep - WebRTC SDP (Offer/Answer) або null
 *
 * Типи подій audiobridge:
 * - "joined"      - успішний вхід в кімнату (наш або чужий)
 * - "left"        - хтось вийшов з кімнати
 * - "kicked"      - користувача видалено
 * - "destroyed"   - кімнату знищено
 * - "event"       - загальна подія (configure, participants тощо)
 */
function handleJanusMessage(msg, jsep) {
    log(`📥 Отримано від Janus: ${JSON.stringify(msg).substring(0, 300)}...`, "DEBUG");

    const event = msg.audiobridge;

    // Обробка помилок від Janus
    if (msg.error_code || msg.error) {
        log(`❌ Помилка від Janus: ${msg.error_code} - ${msg.error}`, "ERROR");
        return;
    }

    // ========================================================================
    // ПОДІЯ: "joined" - хтось увійшов в кімнату
    // ========================================================================
    if (event === "joined") {
        // ⚠️ КРИТИЧНО: Розрізняємо наш вхід від входу інших!
        // - Наш вхід: є поле msg.id (наш ID в Janus)
        // - Чужий вхід: немає msg.id, але є msg.participants

        if (msg.id) {
            // ✅ ЦЕ НАШ ВХІД в кімнату
            const myId = msg.id;
            const myDisplay = msg.display || currentDisplay;
            log(`✅ Увійшли в кімнату ${currentRoomId}. Ваш ID: ${myId}, Display: "${myDisplay}"`, "SUCCESS");

            // Оновлюємо список кімнат в UI
            loadRooms();

            // Якщо сервер НЕ надіслав jsep — ми ПЕРШИМИ створюємо Offer
            // Це стандартний flow для AudioBridge: клієнт ініціює WebRTC
            if (!jsep) {
                log("🎤 Створюємо WebRTC Offer (мікрофон)...", "DEBUG");
                const offerStartTime = Date.now();

                audioBridge.createOffer({
                    // Налаштування медіа: тільки аудіо в обидві сторони
                    media: {audioSend: true, audioRecv: true, video: false},

                    // ✅ Успішно створили Offer
                    success: (localJsep) => {
                        const elapsed = Date.now() - offerStartTime;
                        log(`📤 SDP Offer готовий (за ${elapsed}мс). Надсилаємо configure...`, "INFO");

                        // Відправляємо configure з нашим Offer
                        // Це запускає WebRTC handshake
                        audioBridge.send({
                            message: {request: "configure", muted: false},
                            jsep: localJsep  // Наш SDP Offer
                        });
                    },

                    // ❌ Помилка створення Offer (наприклад, мікрофон недоступний)
                    error: (err) => log(`❌ Offer Error: ${JSON.stringify(err)}`, "ERROR")
                });
            }
        } else {
            // 👤 ЦЕ ВХІД ІНШОГО КОРИСТУВАЧА
            log(`👤 Інший користувач увійшов в кімнату`, "INFO");
            loadRooms();
        }
    }

    // ========================================================================
    // ПОДІЯ: "left" - хтось вийшов з кімнати
    // ========================================================================
    if (event === "left") {
        log(`👋 Хтось вийшов з кімнати`, "INFO");
        loadRooms();
    }

    // ========================================================================
    // ПОДІЯ: "kicked" - нас видалено з кімнати
    // ========================================================================
    if (event === "kicked") {
        log(`⚠️ Вас видалено з кімнати`, "WARN");
    }

    // ========================================================================
    // ПОДІЯ: "destroyed" - кімнату знищено
    // ========================================================================
    if (event === "destroyed") {
        currentRoomId = null;
        log(`💥 Кімнату знищено`, "WARN");
    }

    // ========================================================================
    // ОБРОБКА WEBRTC SDP (Signaling)
    // ========================================================================
    if (jsep) {
        log(`📡 Отримано JSEP типу: ${jsep.type}`, "DEBUG");

        // --------------------------------------------------------------------
        // ANSWER - відповідь сервера на наш Offer
        // --------------------------------------------------------------------
        if (jsep.type === "answer") {
            audioBridge.handleRemoteJsep({
                jsep: jsep,  // SDP Answer від Janus

                success: () => {
                    log("✅ WebRTC Handshake завершено", "SUCCESS");

                    // Запускаємо моніторинг ICE state кожні 2 секунди
                    let checkCount = 0;
                    const checkInterval = setInterval(() => {
                        // Безпечна перевірка (PeerConnection може бути відсутній)
                        if (audioBridge.webrtcStuff?.pc) {
                            const pc = audioBridge.webrtcStuff.pc;
                            log(`🔍 ICE: ${pc.iceConnectionState} (${++checkCount})`, "DEBUG");

                            // ✅ З'єднання встановлено
                            if (pc.iceConnectionState === "connected" || pc.iceConnectionState === "completed") {
                                log("✅ ICE з'єднання встановлено!", "SUCCESS");
                                clearInterval(checkInterval);
                            }
                            // ❌ З'єднання не вдалося
                            else if (pc.iceConnectionState === "failed") {
                                log("❌ ICE FAILED! Потрібен TURN сервер", "ERROR");
                                clearInterval(checkInterval);
                            }
                        }

                        // Таймаут через 30 секунд
                        if (checkCount >= 15) {
                            log("⚠️ ICE не встановився за 30 секунд", "WARN");
                            clearInterval(checkInterval);
                        }
                    }, 2000);
                },

                error: (err) => log(`❌ Answer Error: ${JSON.stringify(err)}`, "ERROR")
            });
        }

            // --------------------------------------------------------------------
            // OFFER - сервер ініціює WebRTC (рідкісний випадок)
        // --------------------------------------------------------------------
        else if (jsep.type === "offer") {
            audioBridge.createAnswer({
                jsep: jsep,  // SDP Offer від Janus
                media: {audioSend: true, audioRecv: true, video: false},

                success: (jsepAnswer) => {
                    // Відправляємо наш Answer назад
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

// ============================================================================
// СЕКЦІЯ 9: ЗАВАНТАЖЕННЯ ТА ВІДОБРАЖЕННЯ КІМНАТ
// ============================================================================

/**
 * Завантажує список активних кімнат з бекенду та оновлює UI.
 * Викликається автоматично кожні 5 секунд (ROOMS_REFRESH_INTERVAL).
 */
async function loadRooms() {
    if (!isJanusReady) return;  // Не завантажуємо поки Janus не готовий

    try {
        const response = await fetch(`${BACKEND_API}/rooms`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        // Парсимо JSON та рендеримо
        renderRooms(await response.json());
    } catch (error) {
        log(`❌ Помилка завантаження кімнат: ${error.message}`, "ERROR");
    }
}

/**
 * Рендерить список кімнат в HTML.
 *
 * @param {Array} rooms - Масив об'єктів StationDto з бекенду
 *
 * Структура кожного room:
 * {
 *   id: 802,                    // ID кімнати
 *   name: "Радіо Україна",      // Назва
 *   stationstate: true,         // Чи йде трансляція (LIVE badge)
 *   participants: [             // Список учасників
 *     {id: 123, display: "Admin", talking: true, muted: false},
 *     ...
 *   ]
 * }
 */
function renderRooms(rooms) {
    const container = document.getElementById("roomsList");
    if (!container) return;

    // Порожній список
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
        // Перевіряємо чи це поточна кімната користувача
        const isCurrent = currentRoomId === room.id;

        // Стиль кнопки залежить від того, чи ми вже в цій кімнаті
        const btnClass = isCurrent ? 'btn-action-room' : 'btn-action-room btn-join-room';
        const btnText = isCurrent ? '✓ Ви тут' : '🚪 Увійти';
        const btnDisabled = isCurrent ? 'disabled' : '';
        const btnAction = isCurrent ? '' : `onclick="switchRoom(${room.id})"`;

        // Рендеримо список учасників
        let participantsHtml = '<div class="no-users">Немає користувачів</div>';
        if (room.participants && room.participants.length > 0) {
            participantsHtml = '<ul class="user-list">' + room.participants.map(p => {
                // Визначаємо стан користувача
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

        // Фінальний HTML для картки кімнати
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

/**
 * Розблоковує кнопки кімнат (викликається після успішного attach).
 */
function enableRoomButtons() {
    document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = false);
}

/**
 * Блокує кнопки кімнат (викликається при старті).
 */
function disableRoomButtons() {
    document.querySelectorAll('.btn-action-room').forEach(btn => btn.disabled = true);
}

// ============================================================================
// СЕКЦІЯ 10: ІНІЦІАЛІЗАЦІЯ ПРИ ЗАВАНТАЖЕННІ СТОРІНКИ
// ============================================================================

/**
 * Головний entry point. Викликається коли DOM готовий.
 *
 * Flow:
 * 1. Блокуємо кнопки (щоб не клікали до підключення)
 * 2. Отримуємо токен користувача з бекенду
 * 3. Встановлюємо WebSocket з Janus
 * 4. Прикріплюємося до плагіна AudioBridge
 * 5. Автоматично заходимо в DEFAULT_ROOM_ID
 * 6. Запускаємо автооновлення списку кімнат
 */
document.addEventListener("DOMContentLoaded", async () => {
    log("🚀 Сторінку завантажено. Ініціалізація...", "INFO");

    // Блокуємо кнопки до готовності
    disableRoomButtons();

    // Крок 1: Отримуємо токен
    const tokenReady = await initUserSession();
    if (!tokenReady) return;  // Якщо не вдалося — виходимо (retry вже запущено)

    try {
        // Крок 2-4: Підключаємося до Janus та прикріплюємо плагін
        await initJanusConnection();
        log("🟢 Система готова до роботи!", "SUCCESS");
    } catch (error) {
        log(`❌ Критична помилка ініціалізації: ${error}`, "ERROR");
    }

    // Крок 5: Перше завантаження кімнат
    loadRooms();

    // Крок 6: Автооновлення кожні 5 секунд
    setInterval(loadRooms, ROOMS_REFRESH_INTERVAL);
});

/**
 * Обробка закриття сторінки — коректне завершення сесії.
 * Без цього Janus може тримати сесію ще деякий час за таймаутом.
 */
window.addEventListener("beforeunload", () => {
    if (janusInstance) {
        janusInstance.destroy({cleanupHandles: true});
    }
});

// ============================================================================
// СЕКЦІЯ 11: ЕКСПОРТ ФУНКЦІЙ ДЛЯ HTML (onclick handlers)
// ============================================================================

/**
 * Робимо функції доступними глобально, щоб їх можна було викликати з HTML:
 * <button onclick="switchRoom(802)">Увійти</button>
 */
window.switchRoom = switchRoom;
window.loadRooms = loadRooms;