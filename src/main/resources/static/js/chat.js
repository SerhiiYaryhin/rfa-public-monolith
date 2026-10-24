'use strict';
/**
 Логіка сучасного чату RFA
 Включає підтримку WebSockets (STOMP), Емодзі, адаптивний рендеринг повідомлень,
 механізм "підняття руки", зворотний зв'язок від модератора,
 та інтеграцію з GreenRoomUI для Janus WebRTC.
 */

var stompClient = null;
var clientuuid = null;
var toclientname = null;
var subcurroom = null;
var subprivate = null;
var subpublic = null;
var subusers = null;
var subroom = null;
var subPresence = null;
var subMediaToPublic = null;
var subEfirControl = null;
var subHandStatus = null;
var reconnectTimeout = null;
var reconnectDelay = 1000;

// Стан "піднятої руки" для поточного користувача
var handRaised = false;
var handRaiseTimeout = null;
var myMuted = false;

const messagesArea = document.getElementById('mainMessagesArea');

function connect() {
    if (stompClient && stompClient.connected) return;
    console.log("Attempting to connect to STOMP...");
    var socket = new SockJS('/rfachat');
    stompClient = Stomp.over(socket);
    stompClient.debug = function(str) { console.log("STOMP: " + str); };
    stompClient.connect({
        heartbeatIncoming: 8000,
        heartbeatOutgoing: 8000
    }, onConnected, onError);
}

function onConnected() {
    console.log("Connected to STOMP cluster");
    reconnectDelay = 1000;
    if (reconnectTimeout) clearTimeout(reconnectTimeout);
    const statusEl = document.getElementById('connectstatus');
    if (statusEl) statusEl.innerHTML = '<i class="fas fa-circle me-1" style="font-size: 0.5rem;"></i> Онлайн';

    subcurroom = stompClient.subscribe('/topic/' + curroom, onPublicMessageReceived);
    subpublic = stompClient.subscribe('/topic/chat.public.' + curuuid, onPublicMessageReceived);
    subprivate = stompClient.subscribe('/topic/chat.private.' + curuuid, onPrivateMessageReceived);
    subusers = stompClient.subscribe('/topic/chat.userslist.' + curuuid, onUserList);
    subroom = stompClient.subscribe('/topic/chat.roomslist.' + curuuid, onRoomList);
    subPresence = stompClient.subscribe('/topic/presence-update', onPresenceUpdate);
    subMediaToPublic = stompClient.subscribe('/topic/chat.media', onMediaPublic);

    // ✅ Команди керування ефіром від модератора (переміщення, mute)
    subEfirControl = stompClient.subscribe('/topic/efir.control', onEfirControl);

    // ✅ Сповіщення про зміну статусу руки (прийнято/відхилено модератором)
    const handStatusStationUuid = (typeof stationUuid !== 'undefined' && stationUuid)
        ? stationUuid : "e5044caa-6eba-4af7-9f95-788b1dd238a9";
    subHandStatus = stompClient.subscribe(`/topic/efir.${handStatusStationUuid}.hand.status`, onHandStatusUpdate);
    console.log(`✅ Підписано на /topic/efir.${handStatusStationUuid}.hand.status`);

    stompClient.send("/app/hello", {}, JSON.stringify({
        uuid: curuuid, roomuuid: curroom, send: Date.now()
    }));

    getuserlist();
    getroomlist();
}

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

document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        if (!stompClient || !stompClient.connected) connect();
    }
});

function onPresenceUpdate(msg) {
    getuserlist();
    getroomlist();
}

// ================ КОМАНДИ КЕРУВАННЯ ЕФІРОМ ВІД МОДЕРАТОРА ================
function onEfirControl(payload) {
    try {
        const cmd = typeof payload.body === 'string' ? JSON.parse(payload.body) : payload.body;
        console.log("🎛️ Отримано команду ефіру:", cmd.action);

        switch (cmd.action) {
            case 'MOVE_YOURSELF':
                if (cmd.targetUserUuid === curuuid && cmd.targetRoomUuid && cmd.targetRoomUuid !== curroom) {
                    // 1. Опускаємо руку якщо піднята
                    if (handRaised) lowerHand();

                    // 2. Перемикаємо текстовий чат
                    selectroom(cmd.targetRoomUuid, cmd.targetRoomName || 'Кімната ефіру');

                    // 3. ✅ Відкриваємо модальне вікно з Janus AudioBridge
                    // targetJanusRoomId = MessageRoom.id цільової кімнати (передається з бекенду)
                    const janusRoomId = cmd.targetJanusRoomId;
                    if (janusRoomId && typeof GreenRoomUI !== 'undefined') {
                        const roomLabel = cmd.autoMute ? '🔴 Прямий ефір' : '🟡 Green Room';
                        GreenRoomUI.enter(janusRoomId, roomLabel, !!cmd.autoMute);
                    } else {
                        // Fallback: якщо Janus недоступний або targetJanusRoomId не передано
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

// ================ ЗВОРОТНІЙ ЗВ'ЯЗОК ПРО СТАТУС РУКИ ================
function onHandStatusUpdate(payload) {
    try {
        const data = typeof payload.body === 'string' ? JSON.parse(payload.body) : payload.body;

        if (data.targetUserUuid !== curuuid) return;

        console.log("🙋 Статус руки змінено:", data.status, data.message);

        // Скидаємо стан руки локально
        handRaised = false;
        if (handRaiseTimeout) {
            clearTimeout(handRaiseTimeout);
            handRaiseTimeout = null;
        }
        updateHandButton();

        // Показуємо сповіщення гостю
        let notifType = 'info';
        if (data.status === 'ACCEPTED') notifType = 'success';
        if (data.status === 'DECLINED') notifType = 'warning';

        showEfirNotification(data.message || 'Статус руки змінено', notifType);
    } catch (e) {
        console.error("Error handling hand status update:", e);
    }
}

// ================ РЕНДЕРИНГ ПОВІДОМЛЕНЬ ================
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
    try { renderBubble(JSON.parse(payload.body), false); }
    catch (e) { console.error("Error parsing public message", e); }
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
            const badge = u.unreadCount > 0 ? `<span class="badge rounded-pill bg-danger" style="font-size: 0.7rem;">${u.unreadCount}</span>` : '';
            item.innerHTML = `<div class="chat-avatar">${initials}</div> <div class="chat-info flex-grow-1"> <div class="chat-name">${u.name || 'Користувач'}</div> <div class="chat-status">Написати особисто</div> </div> <div class="chat-meta">${badge}</div>`;
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
            item.innerHTML = `<div class="chat-avatar"><i class="fas fa-hashtag"></i></div> <div class="chat-info"> <div class="chat-name">${r.name}</div> <div class="chat-status">Публічна кімната</div> </div>`;
            item.onclick = () => selectroom(r.uuid, r.name);
            container.appendChild(item);
        });
    } catch (e) { console.error("Error updating room list", e); }
}

function selectroom(uuid, name) {
    // Автоматично опускаємо руку при зміні кімнати
    if (handRaised) lowerHand();
    if (curroom === uuid && !clientuuid) return;
    if (subcurroom) subcurroom.unsubscribe();
    curroom = uuid;
    document.getElementById('publicarea').innerHTML = '';
    document.getElementById('privatearea').innerHTML = '';
    document.getElementById('chatTitle').innerText = name;
    clientuuid = null;
    toclientname = null;
    if (stompClient && stompClient.connected) {
        subcurroom = stompClient.subscribe('/topic/' + curroom, onPublicMessageReceived);
        stompClient.send("/app/room/history", {}, JSON.stringify({ uuid: curuuid, roomuuid: curroom }));
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
    document.getElementById('publicarea').innerHTML = '';
    document.getElementById('privatearea').innerHTML = '';
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/private/history", {}, JSON.stringify({ uuid: curuuid, touuid: clientuuid }));
    }
    getuserlist();
    getroomlist();
    if (window.innerWidth <= 768) toggleSidebar();
}

function sendpublic() {
    const input = document.getElementById('newmessage');
    if (!input || !input.value.trim() || !stompClient || !stompClient.connected) return;
    stompClient.send("/app/public", {}, JSON.stringify({
        uuid: curuuid, send: Date.now(), fromname: curusername, fromuuid: curuuid,
        body: input.value, roomuuid: curroom
    }));
    input.value = "";
}

function sendprivate() {
    const input = document.getElementById('newmessage');
    if (!input || !input.value.trim() || !clientuuid || !stompClient || !stompClient.connected) return;
    stompClient.send("/app/private", {}, JSON.stringify({
        uuid: curuuid, send: Date.now(), fromname: curusername, fromuuid: curuuid,
        toname: toclientname, touuid: clientuuid, body: input.value
    }));
    input.value = "";
}

async function uploadImage(file) {
    if (!file || !file.type.startsWith('image/')) { alert('Можна завантажувати лише зображення'); return; }
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

// ================ ПІДНЯТТЯ РУКИ ================
function toggleHand() {
    if (handRaised) lowerHand();
    else raiseHand();
}

function raiseHand() {
    if (!stompClient || !stompClient.connected) { alert('Немає з\'єднання з сервером'); return; }
    const stationUuidToUse = (typeof stationUuid !== 'undefined' && stationUuid)
        ? stationUuid : "e5044caa-6eba-4af7-9f95-788b1dd238a9";
    stompClient.send("/app/hand/raise", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({ stationUuid: stationUuidToUse, roomuuid: curroom })
    }));
    handRaised = true;
    updateHandButton();
    if (handRaiseTimeout) clearTimeout(handRaiseTimeout);
    handRaiseTimeout = setTimeout(() => {
        if (handRaised) lowerHand();
    }, 5 * 60 * 1000);
}

function lowerHand() {
    if (!stompClient || !stompClient.connected) return;
    const stationUuidToUse = (typeof stationUuid !== 'undefined' && stationUuid)
        ? stationUuid : "e5044caa-6eba-4af7-9f95-788b1dd238a9";
    stompClient.send("/app/hand/lower", {}, JSON.stringify({
        uuid: curuuid,
        body: JSON.stringify({ stationUuid: stationUuidToUse, targetUuid: curuuid, reason: 'BY_USER' })
    }));
    handRaised = false;
    if (handRaiseTimeout) { clearTimeout(handRaiseTimeout); handRaiseTimeout = null; }
    updateHandButton();
}

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

// ================ МІКРОФОН КОРИСТУВАЧА ================
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

// ================ СПОВІЩЕННЯ ================
function showEfirNotification(message, type = 'info') {
    if (window.toastr) {
        switch (type) {
            case 'success': toastr.success(message, '🎙️ Ефір', { timeOut: 4000 }); break;
            case 'warning': toastr.warning(message, '🎙️ Ефір', { timeOut: 4000 }); break;
            case 'error':   toastr.error(message, '🎙️ Ефір', { timeOut: 5000 }); break;
            default:        toastr.info(message, '🎙️ Ефір', { timeOut: 3000 });
        }
    } else {
        const colors = { success: '#00b894', warning: '#fdcb6e', error: '#d63031', info: '#6c5ce7' };
        const notif = document.createElement('div');
        notif.style.cssText = `position: fixed; top: 80px; right: 20px; background: ${colors[type] || colors.info}; color: white; padding: 15px 20px; border-radius: 10px; box-shadow: 0 4px 20px rgba(0,0,0,0.2); z-index: 9999; font-weight: 500; max-width: 320px; animation: slideInRight 0.3s ease;`;
        notif.textContent = message;
        document.body.appendChild(notif);
        setTimeout(() => {
            notif.style.opacity = '0';
            notif.style.transition = 'opacity 0.3s';
            setTimeout(() => notif.remove(), 300);
        }, 3500);
    }
}

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
    return `${String(d.getDate()).padStart(2,'0')}.${String(d.getMonth()+1).padStart(2,'0')}.${d.getFullYear()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}
function onMediaPublic(payload) {
    try { renderBubble(JSON.parse(payload.body), false); }
    catch (e) { console.error("Error media parse", e); }
}

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
window.toggleSidebar = function() {
    const sidebar = document.getElementById('chatSidebar');
    if (sidebar) sidebar.classList.toggle('hidden');
};

window.addEventListener('DOMContentLoaded', () => {
    connect();
    setInterval(getuserlist, 15000);
    setInterval(getroomlist, 15000);
});