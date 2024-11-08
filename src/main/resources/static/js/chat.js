'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
//var connectingElement = document.querySelector('');
var connectingElement = document.getElementById('connectstatus');
var stompClient = null;
var username = null;
var clientuuid = null;
var toclientname = null;
var subcurroom = null;
var subprivate = null;
var subpublic = null
var subhello = null;
var subusers = null;
var subroom = null;
var subMediaToPublic = null; // підписка для публікації повідомлень з медіа в пабліку
var subheartbeats = null;





//var colors = [
//    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
//    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
//];
function connect() {
    //username = document.querySelector('#name').value.trim();
//    var socket = new WebSocket("ws://localhost:8080/rfachat");
//    var socket = new WebSocket("https://rfa.toloka.media/rfachat");
    var socket = new SockJS('/rfachat');
    stompClient = Stomp.over(socket);
    stompClient.connect({
        heartbeatIncoming: 8000,
        heartbeatOutgoing: 8000},
        onConnected, onError, onCloseEventCallback);
        stompClient.debug = debugToConsole;
        stompClient.reconnect_delay = 300;
}

function onCloseEventCallback(str) {
    console.log("========== CLOSE EVENT "+str);
}

function debugToConsole (str) {
    console.log("========== DEBUG "+str);
}

function onConnected() {

    subcurroom = stompClient.subscribe('/topic/'+curroom, onPublicMessageReceived);

    subpublic = stompClient.subscribe('/public/'+curuuid, onPublicMessageReceived);

    subprivate = stompClient.subscribe('/private/'+curuuid, onPrivateMessageReceived);

    subusers = stompClient.subscribe('/userslist/'+curuuid, onUserList);

    subroom = stompClient.subscribe('/roomslist/'+curuuid, onRoomList);

    subheartbeats = stompClient.subscribe('/heartbeats', onHeartbeats);

    subMediaToPublic = stompClient.subscribe('/media', onMediaPublic);


    stompClient.send("/app/hello",
        {},
        JSON.stringify({
        uuid: curuuid,
        roomuuid: curroom,
        send: Date.now()
        })
    );
    getuserlist();
    getroomlist();

    ligthOnRoom(curroom);
}

function getuserlist() {
   stompClient.send("/app/userslist",
        {},
        JSON.stringify({
        uuid: curuuid,
        send: Date.now()
        })
    );
 }

function getroomlist() {
    stompClient.send("/app/roomslist",
        {},
        JSON.stringify({
        uuid: curuuid,
        roomuuid: curroom,
        send: Date.now()
        })
    );
}

function onUserList(payload) {
// userlist
//    console.log("==== onUserList ==========================================");
    let jbody = JSON.parse(payload.body);
    // clear userlist
    document.getElementById('userlist').innerHTML = '';
    // перелік користувачів передається в body повідомлення - масив Маp???
    // тепер цикл по jbody.body
    let bodyuserslist = JSON.parse(jbody.body);
    bodyuserslist.forEach(function(elem, ind) {
        // elem to json
//        console.log(elem);
        let ejson = JSON.parse(elem);
        // create span with name
        let spanname = document.createElement('span');
        spanname.textContent = ejson.name;
        ejson.uuid.includes(curusername) ? spanname.style.color = 'red' : spanname.style.color = 'black';

        spanname.onclick= function () {selectuserFromPublic(ejson.uuid, ejson.name)};
        // create div with id = userUuid from
        let iDiv = document.createElement('div');
        iDiv.id =  ejson.uuid;
        //iDiv.onclick= function () {selectuser(ejson.uuid)};

        // add span to div
        iDiv.appendChild(spanname);
        // add user div to userList
        document.getElementById('userlist').appendChild(iDiv);
    });
}

function onRoomList(payload) {
// roomlist
    let jbody = JSON.parse(payload.body);
    // clear userlist
    document.getElementById('roomlist').innerHTML = '';
    // перелік кімнат передається в body повідомлення - масив Маp???
    // тепер цикл по jbody.body
    let bodyuserslist = JSON.parse(jbody.body);
    bodyuserslist.forEach(function(elem, ind) {
        // elem to json
//        console.log(elem);
        let ejson = JSON.parse(elem);
        // create span with name
        let spanname = document.createElement('span');
        spanname.textContent = ejson.name;
        ejson.uuid.includes(curroom) ? spanname.style.color = 'green' : spanname.style.color = 'black';
        spanname.id = ejson.uuid;
        spanname.onclick= function () {selectroom(ejson.uuid)};
        // create div with id = userUuid from
        let iDiv = document.createElement('div');

        // add span to div
        iDiv.appendChild(spanname);
        // add user div to userList
        document.getElementById('roomlist').appendChild(iDiv);
    });
}

function onPublicMessageReceived(payload) {
    var jbody = JSON.parse(payload.body);

    var iDiv = document.createElement('div');
    if (jbody.rtype === 0 ) {
        iDiv.innerHTML = jbody.body;

//        document.getElementById('publicarea').appendChild(iDiv);
//        var objDiv = document.getElementById("publicarea");
//        objDiv.scrollTop = objDiv.scrollHeight;

    } else {
        var spanname = document.createElement('span');
        spanname.textContent = date2string(jbody.send)+" "+jbody.fromname+': ';

        var spanbody = document.createElement('span');
        spanbody.textContent = jbody.body;

        //var iDiv = document.createElement('div');
            iDiv.id = jbody.fromuuid;
            iDiv.onclick= function () {selectuserFromPublic(jbody.fromuuid, jbody.fromname)};

        iDiv.appendChild(spanname);
        iDiv.appendChild(spanbody);
    }

    document.getElementById('publicarea').appendChild(iDiv);

    var objDiv = document.getElementById("publicarea");
    objDiv.scrollTop = objDiv.scrollHeight;
}

function onPrivateMessageReceived(payload) {
    var jbody = JSON.parse(payload.body);

    var spanname = document.createElement('span');
    if (curuuid.includes(jbody.fromuuid)) {
        spanname.textContent = date2string(jbody.send)+' > '+jbody.toname+': ';
    } else {
        spanname.textContent = date2string(jbody.send)+" "+jbody.fromname+': ';
    }

    var spanbody = document.createElement('span');
    spanbody.textContent = jbody.body;

    var iDiv = document.createElement('div');
    if (curuuid.includes(jbody.fromuuid)) {
        iDiv.id = jbody.touuid;
        iDiv.onclick= function () {selectuserFromPublic(jbody.touuid, jbody.toname)};
    } else {
        iDiv.id = jbody.fromuuid;
        iDiv.onclick= function () {selectuserFromPublic(jbody.fromuuid, jbody.fromname)};
    }

//    iDiv.setAttribute("onclick", "selectuser(this)");

    iDiv.appendChild(spanname);
    iDiv.appendChild(spanbody);
    document.getElementById('privatearea').appendChild(iDiv);

    var objDiv = document.getElementById("privatearea");
    objDiv.scrollTop = objDiv.scrollHeight;
//    console.log("==== PRIVATE ==========================================");
}

function unsubscribeRoom(lcurroom) {subcurroom.unsubscribe();}

function subscribeRoom(lcurroom) {
//        console.log("subscribeRoom");
        subcurroom = stompClient.subscribe('/topic/'+lcurroom, onPublicMessageReceived);
            stompClient.send("/app/hello",{},
                JSON.stringify({
                    uuid: curuuid,
                    roomuuid: curroom,
                    send: Date.now()
                })
            );
    }

function LigthOffRoom(lcurroom) {
        document.getElementById(lcurroom).style.color = 'black';
    }

function ligthOnRoom(lcurroom) {
        document.getElementById(lcurroom).style.color = 'green';
    }

function selectroom(toroom) {
// відписуємося від кімнати
    unsubscribeRoom(curroom);
    LigthOffRoom(curroom);
// clear public
    var myNode = document.getElementById("publicarea");
    myNode.innerHTML = '';
// clear private
    var myNode = document.getElementById("privatearea");
    myNode.innerHTML = '';
// підписуємося на кімнату
//    console.log('////////// before change room: '+curroom);
    curroom = toroom
//    console.log('////////// after change room: '+curroom);
    subscribeRoom(curroom);
    ligthOnRoom(curroom);
}

function sendpublic(event) {
    stompClient.send("/app/public",
        {},
        JSON.stringify({
        uuid: curuuid,
        send: Date.now(),
        fromname: curusername,
        fromuuid: curuuid,
        body: document.getElementById('newmessage').value,
        roomuuid: curroom
        })
    );
    document.getElementById('newmessage').value = "";
}

function sendprivate(event) {
    let sinput = document.getElementById('newmessage').value;
    if (sinput.length === 0 ) {return}
    stompClient.send("/app/private",
        {},
        JSON.stringify({uuid: curuuid,
        send: Date.now(),
        fromname: curusername,
        fromuuid: curuuid,
        toname: toclientname,
        touuid: clientuuid,
        body: sinput
        })
    );
    document.getElementById('newmessage').value = "";
}

function selectuserFromPublic(luuid, lname) {
    clientuuid = luuid;
    toclientname = lname;
    document.getElementById('connectstatus').innerHTML = 'Кому написати: '+lname;
    document.getElementById('connectstatus').style.color = "red";

}

function selectuser(event ) {
    var spanname = document.getElementById(event.id);
    clientuuid = spanname.id;
    toclientname = spanname.innerText;
//    console.log(toclientname);
    document.getElementById('connectstatus').innerHTML = 'Кому:'+spanname.innerText;
}

function onError(){
    console.log("Щось пішло не так при коннекті :( ");
}

function date2string (idate) {
    let d = new Date(idate);
    let day = '0' + d.getDate();
    let month = '0'+ (d.getMonth()+1);
    let year = "0" + d.getFullYear();
    let hours = "0" + d.getHours();
    let minutes = "0" + d.getMinutes();
    let datestring = day.slice(-2) + "." + month.slice(-2) + "." + year.slice(-2) + " " + hours.slice(-2) + ":" + minutes.slice(-2);
    // 16-05-2015 09:50
    return datestring;

}

function onHeartbeats () {
}

function onMediaPublic (payload) {
    var jbody = JSON.parse(payload.body);
    var iDiv = document.createElement('div'); //головний div повідомлення
        iDiv.innerHTML = jbody.body;

        document.getElementById('publicarea').appendChild(iDiv);
        var objDiv = document.getElementById("publicarea");
        objDiv.scrollTop = objDiv.scrollHeight;
}

window.addEventListener('DOMContentLoaded', event => {
connect();

var intervaluser = setInterval(getuserlist, 8000);
var intervalroom = setInterval(getroomlist, 8000);
})