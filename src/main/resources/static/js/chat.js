'use strict';
/**
 Логіка сучасного чату RFA
 Включає підтримку WebSockets (STOMP), Емодзі, адаптивний рендеринг повідомлень
 та механізм "підняття руки" для інтерактивного ефіру.
 Реалізовано стійкість до розривів з'єднання на мобільних пристроях.
 */

var stompClient = null;
var clientuuid = null; // Поточний UUID співрозмовника (якщо приватний чат)
var toclientname = null;
var subcurroom = null;
var subprivate = null;
var subpublic = null;
var subusers = null;
var subroom = null;
var subPresence = null;
var subMediaToPublic = null;
var reconnectTimeout = null;
var reconnectDelay = 1000; // Початкова затримка перепідключення 1с

// ✅ Стан "піднятої руки" для поточного користувача
var handRaised = false;
var handRaiseTimeout = null; // Таймер для автоопускання

const messagesArea = document.getElementById('mainMessagesArea');

/**
 Встановлення з'єднання через SockJS та STOMP
 */
function connect() {
    if (stompClient && stompClient.connected) return;
    console.log("Attempting to connect to STOMP...");
    var socket = new SockJS('/rfachat');
    stompClient = Stomp.over(socket);
    stompClient.debug = function(str) {
        console.log("STOMP: " + str);
    };
    stompClient.connect({
        heartbeatIncoming: 8000,
        heartbeatOutgoing: 8000
    }, onConnected, onError);
}

/**
 Callback при успішному підключенні
 */
function onConnected() {
    console.log("Connected to STOMP cluster");
    reconnectDelay = 1000;
    if (reconnectTimeout) clearTimeout(reconnectTimeout);
    const statusEl = document.getElementById('connectstatus');
    if (statusEl) statusEl.innerHTML = '<i class="fas fa-circle me-1" style="font-size: 0.5rem;"></i> Онлайн';

    // Підписки на канали
    subcurroom = stompClient.subscribe('/topic/' + curroom, onPublicMessageReceived);
    subpublic = stompClient.subscribe('/topic/chat.public.' + curuuid, onPublicMessageReceived);
    subprivate = stompClient.subscribe('/topic/chat.private.' + curuuid, onPrivateMessageReceived);
    subusers = stompClient.subscribe('/topic/chat.userslist.' + curuuid, onUserList);
    subroom = stompClient.subscribe('/topic/chat.roomslist.' + curuuid, onRoomList);
    subPresence = stompClient.subscribe('/topic/presence-update', onPresenceUpdate);
    subMediaToPublic = stompClient.subscribe('/topic/chat.media', onMediaPublic);

    // Вітаємося з сервером для реєстрації сесії та отримання історії
    stompClient.send("/app/hello", {}, JSON.stringify({
        uuid: curuuid,
        roomuuid: curroom,
        send: Date.now()
    }));

    // Початкове завантаження списків
    getuserlist();
    getroomlist();
}

/**
 Обробка помилок та розриву з'єднання
 */
function onError(error) {
    console.error("STOMP Error/Disconnect:", error);
    const statusEl = document.getElementById('connectstatus');
    if (statusEl) statusEl.innerHTML = '<span class="text-danger"><i class="fas fa-sync-alt fa-spin me-1"></i> Відключено. Перепідключення...</span>';
    if (reconnectTimeout) clearTimeout(reconnectTimeout);
    reconnectTimeout = setTimeout(() => {
        reconnectDelay = Math.min(reconnectDelay * 2, 30000);
        connect();
    }, reconnectDelay);
}

/**
 Детекція повернення користувача на вкладку (критично для мобільних пристроїв)
 */
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        console.log("App wake up. Checking connection...");
        if (!stompClient || !stompClient.connected) {
            connect();
        }
    }
});

function onPresenceUpdate(msg) {
    console.log("=== Presence Update Signal ===");
    getuserlist();
    getroomlist();
}

/**
 Рендеринг повідомлення у вигляді бульбашки (bubble)
 */
function renderBubble(msg, isPrivate = false) {
    const msgId = `msg-${msg.send}-${msg.fromuuid}`;
    if (document.getElementById(msgId)) return;

    const isMe = msg.fromuuid === curuuid;
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

    const container = isPrivate ? document.getElementById('privatearea') : document.getElementById('publicarea');
    if (container) {
        container.appendChild(bubble);
        messagesArea.scrollTo({ top: messagesArea.scrollHeight, behavior: 'smooth' });
    }
}

function onPublicMessageReceived(payload) {
    try {
        var msg = JSON.parse(payload.body);
        renderBubble(msg, false);
    } catch (e) { console.error("Error parsing public message", e); }
}

function onPrivateMessageReceived(payload) {
    try {
        var msg = JSON.parse(payload.body);
        if (clientuuid && (msg.fromuuid === clientuuid || msg.touuid === clientuuid)) {
            renderBubble(msg, true);
        } else if (msg.fromuuid !== curuuid) {
            if (window.toastr) toastr.info('Нове повідомлення від ' + msg.fromname);
            getuserlist();
        }
    } catch (e) { console.error("Error parsing private message", e); }
}

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
            const initials = u.name ? u.name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2) : '??';
            const badge = u.unreadCount > 0 ? `<span class="badge rounded-pill bg-danger animate__animated animate__pulse animate__infinite" style="font-size: 0.7rem;">${u.unreadCount}</span>` : '';
            const displayName = u.name || 'Користувач';

            item.innerHTML = `
                <div class="chat-avatar">${initials}</div>
                <div class="chat-info flex-grow-1">
                    <div class="chat-name">${displayName}</div>
                    <div class="chat-status">Написати особисто</div>
                </div>
                <div class="chat-meta">${badge}</div>
            `;
            item.onclick = () => selectuserFromPublic(u.uuid, u.name);
            container.appendChild(item);
        });
    } catch (e) { console.error("Error updating user list", e); }
}

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
                </div>
            `;
            item.onclick = () => selectroom(r.uuid, r.name);
            container.appendChild(item);
        });
    } catch (e) { console.error("Error updating room list", e); }
}

/**
 Вибір кімнати
 */
function selectroom(uuid, name) {
    // ✅ Автоматично опускаємо руку при зміні кімнати
    if (handRaised) {
        lowerHand();
    }

    if (curroom === uuid && !clientuuid) return;
    if (subcurroom) subcurroom.unsubscribe();

    curroom = uuid;
    document.getElementById('publicarea').innerHTML = '';
    document.getElementById('privatearea').innerHTML = '';
    document.getElementById('chatTitle').innerText = name;

    const statusEl = document.getElementById('connectstatus');
    if (statusEl) statusEl.innerHTML = '<i class="fas fa-circle me-1" style="font-size: 0.5rem;"></i> Онлайн';

    clientuuid = null;
    toclientname = null;

    if (stompClient && stompClient.connected) {
        subcurroom = stompClient.subscribe('/topic/' + curroom, onPublicMessageReceived);
        stompClient.send("/app/room/history", {}, JSON.stringify({
            uuid: curuuid,
            roomuuid: curroom
        }));
    }
    getroomlist();
    getuserlist();

    if (window.innerWidth <= 768) toggleSidebar();
}

function selectuserFromPublic(uuid, name) {
    if (clientuuid === uuid) return;
    clientuuid = uuid;
    toclientname = name;
    document.getElementById('chatTitle').innerText = `Чат з ${name}`;

    const statusEl = document.getElementById('connectstatus');
    if (statusEl) statusEl.innerHTML = '<i class="fas fa-lock me-1"></i> Приватний канал';

    document.getElementById('publicarea').innerHTML = '';
    document.getElementById('privatearea').innerHTML = '';

    if (stompClient && stompClient.connected) {
        stompClient.send("/app/private/history", {}, JSON.stringify({
            uuid: curuuid,
            touuid: clientuuid
        }));
    }
    getuserlist();
    getroomlist();

    if (window.innerWidth <= 768) toggleSidebar();
}

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

async function uploadImage(file) {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
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
            const imgHtml = `<img src="${result.url}" class="img-fluid rounded-3 mt-2 mb-2" style="max-height: 300px; cursor: pointer;" onclick="window.open('${result.url}', '_blank')">`;
            if (clientuuid) {
                stompClient.send("/app/private", {}, JSON.stringify({
                    uuid: curuuid, send: Date.now(), fromname: curusername, fromuuid: curuuid,
                    toname: toclientname, touuid: clientuuid, body: imgHtml
                }));
            } else {
                stompClient.send("/app/public", {}, JSON.stringify({
                    uuid: curuuid, send: Date.now(), fromname: curusername, fromuuid: curuuid,
                    body: imgHtml, roomuuid: curroom
                }));
            }
        }
    } catch (e) { console.error('Upload error:', e); }
    finally { if (input) input.placeholder = 'Напишіть повідомлення...'; }
}

// ========================================================================
// ✅ МЕХАНІЗМ "ПІДНЯТИ / ОПУСТИТИ РУКУ"
// ========================================================================

/**
 * Перемикає стан руки: підняти ↔ опустити
 */
function toggleHand() {
    if (handRaised) {
        lowerHand();
    } else {
        raiseHand();
    }
}

/**
 * Гість піднімає руку
 */
function raiseHand() {
    if (!stompClient || !stompClient.connected) {
        alert('Немає з\'єднання з сервером');
        return;
    }

    // Визначаємо stationUuid (з глобальної змінної або fallback)
    const stationUuidToUse = (typeof stationUuid !== 'undefined' && stationUuid)
        ? stationUuid
        : "e5044caa-6eba-4af7-9f95-788b1dd238a9";

    stompClient.send("/app/hand/raise", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({
            stationUuid: stationUuidToUse,
            roomuuid: curroom
        })
    }));

    handRaised = true;
    updateHandButton();
    console.log("🙋 Руку піднято!");

    // Скидаємо попередній таймер, якщо він був
    if (handRaiseTimeout) {
        clearTimeout(handRaiseTimeout);
    }

    // Автоматичне опускання через 5 хвилин (захист від "завислих" рук)
    handRaiseTimeout = setTimeout(() => {
        if (handRaised) {
            console.log("⏰ Тайм-аут: автоматичне опускання руки");
            lowerHand();
        }
    }, 5 * 60 * 1000);
}

/**
 * Гість опускає руку
 */
function lowerHand() {
    if (!stompClient || !stompClient.connected) return;

    const stationUuidToUse = (typeof stationUuid !== 'undefined' && stationUuid)
        ? stationUuid
        : "e5044caa-6eba-4af7-9f95-788b1dd238a9";

    stompClient.send("/app/hand/lower", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({
            stationUuid: stationUuidToUse,
            targetUuid: curuuid
        })
    }));

    handRaised = false;
    updateHandButton();
    console.log("✋ Руку опущено!");

    // Очищаємо таймер автоопускання
    if (handRaiseTimeout) {
        clearTimeout(handRaiseTimeout);
        handRaiseTimeout = null;
    }
}

/**
 * Оновлює вигляд кнопки-перемикача
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

// ========================================================================
// Допоміжні функції
// ========================================================================

function getuserlist() {
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/userslist", {}, JSON.stringify({ uuid: curuuid, send: Date.now() }));
    }
}

function getroomlist() {
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/roomslist", {}, JSON.stringify({ uuid: curuuid, roomuuid: curroom, send: Date.now() }));
    }
}

function date2string(idate) {
    let d = new Date(idate);
    let day = String(d.getDate()).padStart(2, '0');
    let month = String(d.getMonth() + 1).padStart(2, '0');
    let year = d.getFullYear();
    let hours = String(d.getHours()).padStart(2, '0');
    let minutes = String(d.getMinutes()).padStart(2, '0');
    return `${day}.${month}.${year} ${hours}:${minutes}`;
}

function onMediaPublic(payload) {
    try {
        var msg = JSON.parse(payload.body);
        renderBubble(msg, false);
    } catch (e) { console.error("Error media parse", e); }
}

// Експортуємо функції у глобальну область для HTML
window.sendpublic = sendpublic;
window.sendprivate = sendprivate;
window.uploadImage = uploadImage;
window.selectroom = selectroom;
window.selectuserFromPublic = selectuserFromPublic;
window.toggleHand = toggleHand;
window.raiseHand = raiseHand;
window.lowerHand = lowerHand;
window.toggleSidebar = function() {
    const sidebar = document.getElementById('chatSidebar');
    if (sidebar) sidebar.classList.toggle('hidden');
};

// Ініціалізація при завантаженні
window.addEventListener('DOMContentLoaded', () => {
    connect();
    setInterval(getuserlist, 15000);
    setInterval(getroomlist, 15000);
});