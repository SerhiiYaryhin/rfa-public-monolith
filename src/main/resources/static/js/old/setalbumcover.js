/*!
* Start Rfa -
*
*
*/
//
// Scripts
// https://rfa.toloka.media

var toloka = {};

    function setcover ( curpic, alcoid, albumid, cdid ) {
        setnewcover.alcoid = alcoid;
        setnewcover.clickpic = curpic;
        setborder();
        var frm = document.getElementById('setalbumcover') || null;
        if(frm) {
           frm.action = '/creater/setalbumcover/'+alcoid+'/'+albumid+'/'+cdid;
        }
}

    function showborder() {
        if (setnewcover.activeborder) {
            setnewcover.activepic.style.border = "10px solid red";
            setnewcover.activepic.style.padding = "10px 10px 10px 10px";
        } else {
            setnewcover.activepic.style.border = "0px solid red";
            setnewcover.activepic.style.padding = "0px 0px 0px 0px";
        }
    }

    function setborder () {
        if (typeof setnewcover.activepic != "undefined") {
            if (setnewcover.clickpic != setnewcover.activepic) {
                setnewcover.activeborder = false;
                showborder();
                setnewcover.activeborder = true;
                setnewcover.activepic = setnewcover.clickpic;
                showborder();
            }
        } else {
            setnewcover.activepic = setnewcover.clickpic;
            setnewcover.activeborder = true;
            showborder();
        }
    }

    function coverset() {
        getStatus(setnewcover.alcoid, setnewcover.albumid, setnewcover.cdid);
    }

async function getStatus(alcoid, albumid, cdid) {
    let siteurl = "https://rfa.toloka.media/api/1.0/setalbumcover/"+alcoid+'/'+albumid+"/"+cdid
    const response = await fetch(siteurl);
    const data = await response.json();
    stationstate = data["status"];

}


window.addEventListener('DOMContentLoaded', event => {

console.log("стартували");

});