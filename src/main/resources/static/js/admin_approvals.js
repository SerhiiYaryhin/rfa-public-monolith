/**
 * Керування заявками на схвалення (Адмінка)
 * AJAX дії для адрес та документів
 */

$(document).ready(function() {
    // Схвалення адреси
    window.approveAddress = async function(addressId, clientId) {
        if (!confirm('Схвалити цю адресу?')) return;

        try {
            const response = await fetch(`/api/1.0/admin/approval/address/${addressId}/approve`, {
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('input[name="_csrf"]')?.value
                }
            });
            const result = await response.json();

            if (result.success) {
                toastr.success(result.message);
                $(`#address-row-${addressId}`).fadeOut();
                updateCounter(clientId);
            } else {
                toastr.error(result.message || 'Помилка');
            }
        } catch (e) {
            toastr.error('Системна помилка: ' + e.message);
        }
    };

    // Оновлення статусу документа
    window.updateDocStatus = async function(docId, status, clientId) {
        const comment = prompt('Коментар (необовʼязково):', '');
        if (comment === null) return;

        try {
            const response = await fetch(`/api/1.0/admin/approval/document/${docId}/status?status=${status}&comment=${encodeURIComponent(comment)}`, {
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('input[name="_csrf"]')?.value
                }
            });
            const result = await response.json();

            if (result.success) {
                toastr.success(result.message);
                if (status === 'STATUS_APPROVED' || status === 'STATUS_REJECTED') {
                    $(`#doc-row-${docId}`).fadeOut();
                    updateCounter(clientId);
                }
            } else {
                toastr.error(result.message || 'Помилка');
            }
        } catch (e) {
            toastr.error('Системна помилка: ' + e.message);
        }
    };

    function updateCounter(clientId) {
        const $badge = $(`#badge-total-${clientId}`);
        let count = parseInt($badge.text());
        count--;
        if (count <= 0) {
            $(`#user-card-${clientId}`).fadeOut();
        } else {
            $badge.text(count);
        }
    }
});
