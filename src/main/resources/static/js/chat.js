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
var subMediaToPublic = null;

const messagesArea = document.getElementById('mainMessagesArea');

/**
 * Встановлення з'єднання через SockJS та STOMP
 */
function connect() {
    var socket = new SockJS('/rfachat');
    stompClient = Stomp.over(socket);
    stompClient.connect({
        heartbeatIncoming: 8000,
        heartbeatOutgoing: 8000
    }, onConnected, onError);
    
    stompClient.debug = null; // Вимикаємо зайвий лог у консоль
}

/**
 * Callback при успішному підключенні
 */
function onConnected() {
    // Підписки на канали
    subcurroom = stompClient.subscribe('/topic/' + curroom, onPublicMessageReceived);
    subpublic = stompClient.subscribe('/public/' + curuuid, onPublicMessageReceived);
    subprivate = stompClient.subscribe('/private/' + curuuid, onPrivateMessageReceived);
    subusers = stompClient.subscribe('/userslist/' + curuuid, onUserList);
    subroom = stompClient.subscribe('/roomslist/' + curuuid, onRoomList);
    subMediaToPublic = stompClient.subscribe('/media', onMediaPublic);

    // Підписка на події оновлення присутності (через RabbitMQ на бекенді)
    stompClient.subscribe('/topic/presence-update', function(msg) {
        console.log("=== Presence Update Received ===");
        getuserlist();
        getroomlist();
    });

    // Вітаємося з сервером
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
 * Рендеринг повідомлення у вигляді бульбашки (bubble)
 */
function renderBubble(msg, isPrivate = false) {
    const isMe = msg.fromuuid === curuuid;
    const bubble = document.createElement('div');
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
    container.appendChild(bubble);
    
    // Автопрокрутка до останнього повідомлення
    messagesArea.scrollTo({ top: messagesArea.scrollHeight, behavior: 'smooth' });
}

/**
 * Обробка вхідних публічних повідомлень
 */
function onPublicMessageReceived(payload) {
    var msg = JSON.parse(payload.body);
    renderBubble(msg, false);
}

/**
 * Обробка вхідних приватних повідомлень
 */
function onPrivateMessageReceived(payload) {
    var msg = JSON.parse(payload.body);
    // Показуємо повідомлення лише якщо ми в приватному чаті з цією людиною
    if (clientuuid && (msg.fromuuid === clientuuid || msg.touuid === clientuuid)) {
        renderBubble(msg, true);
    } else if (msg.fromuuid !== curuuid) {
        // Якщо це не наше власне повідомлення і ми в іншому чаті - показуємо нотифікацію
        if (typeof toastr !== 'undefined') {
            toastr.info('Нове приватне повідомлення від ' + msg.fromname);
        } else {
            console.log('Нове приватне повідомлення від ' + msg.fromname);
        }
    }
}

/**
 * Оновлення списку користувачів
 */
function onUserList(payload) {
    let jbody = JSON.parse(payload.body);
    const container = document.getElementById('userlist');
    container.innerHTML = '';
    
    let users = JSON.parse(jbody.body);
    users.forEach(function(uJson) {
        let u = JSON.parse(uJson);
        const item = document.createElement('div');
        item.className = `chat-item list-group-item ${clientuuid === u.uuid ? 'active' : ''}`;
        
        const initials = u.name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
        
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
}

/**
 * Оновлення списку кімнат
 */
function onRoomList(payload) {
    let jbody = JSON.parse(payload.body);
    const container = document.getElementById('roomlist');
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
    
    // Скидаємо приватного співрозмовника при переході в кімнату
    clientuuid = null;
    toclientname = null;
    
    subcurroom = stompClient.subscribe('/topic/' + curroom, onPublicMessageReceived);
    
    // Запитуємо історію повідомлень для нової кімнати
    stompClient.send("/app/room/history", {}, JSON.stringify({
        uuid: curuuid,
        roomuuid: curroom
    }));
    
    // Оновлюємо списки
    getroomlist();
    
    // На мобілках ховаємо сайдбар
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

    // Очищуємо вікно повідомлень для чистого перемикання
    document.getElementById('publicarea').innerHTML = '';
    document.getElementById('privatearea').innerHTML = '';

    // Запитуємо приватну історію
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
    
    // Показуємо стан завантаження (можна додати прогрес-бар)
    document.getElementById('newmessage').placeholder = 'Завантаження зображення...';

    try {
        const response = await fetch('/api/chat/upload-image', {
            method: 'POST',
            body: formData
        });
        const result = await response.json();
        
        if (result.success) {
            // Формуємо HTML-тег зображення для повідомлення
            const imgHtml = `<img src="${result.url}" class="img-fluid rounded-3 mt-2 mb-2" style="max-height: 300px; cursor: pointer;" onclick="window.open('${result.url}', '_blank')">`;
            
            // Відправляємо як звичайне повідомлення (залежно від контексту)
            if (window.clientuuid) {
                stompClient.send("/app/private", {}, JSON.stringify({
                    uuid: curuuid,
                    send: Date.now(),
                    fromname: curusername,
                    fromuuid: curuuid,
                    toname: toclientname,
                    touuid: clientuuid,
                    body: imgHtml
                }));
            } else {
                stompClient.send("/app/public", {}, JSON.stringify({
                    uuid: curuuid,
                    send: Date.now(),
                    fromname: curusername,
                    fromuuid: curuuid,
                    body: imgHtml,
                    roomuuid: curroom
                }));
            }
        } else {
            alert('Помилка завантаження: ' + result.error);
        }
    } catch (e) {
        console.error('Upload error:', e);
        alert('Помилка при завантаженні файлу');
    } finally {
        document.getElementById('newmessage').placeholder = 'Напишіть повідомлення...';
        document.getElementById('imageInput').value = ''; // Скидаємо інпут
    }
}

// Допоміжні функції
function getuserlist() { stompClient.send("/app/userslist", {}, JSON.stringify({ uuid: curuuid, send: Date.now() })); }
function getroomlist() { stompClient.send("/app/roomslist", {}, JSON.stringify({ uuid: curuuid, roomuuid: curroom, send: Date.now() })); }
function onError() { console.error("WebSocket Connection Error"); }

function date2string(idate) {
    let d = new Date(idate);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function onMediaPublic(payload) {
    var msg = JSON.parse(payload.body);
    renderBubble(msg, false);
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
    setInterval(getuserlist, 10000); // Оновлюємо список рідше для економії трафіку
    setInterval(getroomlist, 10000);
});
