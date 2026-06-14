'use strict';

/**
 * Логіка сучасного чату RFA
 * Включає підтримку WebSockets (STOMP), Емодзі та адаптивний рендеринг повідомлень у вигляді бульбашок.
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

const messagesArea = document.getElementById('mainMessagesArea');

/**
 * Встановлення з'єднання через SockJS та STOMP
 */
function connect() {
    var socket = new SockJS('/rfachat');
    stompClient = Stomp.over(socket);
    
    // Вмикаємо логи для діагностики
    stompClient.debug = function(str) {
        console.log("STOMP: " + str);
    };

    stompClient.connect({
        heartbeatIncoming: 8000,
        heartbeatOutgoing: 8000
    }, onConnected, onError);
}

/**
 * Callback при успішному підключенні
 */
function onConnected() {
    console.log("Connected to STOMP");

    // Підписки на канали (використовуємо стандартизовані RabbitMQ префікси)
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

function onPresenceUpdate(msg) {
    console.log("=== Presence Update Signal ===");
    getuserlist();
    getroomlist();
}

/**
 * Рендеринг повідомлення у вигляді бульбашки (bubble)
 */
function renderBubble(msg, isPrivate = false) {
    // Унікальний ID для запобігання дублікатам (особливо при завантаженні історії)
    const msgId = `msg-${msg.send}-${msg.fromuuid}`;
    if (document.getElementById(msgId)) return;

    const isMe = msg.fromuuid === curuuid;
    const bubble = document.createElement('div');
    bubble.id = msgId;
    bubble.className = `bubble ${isMe ? 'bubble-me' : 'bubble-other'} animate__animated animate__fadeInUp animate__faster`;
    
    // Текст або HTML (для медіа)
    const content = document.createElement('div');
    content.innerHTML = msg.body;
    
    // Мета-інформація (час, ім'я)
    const meta = document.createElement('div');
    meta.className = 'bubble-meta';
    const time = date2string(msg.send);
    meta.innerHTML = `<span>${isMe ? '' : msg.fromname + ' • '}</span><span>${time}</span>`;
    
    bubble.appendChild(content);
    bubble.appendChild(meta);
    
    const container = isPrivate ? document.getElementById('privatearea') : document.getElementById('publicarea');
    if (container) {
        container.appendChild(bubble);
        // Автопрокрутка
        messagesArea.scrollTo({ top: messagesArea.scrollHeight, behavior: 'smooth' });
    }
}

/**
 * Обробка вхідних публічних повідомлень
 */
function onPublicMessageReceived(payload) {
    try {
        var msg = JSON.parse(payload.body);
        renderBubble(msg, false);
    } catch (e) { console.error("Error parsing public message", e); }
}

/**
 * Обробка вхідних приватних повідомлень
 */
function onPrivateMessageReceived(payload) {
    try {
        var msg = JSON.parse(payload.body);
        // Показуємо повідомлення лише якщо ми в приватному чаті з цією людиною
        if (clientuuid && (msg.fromuuid === clientuuid || msg.touuid === clientuuid)) {
            renderBubble(msg, true);
        } else if (msg.fromuuid !== curuuid) {
            // Нотифікація
            if (window.toastr) toastr.info('Нове повідомлення від ' + msg.fromname);
        }
    } catch (e) { console.error("Error parsing private message", e); }
}

/**
 * Оновлення списку користувачів
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
            
            const initials = u.name ? u.name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2) : '??';
            
            item.innerHTML = `
                <div class="chat-avatar">${initials}</div>
                <div class="chat-info">
                    <div class="chat-name">${u.name}</div>
                    <div class="chat-status">Написати особисто</div>
                </div>
            `;
            
            item.onclick = () => selectuserFromPublic(u.uuid, u.name);
            container.appendChild(item);
        });
    } catch (e) { console.error("Error updating user list", e); }
}

/**
 * Оновлення списку кімнат
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
            item.className = `chat-item list-group-item ${curroom === r.uuid ? 'active' : ''}`;
            
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
 * Вибір кімнати
 */
function selectroom(uuid, name) {
    if (curroom === uuid) return;
    
    if (subcurroom) subcurroom.unsubscribe();
    curroom = uuid;
    
    document.getElementById('publicarea').innerHTML = '';
    document.getElementById('privatearea').innerHTML = '';
    document.getElementById('chatTitle').innerText = name;
    document.getElementById('connectstatus').innerHTML = '<i class="fas fa-circle me-1" style="font-size: 0.5rem;"></i> Онлайн';
    
    clientuuid = null;
    toclientname = null;
    
    subcurroom = stompClient.subscribe('/topic/' + curroom, onPublicMessageReceived);
    
    stompClient.send("/app/room/history", {}, JSON.stringify({
        uuid: curuuid,
        roomuuid: curroom
    }));
    
    getroomlist();
    if (window.innerWidth <= 768) toggleSidebar();
}

/**
 * Вибір користувача для приватного чату
 */
function selectuserFromPublic(uuid, name) {
    if (clientuuid === uuid) return;

    clientuuid = uuid;
    toclientname = name;
    
    document.getElementById('chatTitle').innerText = `Чат з ${name}`;
    document.getElementById('connectstatus').innerHTML = '<i class="fas fa-lock me-1"></i> Приватний канал';

    document.getElementById('publicarea').innerHTML = '';
    document.getElementById('privatearea').innerHTML = '';

    stompClient.send("/app/private/history", {}, JSON.stringify({
        uuid: curuuid,
        touuid: clientuuid
    }));
    
    getuserlist();
    if (window.innerWidth <= 768) toggleSidebar();
}

/**
 * Відправка публічного повідомлення
 */
function sendpublic() {
    const input = document.getElementById('newmessage');
    if (!input.value.trim()) return;

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
 * Відправка приватного повідомлення
 */
function sendprivate() {
    const input = document.getElementById('newmessage');
    if (!input.value.trim() || !clientuuid) return;

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
 * Завантаження зображення
 */
async function uploadImage(file) {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
        alert('Можна завантажувати лише зображення');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);
    document.getElementById('newmessage').placeholder = 'Завантаження...';

    try {
        const response = await fetch('/api/chat/upload-image', { method: 'POST', body: formData });
        const result = await response.json();
        
        if (result.success) {
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
    finally { document.getElementById('newmessage').placeholder = 'Напишіть повідомлення...'; }
}

// Допоміжні функції
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
function onError(error) { 
    console.error("STOMP Error:", error);
    document.getElementById('connectstatus').innerHTML = '<span class="text-danger"><i class="fas fa-exclamation-triangle"></i> Помилка з\'єднання</span>';
}

function date2string(idate) {
    let d = new Date(idate);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
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
window.toggleSidebar = function() {
    document.getElementById('chatSidebar').classList.toggle('hidden');
};

// Ініціалізація при завантаженні
window.addEventListener('DOMContentLoaded', () => {
    connect();
    setInterval(getuserlist, 15000); 
    setInterval(getroomlist, 15000);
});
