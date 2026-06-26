/**
 * Конфігурація WebRTC клієнта
 */
const CONFIG = {
    BACKEND_API: "/api/v1q/webrtc/stream",
    JANUS_WS_URL: "wss://test.rfa.toloka.media/janus-ws",
    DEFAULT_ROOM_ID: 802,
    ROOMS_REFRESH_INTERVAL: 5000,

    // CSS класи для бейджів
    BADGE_CLASSES: {
        'INFO': 'badge-info',
        'SUCCESS': 'badge-success',
        'ERROR': 'badge-error',
        'WARN': 'badge-error',
        'DEBUG': 'badge-debug'
    }
};