/**
 * Управління аудіокімнатами
 */
const RoomManager = {
    currentRoomId: null,
    currentDisplay: null,
    pendingRoomId: null,
    isSwitchingRoom: false,

    async switchRoom(newRoomId) {
        newRoomId = parseInt(newRoomId);
        if (isNaN(newRoomId)) return;

        if (this.isSwitchingRoom) {
            Logger.log(`⏳ Вже виконується перехід, ігноруємо`, "WARN");
            return;
        }
        this.isSwitchingRoom = true;

        try {
            if (!JanusConnection.isJanusReady || !JanusConnection.audioBridge) {
                Logger.log(`⏳ Janus підключається. Кімнату ${newRoomId} буде обрано автоматично.`, "WARN");
                this.pendingRoomId = newRoomId;
                return;
            }

            Logger.log(`🔄 Перехід в кімнату ${newRoomId}...`, "INFO");

            const response = await fetch(`${CONFIG.BACKEND_API}/connectclienttoroom`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({roomid: newRoomId})
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const serverResponse = await response.json();
            if (!serverResponse.success) throw new Error(serverResponse.error || "Помилка сервера");
            Logger.log(`✅ Бекенд підтвердив перехід. Display: "${serverResponse.display}"`, "SUCCESS");

            if (this.currentRoomId !== null) {
                Logger.log(`👋 Вихід з кімнати ${this.currentRoomId}...`, "DEBUG");
                Logger.log("🔌 Від'єднуємо плагін AudioBridge...", "DEBUG");
                JanusConnection.audioBridge.detach();
                JanusConnection.audioBridge = null;
                JanusConnection.isJanusReady = false;
                await new Promise(r => setTimeout(r, 500));
                Logger.log("🎙️ Прикріплюємо плагін заново...", "DEBUG");
                await JanusConnection.attachAudioBridgePlugin();
            }

            this.currentRoomId = newRoomId;
            this.currentDisplay = serverResponse.display;
            this.joinRoom(newRoomId, this.currentDisplay);
        } catch (error) {
            Logger.log(`❌ Помилка переходу: ${error.message}`, "ERROR");
        } finally {
            this.isSwitchingRoom = false;
        }
    },

    joinRoom(roomId, display) {
        Logger.log(`🚪 Заходимо в кімнату ${roomId} як "${display}"...`, "INFO");
        const joinMessage = {
            request: "join",
            room: roomId,
            display: display,
            muted: false
        };
        Logger.log(`📤 Відправка: ${JSON.stringify(joinMessage)}`, "DEBUG");
        JanusConnection.audioBridge.send({message: joinMessage});
    },

    handleJanusMessage(msg, jsep) {
        Logger.log(`📥 Отримано від Janus: ${JSON.stringify(msg).substring(0, 300)}...`, "DEBUG");
        const event = msg.audiobridge;

        if (msg.error_code || msg.error) {
            Logger.log(`❌ Помилка від Janus: ${msg.error_code} - ${msg.error}`, "ERROR");
            return;
        }

        if (event === "joined") {
            if (msg.id) {
                const myId = msg.id;
                const myDisplay = msg.display || this.currentDisplay;
                Logger.log(`✅ Увійшли в кімнату ${this.currentRoomId}. Ваш ID: ${myId}, Display: "${myDisplay}"`, "SUCCESS");
                UIRenderer.loadRooms();

                if (!jsep) {
                    this.createOffer();
                }
            } else {
                Logger.log(`👤 Інший користувач увійшов в кімнату`, "INFO");
                UIRenderer.loadRooms();
            }
        }

        if (event === "left") { Logger.log(`👋 Хтось вийшов з кімнати`, "INFO"); UIRenderer.loadRooms(); }
        if (event === "kicked") Logger.log(`⚠️ Вас видалено з кімнати`, "WARN");
        if (event === "destroyed") { this.currentRoomId = null; Logger.log(`💥 Кімнату знищено`, "WARN"); }

        if (jsep) {
            this.handleJsep(jsep);
        }
    },

    createOffer() {
        Logger.log("🎤 Створюємо WebRTC Offer (мікрофон)...", "DEBUG");
        const offerStartTime = Date.now();

        JanusConnection.audioBridge.createOffer({
            media: {audioSend: true, audioRecv: true, video: false},
            success: (localJsep) => {
                const elapsed = Date.now() - offerStartTime;
                Logger.log(`📤 SDP Offer готовий (за ${elapsed}мс). Надсилаємо configure...`, "INFO");
                JanusConnection.audioBridge.send({
                    message: {request: "configure", muted: false},
                    jsep: localJsep
                });
            },
            error: (err) => Logger.log(`❌ Offer Error: ${JSON.stringify(err)}`, "ERROR")
        });
    },

    handleJsep(jsep) {
        Logger.log(`📡 Отримано JSEP типу: ${jsep.type}`, "DEBUG");

        if (jsep.type === "answer") {
            JanusConnection.audioBridge.handleRemoteJsep({
                jsep: jsep,
                success: () => {
                    Logger.log("✅ WebRTC Handshake завершено", "SUCCESS");
                    this.monitorIceState();
                },
                error: (err) => Logger.log(`❌ Answer Error: ${JSON.stringify(err)}`, "ERROR")
            });
        } else if (jsep.type === "offer") {
            JanusConnection.audioBridge.createAnswer({
                jsep: jsep,
                media: {audioSend: true, audioRecv: true, video: false},
                success: (jsepAnswer) => {
                    JanusConnection.audioBridge.send({
                        message: {request: "configure", muted: false},
                        jsep: jsepAnswer
                    });
                },
                error: (err) => Logger.log(`❌ CreateAnswer Error: ${JSON.stringify(err)}`, "ERROR")
            });
        }
    },

    monitorIceState() {
        let checkCount = 0;
        const checkInterval = setInterval(() => {
            if (JanusConnection.audioBridge.webrtcStuff?.pc) {
                const pc = JanusConnection.audioBridge.webrtcStuff.pc;
                Logger.log(`🔍 ICE: ${pc.iceConnectionState} (${++checkCount})`, "DEBUG");

                if (pc.iceConnectionState === "connected" || pc.iceConnectionState === "completed") {
                    Logger.log("✅ ICE з'єднання встановлено!", "SUCCESS");
                    clearInterval(checkInterval);
                } else if (pc.iceConnectionState === "failed") {
                    Logger.log("❌ ICE FAILED!", "ERROR");
                    clearInterval(checkInterval);
                }
            }
            if (checkCount >= 15) { clearInterval(checkInterval); }
        }, 2000);
    }
};