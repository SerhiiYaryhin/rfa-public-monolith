/**
 * Система логування
 */
const Logger = {
    log(message, type = 'INFO') {
        const consoleDiv = document.getElementById("logConsole");
        if (!consoleDiv) {
            console.log(`[${type}] ${message}`);
            return;
        }

        const time = new Date().toLocaleTimeString();
        const badgeClass = CONFIG.BADGE_CLASSES[type] || 'badge-info';

        consoleDiv.innerHTML += `<div><span class="badge ${badgeClass}">${time}</span> ${message}</div>`;
        consoleDiv.scrollTop = consoleDiv.scrollHeight;
        console.log(`[${type}] ${message}`);
    },

    updateConnectionStatus(connected, text = null) {
        const indicator = document.getElementById("statusIndicator");
        const statusText = document.getElementById("statusText");

        if (indicator) {
            indicator.className = connected
                ? "status-indicator status-connected"
                : "status-indicator status-disconnected";
        }
        if (statusText) {
            statusText.textContent = text || (connected ? "Підключено до Janus" : "Відключено");
        }
    }
};