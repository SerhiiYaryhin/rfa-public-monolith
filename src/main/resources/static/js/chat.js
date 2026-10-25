'use strict';

/**
 * ============================================================================
 * 📡 chat.js — Логіка текстового чату RFA + інтеграція з WebRTC/GreenRoom
 * ============================================================================
 *
 * Цей модуль відповідає за:
 *   1. STOMP-підключення до RabbitMQ через SockJS (/rfachat)
 *   2. Текстовий чат: публічні/приватні повідомлення, рендеринг бульбашок
 *   3. Список користувачів та кімнат (оновлення через STOMP)
 *   4. Механізм "підняття руки" (hand raise) з зворотним зв'язком від модератора
 *   5. Команди керування ефіром (MOVE_YOURSELF, MUTE, UNMUTE) від модератора
 *   6. Інтеграцію з GreenRoomUI (модальне вікно Janus AudioBridge)
 *   7. Інтеграцію з MainRoomAudio (автопідключення до головної кімнати станції)
 *
 * ⚠️ ЗАЛЕЖНОСТІ (мають бути завантажені ДО цього файлу):
 *   - sockjs.min.js, stomp.js         → STOMP-клієнт
 *   - janus.js                         → Бібліотека Janus Gateway (CDN)
 *   - /js/webrtc/v2/*.js               → WebRTC модулі (config, logger, janus-connection тощо)
 *   - /js/webrtc/messenger.js          → MainRoomAudio + GreenRoomUI
 *
 * ГЛОБАЛЬНІ ЗМІННІ THYMELEAF (передаються з сервера через inline-скрипт):
 *   - curuuid       : UUID поточного користувача
 *   - curusername   : Відображуване ім'я поточного користувача
 *   - curroom       : UUID поточної текстової кімнати
 *   - stationUuid   : UUID станції (для підписок hand.status)
 *   - curRoomType   : Тип кімнати (EChatRoomType.name)
 *   - curRoomJanusId: Числовий ID кімнати Janus (= MessageRoom.id)
 */

// ============================================================================
// 📌 ГЛОБАЛЬНІ ЗМІННІ СТАНУ
// ============================================================================

/** @type {Stomp.Client|null} STOMP-клієнт для WebSocket-з'єднання з RabbitMQ */
var stompClient = null;

/** @type {string|null} UUID співрозмовника в приватному чаті (null = публічна кімната) */
var clientuuid = null;

/** @type {string|null} Ім'я співрозмовника в приватному чаті */
var toclientname = null;

// --- Підписки STOMP (зберігаємо для можливості unsubscribe при зміні кімнати) ---
/** @type {Stomp.Subscription|null} Підписка на поточну текстову кімнату */
var subcurroom = null;
/** @type {Stomp.Subscription|null} Підписка на публічні повідомлення користувача */
var subpublic = null;
/** @type {Stomp.Subscription|null} Підписка на приватні повідомлення користувача */
var subprivate = null;
/** @type {Stomp.Subscription|null} Підписка на список користувачів */
var subusers = null;
/** @type {Stomp.Subscription|null} Підписка на список кімнат */
var subroom = null;
/** @type {Stomp.Subscription|null} Підписка на події присутності (online/offline) */
var subPresence = null;
/** @type {Stomp.Subscription|null} Підписка на медіа-повідомлення (зображення, аудіо) */
var subMediaToPublic = null;
/** @type {Stomp.Subscription|null} Підписка на команди керування ефіром від модератора */
var subEfirControl = null;
/** @type {Stomp.Subscription|null} Підписка на зворотний зв'язок про статус руки */
var subHandStatus = null;

// --- Автоматичне перепідключення STOMP ---
/** @type {number|null} Таймер затримки перед повторною спробою підключення */
var reconnectTimeout = null;
/** @type {number} Поточна затримка перепідключення (експоненціальне зростання, макс 30с) */
var reconnectDelay = 1000;

// --- Стан "піднятої руки" ---
/** @type {boolean} Чи піднята зараз рука у поточного користувача */
var handRaised = false;
/** @type {number|null} Таймер автоспуску руки (5 хвилин) */
var handRaiseTimeout = null;

// --- Стан мікрофона (текстовий чат, не Janus) ---
/** @type {boolean} Чи вимкнено мікрофон користувача (для UI-кнопки в чаті) */
var myMuted = false;

/** @type {HTMLElement} Посилання на контейнер повідомлень для автопрокрутки */
const messagesArea = document.getElementById('mainMessagesArea');


// ============================================================================
// 🔌 STOMP-ПІДКЛЮЧЕННЯ
// ============================================================================

/**
 * Встановлює WebSocket-з'єднання з RabbitMQ через SockJS + STOMP.
 * Якщо з'єднання вже активне — пропускає виклик.
 * Налаштовує heartbeat (8с вхідний, 8с вихідний) для виявлення обривів.
 */
function connect() {
    if (stompClient && stompClient.connected) return;

    console.log("Attempting to connect to STOMP...");
    var socket = new SockJS('/rfachat');
    stompClient = Stomp.over(socket);

    // STOMP debug вимкнено за замовчуванням для чистої консолі.
    // Для відлагодження додайте ?debug=stomp в URL сторінки.
    if (new URLSearchParams(window.location.search).has('debug')) {
        stompClient.debug = function(str) { console.log("STOMP: " + str); };
    } else {
        stompClient.debug = null;
    }

    stompClient.connect({
        heartbeatIncoming: 8000,
        heartbeatOutgoing: 8000
    }, onConnected, onError);
}

/**
 * Callback успішного підключення до STOMP-брокера.
 * Створює всі необхідні підписки та надсилає початкові запити.
 */
function onConnected() {
    console.log("Connected to STOMP cluster");

    // Скидаємо експоненціальну затримку при успішному підключенні
    reconnectDelay = 1000;
    if (reconnectTimeout) clearTimeout(reconnectTimeout);

    // Оновлюємо індикатор статусу в UI
    const statusEl = document.getElementById('connectstatus');
    if (statusEl) {
        statusEl.innerHTML = '<i class="fas fa-circle me-1" style="font-size: 0.5rem;"></i> Онлайн';
    }

    // --- Створюємо підписки ---
    // Публічні повідомлення поточної кімнати
    subcurroom = stompClient.subscribe('/topic/' + curroom, onPublicMessageReceived);
    // Публічні повідомлення, адресовані особисто цьому користувачеві
    subpublic = stompClient.subscribe('/topic/chat.public.' + curuuid, onPublicMessageReceived);
    // Приватні повідомлення
    subprivate = stompClient.subscribe('/topic/chat.private.' + curuuid, onPrivateMessageReceived);
    // Список онлайн-користувачів
    subusers = stompClient.subscribe('/topic/chat.userslist.' + curuuid, onUserList);
    // Список доступних кімнат
    subroom = stompClient.subscribe('/topic/chat.roomslist.' + curuuid, onRoomList);
    // Події присутності (вхід/вихід користувачів)
    subPresence = stompClient.subscribe('/topic/presence-update', onPresenceUpdate);
    // Медіа-повідомлення (зображення, аудіотреки)
    subMediaToPublic = stompClient.subscribe('/topic/chat.media', onMediaPublic);

    // ✅ Команди керування ефіром від модератора (переміщення, mute)
    subEfirControl = stompClient.subscribe('/topic/efir.control', onEfirControl);

    // ✅ Сповіщення про зміну статусу руки (прийнято/відхилено модератором)
    // Використовуємо fallback UUID, якщо stationUuid не переданий з сервера
    const handStatusStationUuid = (typeof stationUuid !== 'undefined' && stationUuid)
        ? stationUuid : "e5044caa-6eba-4af7-9f95-788b1dd238a9";
    subHandStatus = stompClient.subscribe(
        `/topic/efir.${handStatusStationUuid}.hand.status`,
        onHandStatusUpdate
    );
    console.log(`✅ Підписано на /topic/efir.${handStatusStationUuid}.hand.status`);

    // Надсилаємо привітальне повідомлення для реєстрації сесії на бекенді
    stompClient.send("/app/hello", {}, JSON.stringify({
        uuid: curuuid, roomuuid: curroom, send: Date.now()
    }));

    // Запитуємо початкові списки
    getuserlist();
    getroomlist();
}

/**
 * Callback помилки/обриву STOMP-з'єднання.
 * Реалізує експоненціальне перепідключення (1с → 2с → 4с → ... → 30с макс).
 */
function onError(error) {
    console.error("STOMP Error/Disconnect:", error);

    const statusEl = document.getElementById('connectstatus');
    if (statusEl) {
        statusEl.innerHTML = '<span class="text-danger"><i class="fas fa-sync-alt fa-spin me-1"></i> Відключено. Перепідключення...</span>';
    }

    if (reconnectTimeout) clearTimeout(reconnectTimeout);
    reconnectTimeout = setTimeout(() => {
        reconnectDelay = Math.min(reconnectDelay * 2, 30000);
        connect();
    }, reconnectDelay);
}

/**
 * При поверненні на вкладку браузера перевіряємо з'єднання
 * і перепідключаємося, якщо воно було втрачено.
 */
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        if (!stompClient || !stompClient.connected) connect();
    }
});

/**
 * Обробник подій присутності (вхід/вихід інших користувачів).
 * Оновлює списки користувачів та кімнат.
 */
function onPresenceUpdate(msg) {
    getuserlist();
    getroomlist();
}


// ============================================================================
// 🎛️ КОМАНДИ КЕРУВАННЯ ЕФІРОМ ВІД МОДЕРАТОРА
// ============================================================================

/**
 * Обробляє STOMP-команди від модератора через /topic/efir.control.
 *
 * Підтримувані команди:
 *   - MOVE_YOURSELF    : Переміщення гостя в іншу кімнату + відкриття GreenRoomUI
 *   - MUTE_YOURSELF    : Модератор вимкнув мікрофон гостя
 *   - UNMUTE_YOURSELF  : Модератор увімкнув мікрофон гостя
 *   - MUTE_ALL_IN_ROOM : Модератор вимкнув мікрофони всім у кімнаті
 *   - UNMUTE_ALL_IN_ROOM: Модератор увімкнув мікрофони всім у кімнаті
 *
 * @param {Object} payload - STOMP-повідомлення з JSON-тілом команди
 */
function onEfirControl(payload) {
    try {
        const cmd = typeof payload.body === 'string' ? JSON.parse(payload.body) : payload.body;
        console.log("🎛️ Отримано команду ефіру:", cmd.action);

        switch (cmd.action) {

            // ================================================================
            // ПЕРЕМІЩЕННЯ ГОСТЯ В GREEN ROOM / BROADCAST
            // ================================================================
            case 'MOVE_YOURSELF':
                if (cmd.targetUserUuid === curuuid && cmd.targetRoomUuid && cmd.targetRoomUuid !== curroom) {

                    // Крок 1: Опускаємо руку, якщо вона була піднята
                    if (handRaised) lowerHand();

                    // Крок 2: ✅ СПОЧАТКУ відкриваємо модальне вікно GreenRoomUI
                    //         GreenRoomUI.enter() зберігає previousChatRoom = curroom
                    //         ПОКИ curroom ще містить UUID головної кімнати!
                    const janusRoomId = cmd.targetJanusRoomId;
                    if (janusRoomId && typeof GreenRoomUI !== 'undefined') {
                        const roomLabel = cmd.autoMute ? '🔴 Прямий ефір' : '🟡 Green Room';
                        GreenRoomUI.enter(janusRoomId, roomLabel, !!cmd.autoMute);
                    }

                    // Крок 3: ✅ ПОТІМ перемикаємо текстовий чат на нову кімнату
                    //         curroom зміниться тут, але previousChatRoom вже збережений
                    selectroom(cmd.targetRoomUuid, cmd.targetRoomName || 'Кімната ефіру');

                    // Fallback: якщо Janus недоступний або targetJanusRoomId не передано
                    if (!janusRoomId || typeof GreenRoomUI === 'undefined') {
                        if (cmd.autoMute) {
                            myMuted = true;
                            updateMyMicrophoneUI();
                            showEfirNotification("🎙️ Вас переміщено в Ефір. Мікрофон вимкнено.", 'info');
                        } else {
                            showEfirNotification("🎙️ Вас переміщено в Green Room", 'success');
                        }
                    }
                }
                break;

            // ================================================================
            // MUTE / UNMUTE ОКРЕМОГО ГОСТЯ
            // ================================================================
            case 'MUTE_YOURSELF':
                if (cmd.targetUserUuid === curuuid) {
                    myMuted = true;
                    updateMyMicrophoneUI();
                    showEfirNotification("🔇 Модератор вимкнув ваш мікрофон", 'warning');
                }
                break;

            case 'UNMUTE_YOURSELF':
                if (cmd.targetUserUuid === curuuid) {
                    myMuted = false;
                    updateMyMicrophoneUI();
                    showEfirNotification("🔊 Вам надано слово!", 'success');
                }
                break;

            // ================================================================
            // MUTE / UNMUTE ВСІХ У КІМНАТІ
            // ================================================================
            case 'MUTE_ALL_IN_ROOM':
                if (curroom === cmd.roomUuid) {
                    myMuted = true;
                    updateMyMicrophoneUI();
                    showEfirNotification("🔇 Модератор вимкнув мікрофони всім", 'warning');
                }
                break;

            case 'UNMUTE_ALL_IN_ROOM':
                if (curroom === cmd.roomUuid) {
                    myMuted = false;
                    updateMyMicrophoneUI();
                    showEfirNotification("🔊 Мікрофони увімкнені", 'success');
                }
                break;
        }
    } catch (e) {
        console.error("Error handling efir control command:", e);
    }
}


// ============================================================================
// 🙋 ЗВОРОТНИЙ ЗВ'ЯЗОК ПРО СТАТУС РУКИ
// ============================================================================

/**
 * Обробляє сповіщення від сервера про зміну статусу піднятої руки.
 * Сервер надсилає це повідомлення, коли модератор приймає (ACCEPTED)
 * або відхиляє (DECLINED) руку гостя.
 *
 * @param {Object} payload - STOMP-повідомлення з JSON: {targetUserUuid, status, message}
 */
function onHandStatusUpdate(payload) {
    try {
        const data = typeof payload.body === 'string' ? JSON.parse(payload.body) : payload.body;

        // Обробляємо тільки якщо це стосується поточного користувача
        if (data.targetUserUuid !== curuuid) return;

        console.log("🙋 Статус руки змінено:", data.status, data.message);

        // Скидаємо стан руки локально
        handRaised = false;
        if (handRaiseTimeout) {
            clearTimeout(handRaiseTimeout);
            handRaiseTimeout = null;
        }
        updateHandButton();

        // Показуємо сповіщення гостю з відповідним кольором
        let notifType = 'info';
        if (data.status === 'ACCEPTED') notifType = 'success';
        if (data.status === 'DECLINED') notifType = 'warning';

        showEfirNotification(data.message || 'Статус руки змінено', notifType);
    } catch (e) {
        console.error("Error handling hand status update:", e);
    }
}


// ============================================================================
// 💬 РЕНДЕРИНГ ПОВІДОМЛЕНЬ
// ============================================================================

/**
 * Створює HTML-елемент "бульбашки" повідомлення та додає його в чат.
 * Запобігає дублюванню через перевірку msgId.
 * Автоматично прокручує чат вниз після додавання.
 *
 * @param {Object} msg      - Об'єкт повідомлення з бекенду
 * @param {boolean} isPrivate - true для приватних повідомлень, false для публічних
 */
function renderBubble(msg, isPrivate = false) {
    // Унікальний ID для запобігання дублюванню
    const msgId = `msg-${msg.send}-${msg.fromuuid}`;
    if (document.getElementById(msgId)) return;

    const isMe = msg.fromuuid === curuuid;

    // Створюємо DOM-елементи бульбашки
    const bubble = document.createElement('div');
    bubble.id = msgId;
    bubble.className = `bubble ${isMe ? 'bubble-me' : 'bubble-other'} animate__animated animate__fadeInUp animate__faster`;

    const content = document.createElement('div');
    content.innerHTML = msg.body;

    const meta = document.createElement('div');
    meta.className = 'bubble-meta';
    const time = date2string(msg.send);
    meta.innerHTML = `<span>${isMe ? '' : msg.fromname + ' • '}</span><span>${time}</span>`;

    bubble.appendChild(content);
    bubble.appendChild(meta);

    // Додаємо у відповідний контейнер
    const container = isPrivate
        ? document.getElementById('privatearea')
        : document.getElementById('publicarea');

    if (container) {
        container.appendChild(bubble);
        // Плавна прокрутка до останнього повідомлення
        messagesArea.scrollTo({ top: messagesArea.scrollHeight, behavior: 'smooth' });
    }
}

/**
 * Обробник публічних повідомлень від STOMP.
 */
function onPublicMessageReceived(payload) {
    try {
        renderBubble(JSON.parse(payload.body), false);
    } catch (e) {
        console.error("Error parsing public message", e);
    }
}

/**
 * Обробник приватних повідомлень від STOMP.
 * Показує toastr-сповіщення, якщо повідомлення від іншого користувача
 * і приватний чат зараз не відкритий.
 */
function onPrivateMessageReceived(payload) {
    try {
        var msg = JSON.parse(payload.body);

        if (clientuuid && (msg.fromuuid === clientuuid || msg.touuid === clientuuid)) {
            // Повідомлення для поточного приватного чату — рендеримо
            renderBubble(msg, true);
        } else if (msg.fromuuid !== curuuid) {
            // Повідомлення від іншого користувача — показуємо сповіщення
            if (window.toastr) toastr.info('Нове повідомлення від ' + msg.fromname);
            getuserlist(); // Оновлюємо лічильник непрочитаних
        }
    } catch (e) {
        console.error("Error parsing private message", e);
    }
}


// ============================================================================
// 👥 СПИСКИ КОРИСТУВАЧІВ ТА КІМНАТ
// ============================================================================

/**
 * Оновлює бічну панель зі списком користувачів.
 * Дані приходять як JSON-масив рядків (кожен рядок — серіалізований ChatListElement).
 */
function onUserList(payload) {
    try {
        let jbody = JSON.parse(payload.body);
        const container = document.getElementById('userlist');
        if (!container) return;

        container.innerHTML = '';
        let users = JSON.parse(jbody.body);

        users.forEach(function(uJson) {
            let u = JSON.parse(uJson);
            const item = document.createElement('div');
            item.className = `chat-item list-group-item ${clientuuid === u.uuid ? 'active' : ''}`;

            // Генеруємо ініціали з імені
            const initials = u.name
                ? u.name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2)
                : '??';

            // Бейдж непрочитаних повідомлень
            const badge = u.unreadCount > 0
                ? `<span class="badge rounded-pill bg-danger" style="font-size: 0.7rem;">${u.unreadCount}</span>`
                : '';

            item.innerHTML = `
                <div class="chat-avatar">${initials}</div>
                <div class="chat-info flex-grow-1">
                    <div class="chat-name">${u.name || 'Користувач'}</div>
                    <div class="chat-status">Написати особисто</div>
                </div>
                <div class="chat-meta">${badge}</div>`;

            item.onclick = () => selectuserFromPublic(u.uuid, u.name);
            container.appendChild(item);
        });
    } catch (e) {
        console.error("Error updating user list", e);
    }
}

/**
 * Оновлює бічну панель зі списком кімнат.
 */
function onRoomList(payload) {
    try {
        let jbody = JSON.parse(payload.body);
        const container = document.getElementById('roomlist');
        if (!container) return;

        container.innerHTML = '';
        let rooms = JSON.parse(jbody.body);

        rooms.forEach(function(rJson) {
            let r = JSON.parse(rJson);
            const item = document.createElement('div');
            const isActive = curroom === r.uuid && !clientuuid;

            item.className = `chat-item list-group-item ${isActive ? 'active' : ''}`;
            item.innerHTML = `
                <div class="chat-avatar"><i class="fas fa-hashtag"></i></div>
                <div class="chat-info">
                    <div class="chat-name">${r.name}</div>
                    <div class="chat-status">Публічна кімната</div>
                </div>`;

            item.onclick = () => selectroom(r.uuid, r.name);
            container.appendChild(item);
        });
    } catch (e) {
        console.error("Error updating room list", e);
    }
}


// ============================================================================
// 🔄 ПЕРЕМИКАННЯ КІМНАТ ТА КОРИСТУВАЧІВ
// ============================================================================

/**
 * Перемикає поточну текстову кімнату чату.
 * - Відписується від старої кімнати
 * - Підписується на нову
 * - Запитує історію повідомлень нової кімнати
 * - Автоматично опускає руку при зміні кімнати
 * - На мобільних закриває сайдбар
 *
 * @param {string} uuid - UUID нової кімнати
 * @param {string} name - Відображуване ім'я нової кімнати
 */
function selectroom(uuid, name) {
    // Автоматично опускаємо руку при зміні кімнати
    if (handRaised) lowerHand();

    // Якщо вже в цій кімнаті і не в приватному чаті — нічого не робимо
    if (curroom === uuid && !clientuuid) return;

    // Відписуємося від старої кімнати
    if (subcurroom) subcurroom.unsubscribe();

    // Оновлюємо глобальну змінну поточної кімнати
    curroom = uuid;

    // Очищаємо області повідомлень
    document.getElementById('publicarea').innerHTML = '';
    document.getElementById('privatearea').innerHTML = '';
    document.getElementById('chatTitle').innerText = name;

    // Скидаємо стан приватного чату
    clientuuid = null;
    toclientname = null;

    // Підписуємося на нову кімнату та запитуємо історію
    if (stompClient && stompClient.connected) {
        subcurroom = stompClient.subscribe('/topic/' + curroom, onPublicMessageReceived);
        stompClient.send("/app/room/history", {}, JSON.stringify({
            uuid: curuuid, roomuuid: curroom
        }));
    }

    // Оновлюємо списки
    getroomlist();
    getuserlist();

    // На мобільних закриваємо сайдбар після вибору кімнати
    if (window.innerWidth <= 768) toggleSidebar();
}

/**
 * Перемикає на приватний чат з конкретним користувачем.
 *
 * @param {string} uuid - UUID співрозмовника
 * @param {string} name - Ім'я співрозмовника
 */
function selectuserFromPublic(uuid, name) {
    if (clientuuid === uuid) return;

    clientuuid = uuid;
    toclientname = name;
    document.getElementById('chatTitle').innerText = `Чат з ${name}`;

    // Очищаємо області та запитуємо історію приватного чату
    document.getElementById('publicarea').innerHTML = '';
    document.getElementById('privatearea').innerHTML = '';

    if (stompClient && stompClient.connected) {
        stompClient.send("/app/private/history", {}, JSON.stringify({
            uuid: curuuid, touuid: clientuuid
        }));
    }

    getuserlist();
    getroomlist();
    if (window.innerWidth <= 768) toggleSidebar();
}


// ============================================================================
// 📤 НАДСИЛАННЯ ПОВІДОМЛЕНЬ
// ============================================================================
/**
 * Надсилає повідомлення в правильний канал (публічний або приватний).
 * Викликається з onclick="handleSend()" у messenger.html.
 */
function handleSend() {
    if (clientuuid && typeof sendprivate === 'function') {
        sendprivate();
    } else if (typeof sendpublic === 'function') {
        sendpublic();
    }
}

/**
 * Надсилає публічне повідомлення в поточну кімнату.
 */
function sendpublic() {
    const input = document.getElementById('newmessage');
    if (!input || !input.value.trim() || !stompClient || !stompClient.connected) return;

    stompClient.send("/app/public", {}, JSON.stringify({
        uuid: curuuid,
        send: Date.now(),
        fromname: curusername,
        fromuuid: curuuid,
        body: input.value,
        roomuuid: curroom
    }));
    input.value = "";
}

/**
 * Надсилає приватне повідомлення обраному співрозмовнику.
 */
function sendprivate() {
    const input = document.getElementById('newmessage');
    if (!input || !input.value.trim() || !clientuuid || !stompClient || !stompClient.connected) return;

    stompClient.send("/app/private", {}, JSON.stringify({
        uuid: curuuid,
        send: Date.now(),
        fromname: curusername,
        fromuuid: curuuid,
        toname: toclientname,
        touuid: clientuuid,
        body: input.value
    }));
    input.value = "";
}

/**
 * Завантажує зображення на сервер та надсилає його як повідомлення в чат.
 * Підтримує як публічні, так і приватні повідомлення.
 *
 * @param {File} file - Файл зображення з input[type=file]
 */
async function uploadImage(file) {
    if (!file || !file.type.startsWith('image/')) {
        alert('Можна завантажувати лише зображення');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    const input = document.getElementById('newmessage');
    if (input) input.placeholder = 'Завантаження...';

    try {
        const response = await fetch('/api/chat/upload-image', { method: 'POST', body: formData });
        const result = await response.json();

        if (result.success && stompClient && stompClient.connected) {
            const imgHtml = `<img src="${result.url}" class="img-fluid rounded-3 mt-2 mb-2" 
                style="max-height: 300px; cursor: pointer;" 
                onclick="window.open('${result.url}', '_blank')">`;

            if (clientuuid) {
                // Приватне повідомлення з зображенням
                stompClient.send("/app/private", {}, JSON.stringify({
                    uuid: curuuid, send: Date.now(),
                    fromname: curusername, fromuuid: curuuid,
                    toname: toclientname, touuid: clientuuid,
                    body: imgHtml
                }));
            } else {
                // Публічне повідомлення з зображенням
                stompClient.send("/app/public", {}, JSON.stringify({
                    uuid: curuuid, send: Date.now(),
                    fromname: curusername, fromuuid: curuuid,
                    body: imgHtml, roomuuid: curroom
                }));
            }
        }
    } catch (e) {
        console.error('Upload error:', e);
    } finally {
        if (input) input.placeholder = 'Напишіть повідомлення...';
    }
}


// ============================================================================
// 🙋 ПІДНЯТТЯ РУКИ
// ============================================================================

/**
 * Перемикає стан руки: підняти ↔ опустити.
 */
function toggleHand() {
    if (handRaised) lowerHand();
    else raiseHand();
}

/**
 * Піднімає руку гостя. Надсилає STOMP-запит на сервер.
 * Встановлює таймер автоспуску (5 хвилин).
 */
function raiseHand() {
    if (!stompClient || !stompClient.connected) {
        alert('Немає з\'єднання з сервером');
        return;
    }

    const stationUuidToUse = (typeof stationUuid !== 'undefined' && stationUuid)
        ? stationUuid : "e5044caa-6eba-4af7-9f95-788b1dd238a9";

    stompClient.send("/app/hand/raise", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({ stationUuid: stationUuidToUse, roomuuid: curroom })
    }));

    handRaised = true;
    updateHandButton();

    // Автоспуск руки через 5 хвилин
    if (handRaiseTimeout) clearTimeout(handRaiseTimeout);
    handRaiseTimeout = setTimeout(() => {
        if (handRaised) lowerHand();
    }, 5 * 60 * 1000);
}

/**
 * Опускає руку гостя. Надсилає STOMP-запит з причиною BY_USER.
 */
function lowerHand() {
    if (!stompClient || !stompClient.connected) return;

    const stationUuidToUse = (typeof stationUuid !== 'undefined' && stationUuid)
        ? stationUuid : "e5044caa-6eba-4af7-9f95-788b1dd238a9";

    stompClient.send("/app/hand/lower", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({
            stationUuid: stationUuidToUse,
            targetUuid: curuuid,
            reason: 'BY_USER'
        })
    }));

    handRaised = false;
    if (handRaiseTimeout) {
        clearTimeout(handRaiseTimeout);
        handRaiseTimeout = null;
    }
    updateHandButton();
}

/**
 * Оновлює вигляд кнопки руки залежно від стану handRaised.
 * Піднята: червона, пульсує. Опущена: жовта, статична.
 */
function updateHandButton() {
    const btn = document.getElementById('handToggleButton');
    if (!btn) return;

    if (handRaised) {
        btn.className = 'btn btn-outline-danger rounded-circle hand-raised-active';
        btn.innerHTML = '<i class="fas fa-hand-paper"></i>';
        btn.title = 'Опустити руку';
    } else {
        btn.className = 'btn btn-warning rounded-circle';
        btn.innerHTML = '<i class="fas fa-hand-paper"></i>';
        btn.title = 'Підняти руку';
    }
}


// ============================================================================
// 🎤 МІКРОФОН КОРИСТУВАЧА (ТЕКСТОВИЙ ЧАТ)
// ============================================================================

/**
 * Перемикає стан мікрофона через STOMP-команду /app/room/mute.
 * Це змінює стан у ChatRoomPresenceService на бекенді,
 * який транслюється модератору через batch-participants.
 */
function toggleMyMicrophone() {
    if (!stompClient || !stompClient.connected) return;

    const stationUuidToUse = (typeof stationUuid !== 'undefined' && stationUuid)
        ? stationUuid : "e5044caa-6eba-4af7-9f95-788b1dd238a9";

    stompClient.send("/app/room/mute", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({ targetUserUuid: curuuid, muted: !myMuted })
    }));

    myMuted = !myMuted;
    updateMyMicrophoneUI();
}

/**
 * Оновлює вигляд кнопки мікрофона в footer чату.
 */
function updateMyMicrophoneUI() {
    const micBtn = document.getElementById('microphoneToggleButton');
    if (!micBtn) return;

    if (myMuted) {
        micBtn.className = 'btn btn-outline-danger rounded-circle';
        micBtn.innerHTML = '<i class="fas fa-microphone-slash"></i>';
        micBtn.title = 'Мікрофон вимкнено (натисніть, щоб увімкнути)';
    } else {
        micBtn.className = 'btn btn-success rounded-circle';
        micBtn.innerHTML = '<i class="fas fa-microphone"></i>';
        micBtn.title = 'Мікрофон увімкнено (натисніть, щоб вимкнути)';
    }
}


// ============================================================================
// 🔔 СПОВІЩЕННЯ
// ============================================================================

/**
 * Показує сповіщення користувачеві.
 * Використовує toastr, якщо доступний, інакше — кастомний floating div.
 *
 * @param {string} message - Текст сповіщення
 * @param {string} type    - Тип: 'success' | 'warning' | 'error' | 'info'
 */
function showEfirNotification(message, type = 'info') {
    if (window.toastr) {
        switch (type) {
            case 'success': toastr.success(message, '🎙️ Ефір', { timeOut: 4000 }); break;
            case 'warning': toastr.warning(message, '🎙️ Ефір', { timeOut: 4000 }); break;
            case 'error':   toastr.error(message, '🎙️ Ефір', { timeOut: 5000 }); break;
            default:        toastr.info(message, '🎙️ Ефір', { timeOut: 3000 });
        }
    } else {
        // Fallback: кастомне сповіщення без toastr
        const colors = {
            success: '#00b894', warning: '#fdcb6e',
            error: '#d63031', info: '#6c5ce7'
        };
        const notif = document.createElement('div');
        notif.style.cssText = `
            position: fixed; top: 80px; right: 20px;
            background: ${colors[type] || colors.info}; color: white;
            padding: 15px 20px; border-radius: 10px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.2);
            z-index: 9999; font-weight: 500; max-width: 320px;
            animation: slideInRight 0.3s ease;`;
        notif.textContent = message;
        document.body.appendChild(notif);

        setTimeout(() => {
            notif.style.opacity = '0';
            notif.style.transition = 'opacity 0.3s';
            setTimeout(() => notif.remove(), 300);
        }, 3500);
    }
}


// ============================================================================
// 🛠️ ДОПОМІЖНІ ФУНКЦІЇ
// ============================================================================

/** Надсилає запит на оновлення списку користувачів */
function getuserlist() {
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/userslist", {}, JSON.stringify({
            uuid: curuuid, send: Date.now()
        }));
    }
}

/** Надсилає запит на оновлення списку кімнат */
function getroomlist() {
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/roomslist", {}, JSON.stringify({
            uuid: curuuid, roomuuid: curroom, send: Date.now()
        }));
    }
}

/**
 * Форматує дату/час для відображення в метаданих бульбашки.
 * @param {string|number} idate - Дата у форматі ISO або timestamp
 * @returns {string} Відформатована дата "DD.MM.YYYY HH:mm"
 */
function date2string(idate) {
    let d = new Date(idate);
    return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}.${d.getFullYear()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/** Обробник медіа-повідомлень (зображення, аудіо в чаті) */
function onMediaPublic(payload) {
    try {
        renderBubble(JSON.parse(payload.body), false);
    } catch (e) {
        console.error("Error media parse", e);
    }
}


// ============================================================================
// 🌍 ЕКСПОРТ У ГЛОБАЛЬНУ ОБЛАСТЬ ВИДИМОСТІ
// ============================================================================
// Функції експортуються в window, щоб бути доступними з:
//   - inline onclick атрибутів у messenger.html
//   - інших JS-модулів (GreenRoomUI, MainRoomAudio)

window.sendpublic = sendpublic;
window.sendprivate = sendprivate;
window.uploadImage = uploadImage;
window.selectroom = selectroom;
window.selectuserFromPublic = selectuserFromPublic;
window.toggleHand = toggleHand;
window.raiseHand = raiseHand;
window.lowerHand = lowerHand;
window.toggleMyMicrophone = toggleMyMicrophone;
window.showEfirNotification = showEfirNotification;
window.handleSend = handleSend;

/** Перемикає видимість сайдбару на мобільних пристроях */
window.toggleSidebar = function() {
    const sidebar = document.getElementById('chatSidebar');
    if (sidebar) sidebar.classList.toggle('hidden');
};


// ============================================================================
// 🚀 ІНІЦІАЛІЗАЦІЯ ПРИ ЗАВАНТАЖЕННІ СТОРІНКИ
// ============================================================================

window.addEventListener('DOMContentLoaded', () => {
    // Підключаємося до STOMP
    connect();

    // Періодичне оновлення списків (кожні 15 секунд)
    setInterval(getuserlist, 15000);
    setInterval(getroomlist, 15000);
});