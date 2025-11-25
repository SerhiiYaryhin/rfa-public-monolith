/*!
* Start Rfa - відслідковуємо кнопки на панелі керування станцією.
*
*
*/
//
// Scripts
// https://rfa.toloka.media

let intervalId;
let curstatus;
var toloka = {};

window.addEventListener('DOMContentLoaded', event => {

function getkeyhrev () {
    toloka.blink = document.getElementById('toStation');
    toloka.bstart = document.getElementById('startStation');
    toloka.bstop = document.getElementById('stopStation');
    toloka.bdel = document.getElementById('delStation');
    toloka.changepswdiv = document.getElementById('setpswarea');

    toloka.blinkdisplay = toloka.blink.style.display;
    toloka.bstartdisplay = toloka.bstart.style.display;
    toloka.bstopdisplay = toloka.bstop.style.display;
    toloka.bdeldisplay = toloka.bdel.style.display;
    toloka.bchangepswdiv =toloka.changepswdiv.style.display;

    toloka.blink.style.display = "none";
    toloka.bstart.style.display = "none";
    toloka.bstop.style.display = "none";
    toloka.bdel.style.display = "none";
    toloka.changepswdiv.style.display = "none";
    toloka.changepswdiv.style.display = "none";
}



async function getStatus() {
    let siteurl = "https://rfa.toloka.media/api/1.0/ps/"+stationId;
    const response = await fetch(siteurl);
    const data = await response.json();
    stationstate = data["status"];
    myspan = document.getElementById('spanstationstate');

    switch (stationstate) {
//        case '6':
        case '7':
            myspan.textContent = 'Всі сервіси працюють нормально.';
            myspan.setAttribute("style", "color:green;");
            toloka.blink.style.display = toloka.blinkdisplay;
            toloka.bstart.style.display = "none";
            toloka.bstop.style.display = toloka.bstopdisplay;
            toloka.bdel.style.display = "none";
            toloka.changepswdiv.style.display = "none";
            break;
        case '0':
            myspan.textContent = 'Станція вимкнена';
            myspan.setAttribute("style", "color:red;");
            toloka.blink.style.display = "none";
            toloka.bstart.style.display = toloka.bstartdisplay
            toloka.bstop.style.display = "none";
            toloka.bdel.style.display = toloka.bdeldisplay ;
            toloka.changepswdiv.style.display = toloka.bchangepswdiv;
            break;
        default:
            myspan.textContent = 'Станція в процесі запуску або зупинки';
            myspan.setAttribute("style", "color:orange;");
            toloka.blink.style.display = "none";
            toloka.bstart.style.display = "none";
            toloka.bstop.style.display = "none";
            toloka.bdel.style.display = "none";
            break;
    }
  return data;
}


getkeyhrev();

intervalId = setInterval(getStatus, 3000);


});