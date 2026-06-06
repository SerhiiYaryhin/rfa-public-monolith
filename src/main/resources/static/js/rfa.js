/*!
* Start Rfa - відслідковуємо кнопки на панелі керування станцією.
* Оновлено для роботи з AJAX (Фаза 4)
*/

let intervalId;
var toloka = {};

window.addEventListener('DOMContentLoaded', event => {

    function getElements() {
        toloka.blink = document.getElementById('toStation');
        toloka.bstart = document.getElementById('startStation');
        toloka.bstop = document.getElementById('stopStation');
        toloka.bdel = document.getElementById('delStation');
        toloka.myspan = document.getElementById('spanstationstate');
        
        // Зберігаємо оригінальні стани відображення
        if (toloka.bstart) toloka.bstartdisplay = "inline-block";
        if (toloka.bstop) toloka.bstopdisplay = "inline-block";
        if (toloka.blink) toloka.blinkdisplay = "inline-block";
        if (toloka.bdel) toloka.bdeldisplay = "inline-block";
    }

    async function getStatus() {
        if (!stationId) return;
        let siteurl = "/api/1.0/ps/" + stationId;
        try {
            const response = await fetch(siteurl);
            const data = await response.json();
            const stationstate = data["status"];
            
            if (!toloka.myspan) return;

            switch (stationstate) {
                case '7':
                    toloka.myspan.textContent = 'Всі сервіси працюють нормально';
                    toloka.myspan.className = 'fw-bold px-3 text-success';
                    if (toloka.blink) toloka.blink.style.display = "inline-block";
                    if (toloka.bstart) toloka.bstart.style.display = "none";
                    if (toloka.bstop) toloka.bstop.style.display = "inline-block";
                    if (toloka.bdel) toloka.bdel.style.display = "none";
                    break;
                case '0':
                    toloka.myspan.textContent = 'Станція вимкнена';
                    toloka.myspan.className = 'fw-bold px-3 text-danger';
                    if (toloka.blink) toloka.blink.style.display = "none";
                    if (toloka.bstart) toloka.bstart.style.display = "inline-block";
                    if (toloka.bstop) toloka.bstop.style.display = "none";
                    if (toloka.bdel) toloka.bdel.style.display = "inline-block";
                    break;
                default:
                    toloka.myspan.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Станція в процесі...';
                    toloka.myspan.className = 'fw-bold px-3 text-warning';
                    if (toloka.blink) toloka.blink.style.display = "none";
                    if (toloka.bstart) toloka.bstart.style.display = "none";
                    if (toloka.bstop) toloka.bstop.style.display = "none";
                    if (toloka.bdel) toloka.bdel.style.display = "none";
                    break;
            }
        } catch (e) {
            console.error("Status check error", e);
        }
    }

    // AJAX управління
    window.stationAction = async function(action) {
        if (!confirm('Виконати дію ' + action + '?')) return;
        
        const url = `/api/1.0/user/station/${stationId}/${action}`;
        const method = action === 'delete' ? 'DELETE' : 'POST';
        
        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('input[name="_csrf"]')?.value
                }
            });
            const result = await response.json();
            
            if (result.success) {
                toastr.success(result.message);
                if (action === 'delete') window.location.href = '/user/stations';
                getStatus(); // Одразу оновлюємо стан
            } else {
                toastr.error(result.message || 'Помилка виконання операції');
            }
        } catch (e) {
            toastr.error('Системна помилка: ' + e.message);
        }
    };

    getElements();
    if (stationId) {
        getStatus();
        intervalId = setInterval(getStatus, 5000);
    }
});
