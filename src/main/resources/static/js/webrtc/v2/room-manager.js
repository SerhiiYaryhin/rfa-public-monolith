/**
 ️ Модуль RoomManager (версія v3 - виправлена).
 Керує процесом підключення клієнта до аудіокімнат Janus AudioBridge.
 Виправлено синтаксичні помилки (arrow functions, optional chaining) та додано вимкнення обробки звуку для мобільних.
 */
const RoomManager = {
    // ID кімнати, в якій зараз знаходиться користувач
    currentRoomId: null,
    // Відображуване ім'я поточного користувача
    currentDisplay: null,
    // Тимчасовий ID кімнати, куди користувач хоче зайти, якщо Janus ще не підключився
    pendingRoomId: null,
    // Статус виконання переходу між кімнатами
    isSwitchingRoom: false,

    /**
     🔄 Перехід в іншу аудіокімнату.
     */
    async switchRoom(newRoomId) {
        newRoomId = parseInt(newRoomId);
        if (isNaN(newRoomId)) return;

        // Запобігаємо подвійному кліку / паралельному виконанню переходу
        if (this.isSwitchingRoom) {
            Logger.log(` Вже виконується перехід, ігноруємо`, "WARN");
            return;
        }
        this.isSwitchingRoom = true;

        try {
            // Перевіряємо готовність з'єднання з Janus
            if (!JanusConnection.isJanusReady || !JanusConnection.audioBridge) {
                Logger.log(` Janus підключається. Кімнату ${newRoomId} буде обрано автоматично.`, "WARN");
                this.pendingRoomId = newRoomId;
                return;
            }

            Logger.log(`🔄 Перехід в кімнату ${newRoomId}...`, "INFO");

            // Повідомляємо бекенд про бажання зайти в нову кімнату
            const response = await fetch(`${CONFIG.BACKEND_API}/connectclienttoroom`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({roomid: newRoomId})
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const serverResponse = await response.json();
            if (!serverResponse.success) throw new Error(serverResponse.error || "Помилка сервера");

            Logger.log(`✅ Бекенд підтвердив перехід. Display: "${serverResponse.display}"`, "SUCCESS");

            // Якщо користувач вже сидить у кімнаті, очищуємо попереднє PeerConnection
            if (this.currentRoomId !== null) {
                Logger.log(`👋 Вихід з кімнати ${this.currentRoomId}...`, "DEBUG");

                // 🎯 Зупиняємо ICE моніторинг перед виходом
                if (typeof NetworkMonitor !== 'undefined') {
                    NetworkMonitor.stopIceMonitoring();
                }

                Logger.log("🔌 Від'єднуємо плагін AudioBridge...", "DEBUG");
                JanusConnection.audioBridge.detach();
                JanusConnection.audioBridge = null;
                JanusConnection.isJanusReady = false;

                // Коротка пауза для завершення деструкції на стороні Janus
                await new Promise(r => setTimeout(r, 500));

                Logger.log("🎙️ Прикріплюємо плагін заново...", "DEBUG");
                await JanusConnection.attachAudioBridgePlugin();
            }

            this.currentRoomId = newRoomId;
            this.currentDisplay = serverResponse.display;

            // Заходимо в нову кімнату
            this.joinRoom(newRoomId, this.currentDisplay);

        } catch (error) {
            Logger.log(` Помилка переходу: ${error.message}`, "ERROR");
        } finally {
            this.isSwitchingRoom = false;
        }
    },

    /**
     🚪 Відправка команди входу в кімнату AudioBridge плагіна.
     */
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

    /**
     Обробка вхідних текстових повідомлень від плагіна AudioBridge.
     */
    handleJanusMessage(msg, jsep) {
        Logger.log(`📥 Отримано від Janus: ${JSON.stringify(msg).substring(0, 300)}...`, "DEBUG");
        const event = msg.audiobridge;

        if (msg.error_code || msg.error) {
            Logger.log(`❌ Помилка від Janus: ${msg.error_code} - ${msg.error}`, "ERROR");
            return;
        }

        // Клієнт успішно приєднався до кімнати
        if (event === "joined") {
            if (msg.id) {
                const myId = msg.id;
                const myDisplay = msg.display || this.currentDisplay;
                Logger.log(`✅ Увійшли в кімнату ${this.currentRoomId}. Ваш ID: ${myId}, Display: "${myDisplay}"`, "SUCCESS");

                // Оновлюємо UI список учасників
                UIRenderer.loadRooms();

                // Якщо разом із подією joined не прийшов JSEP, ініціюємо створення локального WebRTC Offer
                if (!jsep) {
                    this.createOffer();
                }
            } else {
                Logger.log(` Інший користувач увійшов в кімнату`, "INFO");
                UIRenderer.loadRooms();
            }
        }

        // Обробка подій виходу та руйнування кімнат (виправлено зайві пробіли в рядках)
        if (event === "left") {
            Logger.log(`👋 Хтось вийшов з кімнати`, "INFO");
            UIRenderer.loadRooms();
        }
        if (event === "kicked") {
            Logger.log(`️ Вас видалено з кімнати`, "WARN");
        }
        if (event === "destroyed") {
            this.currentRoomId = null;
            Logger.log(`💥 Кімнату знищено`, "WARN");
            if (typeof NetworkMonitor !== 'undefined') {
                NetworkMonitor.stopIceMonitoring();
            }
        }

        // Якщо в повідомленні присутній SDP об'єкт (JSEP), обробляємо його
        if (jsep) {
            this.handleJsep(jsep);
        }
    },

    /**
     🎤 Створення локального WebRTC Offer для публікації аудіопотоку з мікрофона.
     ВИПРАВЛЕНО: Вимкнено AGC/NS/EC для якісного звуку на мобільних пристроях.
     */
    createOffer() {
        Logger.log("🎤 Створюємо WebRTC Offer (мікрофон)...", "DEBUG");
        const offerStartTime = Date.now();

        JanusConnection.audioBridge.createOffer({
            media: {
                audioSend: {
                    echoCancellation: false,  // Вимкнути придушення луни (важливо для мобільних!)
                    noiseSuppression: false,  // Вимкнути придушення шумів
                    autoGainControl: false    // Вимкнути авто-гучність
                },
                audioRecv: true,
                video: false
            },
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

    /**
     Обробка обміну SDP (JSEP Handshake).
     🎯 ВИПРАВЛЕНО: Синтаксис стрілкових функцій () => та зайві пробіли в типах.
     */
    handleJsep(jsep) {
        Logger.log(` Отримано JSEP типу: ${jsep.type}`, "DEBUG");

        if (jsep.type === "answer") {
            // Отримано відповідь SDP від Janus — завершуємо WebRTC Handshake
            JanusConnection.audioBridge.handleRemoteJsep({
                jsep: jsep,
                success: () => {
                    Logger.log("✅ WebRTC Handshake завершено", "SUCCESS");
                    // Починаємо стежити за процесом з'єднання ICE
                    this.monitorIceState();
                },
                error: (err) => Logger.log(`❌ Answer Error: ${JSON.stringify(err)}`, "ERROR")
            });
        } else if (jsep.type === "offer") {
            // Якщо Janus надсилає Offer, автоматично створюємо Answer у відповідь
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

    /**
     Моніторинг статусу ICE Connection.
     ВИПРАВЛЕНО: Замінено оператор ?. на стандартну перевірку для уникнення "Unexpected token '.'".
     */
    monitorIceState() {
        let checkCount = 0;
        const checkInterval = setInterval(() => {
            // Безпечна перевірка без використання ?.
            const bridge = JanusConnection.audioBridge;
            if (bridge && bridge.webrtcStuff && bridge.webrtcStuff.pc) {
                const pc = bridge.webrtcStuff.pc;
                Logger.log(` ICE: ${pc.iceConnectionState} (${++checkCount})`, "DEBUG");

                if (pc.iceConnectionState === "connected" || pc.iceConnectionState === "completed") {
                    Logger.log("✅ ICE з'єднання встановлено!", "SUCCESS");
                    clearInterval(checkInterval);

                    // 🎯 Запускаємо постійний ICE моніторинг для виявлення втрати зв'язку
                    if (typeof NetworkMonitor !== 'undefined') {
                        NetworkMonitor.startIceMonitoring();
                    }
                } else if (pc.iceConnectionState === "failed") {
                    Logger.log("❌ ICE FAILED!", "ERROR");
                    clearInterval(checkInterval);
                }
            }

            // Обмежуємо тривалість перевірки 30-ма секундами (15 перевірок по 2 сек)
            if (checkCount >= 15) {
                clearInterval(checkInterval);
            }
        }, 2000);
    }
};