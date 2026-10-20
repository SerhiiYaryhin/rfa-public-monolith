/**
 🔌 Модуль JanusConnection (версія v2).
 Керує підключенням до медіасервера Janus Gateway:
 - Отримання токена сесії з бекенду (Spring Security Integration).
 - Ініціалізація WebSocket сесії Janus.
 - Прикріплення плагіна AudioBridge.
 - Обробка та відтворення віддалених аудіо треків (WebRTC Audio).
 - Передача треків у AudioMonitor для візуалізації рівня звуку.
 */
const JanusConnection = {
    janusInstance: null,
    audioBridge: null,
    currentSessionToken: null,
    isJanusReady: false,
    isReconnecting: false,

    /**
     🔑 Отримання токена сесії користувача від нашого бекенду.
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
                // 🎤 Сумісність з обома сигнатурами janus.js: (track, on) та (track, mid, on)
                onlocaltrack: (track, midOrOn, on) => {
                    const added = (typeof on === 'boolean') ? on
                        : (typeof midOrOn === 'boolean') ? midOrOn
                            : true;
                    Logger.log(`🎤 Локальний трек: ${track ? track.kind : 'null'}, added=${added}`, "DEBUG");
                    if (added && track && track.kind === 'audio') {
                        AudioMonitor.attachLocalTrack(track);
                    }
                },
                onmessage: (msg, jsep) => RoomManager.handleJanusMessage(msg, jsep),
                oncleanup: () => Logger.log("🧹 З'єднання плагіна очищено", "INFO"),
                ondetached: () => Logger.log("⚠️ Плагін від'єднано сервером", "WARN")
            });
        });
    },

    /**
     🔊 Обробка віддаленого аудіо треку WebRTC (Mixed audio).
     */
    handleRemoteTrack(track, mid, on) {
        Logger.log(`🔊 ${on ? 'Додано' : 'Видалено'} віддалений трек: ${track.kind} (mid=${mid})`, "SUCCESS");

        if (on && track.kind === 'audio') {
            let audio = document.getElementById("audioPlayer");

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

            // ✅ БЕЗ stop()! Монітор живє весь час життя сторінки.
            audio.play().then(() => {
                Logger.log("✅ Аудіо відтворення розпочато успішно", "SUCCESS");
                AudioMonitor.attachRemote(audio);
            }).catch(e => {
                Logger.log(`⚠️ Автопрогравання заблоковано: ${e.message}`, "WARN");
                AudioMonitor.attachRemote(audio);
            });
        } else if (!on) {
            // Трек видалено (вихід з кімнати): лише обнуляємо смужки, контекст НЕ чіпаємо
            AudioMonitor.stop();
        }
    }
};