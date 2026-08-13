/**
 * ⚙️ Глобальна конфігурація WebRTC голосового чату (версія v2).
 */
const CONFIG = {
    // Базовий шлях до REST API Spring Boot бекенду
    BACKEND_API: "/api/v1q/webrtc/stream",

    // Адреса WebSocket підключення до Janus Gateway
    JANUS_WS_URL: "wss://test.rfa.toloka.media/janus-ws",

    // ID аудіокімнати за замовчуванням при першому вході на сторінку
    DEFAULT_ROOM_ID: 802,

    // Інтервал оновлення списку кімнат та учасників (в мілісекундах)
    ROOMS_REFRESH_INTERVAL: 5000,

    // CSS класи для кольорового маркування логів в консолі інтерфейсу
    BADGE_CLASSES: {
        'INFO': 'badge-info',       // Синій
        'SUCCESS': 'badge-success',   // Зелений
        'ERROR': 'badge-error',     // Червоний
        'WARN': 'badge-error',      // Червоний (попередження)
        'DEBUG': 'badge-debug'      // Сірий (дебаг)
    }
};