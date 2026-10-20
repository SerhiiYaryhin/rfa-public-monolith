/**
 🔌 Модуль JanusConnection (версія v2).
 Керує підключенням до медіасервера Janus Gateway:
 - Отримання токена сесії з бекенду (Spring Security Integration).
 - Ініціалізація WebSocket сесії Janus.
 - Прикріплення плагіна AudioBridge.
 - Обробка та відтворення віддалених аудіо треків (WebRTC Audio).
 */
const JanusConnection = {
    // Екземпляр головного об'єкта Janus JS SDK
    janusInstance: null,
    // Хендл прикріпленого плагіна AudioBridge
    audioBridge: null,
    // Авторизаційний токен поточної сесії користувача (UUID)
    currentSessionToken: null,
    // Флаг готовності Janus з'єднання
    isJanusReady: false,
    // Статус виконання авто-перепідключення
    isReconnecting: false,

    /**
     🔑 Отримання токена сесії користувача від нашого бекенду.
     Запобігає передачі api_secret клієнту.
     */
    async initUserSession() {
        Logger.log("🔑 Отримання токена користувача...", "INFO");
        try {
            const response = await fetch(`${CONFIG.BACKEND_API}/token`);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const data = await response.json();

            this.currentSessionToken = data.token;
            Logger.log(`✅ Токен отримано: ${this.currentSessionToken.substring(0, 8)}...`, "SUCCESS");
            return true;
        } catch (error) {
            Logger.log(`❌ Помилка отримання токена: ${error.message}`, "ERROR");
            Logger.updateConnectionStatus(false, "Помилка авторизації");
            setTimeout(() => this.initUserSession(), 5000);
            return false;
        }
    },

    /**
     🔌 Ініціалізація з'єднання з Janus медіасервером через WebSocket.
     */
    initJanusConnection() {
        return new Promise((resolve, reject) => {
            if (!this.currentSessionToken) return reject("Токен відсутній");
            Logger.log("🔌 Підключення до Janus WebSocket...", "INFO");

            Janus.init({
                debug: ["error", "warn"],
                callback: () => {
                    this.janusInstance = new Janus({
                        server: CONFIG.JANUS_WS_URL,
                        token: this.currentSessionToken,
                        success: () => {
                            Logger.log("✅ WebSocket підключено", "SUCCESS");
                            this.attachAudioBridgePlugin().then(resolve).catch(reject);
                        },
                        error: (error) => {
                            Logger.log(`❌ Помилка WebSocket: ${error}`, "ERROR");
                            if (!this.isReconnecting) {
                                this.isReconnecting = true;
                                setTimeout(() => {
                                    this.isReconnecting = false;
                                    this.initJanusConnection().then(resolve).catch(reject);
                                }, 5000);
                            } else {
                                reject(error);
                            }
                        },
                        destroyed: () => {
                            Logger.log("🔌 Сесію Janus знищено", "INFO");
                            Logger.updateConnectionStatus(false);
                            this.isJanusReady = false;
                        }
                    });
                }
            });
        });
    },

    /**
     🎙️ Прикріплення до плагіна AudioBridge (janus.plugin.audiobridge).
     */
    attachAudioBridgePlugin() {
        return new Promise((resolve, reject) => {
            Logger.log("🎙️ Прикріплення до AudioBridge...", "INFO");
            this.janusInstance.attach({
                plugin: "janus.plugin.audiobridge",
                token: this.currentSessionToken,
                opaqueId: "audiobridge-" + Janus.randomString(12),
                success: (pluginHandle) => {
                    this.audioBridge = pluginHandle;
                    this.isJanusReady = true;
                    Logger.updateConnectionStatus(true, "Готово до роботи");
                    Logger.log("✅ AudioBridge прикріплено та готовий", "SUCCESS");
                    resolve();
                },
                error: (error) => {
                    Logger.log(`❌ Помилка прикріплення плагіна: ${error}`, "ERROR");
                    reject(error);
                },
                onremotetrack: (track, mid, on) => {
                    this.handleRemoteTrack(track, mid, on);
                },
                onlocaltrack: (track, mid) => {
                    Logger.log(`🎤 Локальний трек додано: ${track.kind} (mid=${mid})`, "DEBUG");
                },
                onmessage: (msg, jsep) => RoomManager.handleJanusMessage(msg, jsep),
                oncleanup: () => Logger.log("🧹 З'єднання плагіна очищено", "INFO"),
                ondetached: () => Logger.log("⚠️ Плагін від'єднано сервером", "WARN")
            });
        });
    },

    /**
     🔊 Обробка віддаленого аудіо треку WebRTC (Mixed audio).
     @param {MediaStreamTrack} track Медіа трек від Janus
     @param {string} mid Ідентифікатор медіа-каналу
     @param {boolean} on Флаг підключення (true) чи відключення (false)
     */
    handleRemoteTrack(track, mid, on) {
        Logger.log(`🔊 ${on ? 'Додано' : 'Видалено'} віддалений трек: ${track.kind} (mid=${mid})`, "SUCCESS");

        if (on && track.kind === 'audio') {
            let audio = document.getElementById("audioPlayer");

            // Якщо плеєра немає на сторінці, створюємо динамічно
            if (!audio) {
                audio = document.getElementById("remoteAudio-" + mid);
                if (!audio) {
                    audio = document.createElement("audio");
                    audio.id = "remoteAudio-" + mid;
                    audio.autoplay = true;
                    audio.controls = true;
                    document.body.appendChild(audio);
                }
            }

            const stream = new MediaStream([track]);
            audio.srcObject = stream;

            // 🎯 ВИПРАВЛЕННЯ: Зупиняємо старий моніторинг перед запуском нового
            AudioMonitor.stop();

            audio.play().then(() => {
                Logger.log("✅ Аудіо відтворення розпочато успішно", "SUCCESS");
                AudioMonitor.init(audio);
            }).catch(e => {
                Logger.log(`⚠️ Автопрогравання заблоковано: ${e.message}`, "WARN");
                // Навіть якщо autoplay заблоковано, ініціалізуємо монітор (він буде готовий до кліку користувача)
                AudioMonitor.init(audio);
            });
        } else if (!on) {
            // Якщо трек видалено (користувач вийшов або змінив кімнату), зупиняємо візуалізацію
            AudioMonitor.stop();
        }
    }
};