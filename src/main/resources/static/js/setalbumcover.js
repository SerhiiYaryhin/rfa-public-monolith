/*!
 * Вибір обкладинки альбому
 * https://rfa.toloka.media
 */

var setnewcover = {};

/**
 * Викликається при кліку на обкладинку в галереї.
 * Формує action форми: /creater/setalbumcover/{storeUuid}/{albumUuid}/{cdId}
 * (відповідає сигнатурі CreaterAlbumController.GetAlbumSetCover)
 */
function selectCover(item) {
    // Знімаємо попереднє виділення
    document.querySelectorAll('.cover-item.selected').forEach(function (el) {
        el.classList.remove('selected');
    });
    // Виділяємо обрану
    item.classList.add('selected');

    var storeUuid = item.getAttribute('data-store');
    var albumUuid = item.getAttribute('data-album');
    var cdId = item.getAttribute('data-cd');

    setnewcover.alcoid = storeUuid;
    setnewcover.albumuuid = albumUuid;
    setnewcover.cdid = cdId;

    var frm = document.getElementById('setalbumcover');
    if (frm) {
        frm.action = '/creater/setalbumcover/' + storeUuid + '/' + albumUuid + '/' + cdId;
    }

    // Активуємо кнопку застосування
    var btn = document.getElementById('applyCoverBtn');
    if (btn) {
        btn.disabled = false;
    }
}

// Сумісність зі старою назвою функції (якщо десь викликається)
function setcover(curpic, storeUuid, albumUuid, cdId) {
    selectCover(curpic);
}

window.addEventListener('DOMContentLoaded', function () {
    console.log('setalbumcover: сторінку завантажено');
});