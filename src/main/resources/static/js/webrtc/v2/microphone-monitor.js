/**
 🎤 Модуль MicrophoneMonitor.
 Відповідає за візуалізацію рівня гучності локального мікрофона користувача.
 Отримує власний потік через getUserMedia тільки для аналізу (не відправляється в Janus).
 */
const MicrophoneMonitor = {
    audioContext: null,
    analyser: null,
    source: null,
    stream: null,
    animationId: null,
    isInitialized: false,

    /**
     🎛️ Ініціалізація моніторингу мікрофона.
     Отримує доступ до мікрофона через getUserMedia.
     */
    async init() {
        if (this.isInitialized) {
            Logger.log("⚠️ MicrophoneMonitor вже ініціалізовано", "WARN");
            return;
        }

        try {
            // Запитуємо доступ до мікрофона
            Logger.log("🎤 Запит доступу до мікрофона для моніторингу...", "INFO");
            this.stream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true
                }
            });

            // Створюємо AudioContext
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            this.audioContext = new AudioContext();

            // Розблокування для мобільних
            if (this.audioContext.state === 'suspended') {
                this.audioContext.resume().catch(e => console.warn("AudioContext resume failed:", e));
            }

            // Створюємо джерело з мікрофонного потоку
            this.source = this.audioContext.createMediaStreamSource(this.stream);

            // Створюємо аналізатор
            this.analyser = this.audioContext.createAnalyser();
            this.analyser.fftSize = 256;
            this.analyser.smoothingTimeConstant = 0.8; // Згладжування для плавнішої анімації

            // З'єднуємо: Мікрофон -> Аналізатор (НЕ підключаємо до destination, щоб не було луни!)
            this.source.connect(this.analyser);
            // ❗ ВАЖЛИВО: НЕ підключаємо до audioContext.destination, інакше буде луна!

            this.isInitialized = true;
            Logger.log("🎤 Моніторинг мікрофона активовано", "SUCCESS");

            this.startLoop();

        } catch (e) {
            Logger.log(`❌ Помилка ініціалізації MicrophoneMonitor: ${e.message}`, "ERROR");
            if (e.name === 'NotAllowedError') {
                Logger.log("⚠️ Доступ до мікрофона заборонено користувачем", "WARN");
            }
        }
    },

    /**
     🔄 Головний цикл аналізу мікрофона.
     */
    startLoop() {
        if (!this.analyser) return;

        const bufferLength = this.analyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);

        const update = () => {
            if (!this.analyser) return;

            // Отримуємо часові дані (waveform) для мікрофона — це точніше для голосу
            this.analyser.getByteTimeDomainData(dataArray);

            // Обчислюємо RMS (середньоквадратичне значення) для більш точного вимірювання гучності
            let sumSquares = 0;
            for (let i = 0; i < bufferLength; i++) {
                const value = (dataArray[i] - 128) / 128; // Нормалізуємо від -1 до 1
                sumSquares += value * value;
            }
            const rms = Math.sqrt(sumSquares / bufferLength);

            // Масштабуємо RMS у відсотки (RMS зазвичай 0-0.5 для нормальної мови)
            const percentage = Math.min(100, Math.round(rms * 200 * 1.5));

            this.updateUI(percentage);
            this.animationId = requestAnimationFrame(update);
        };

        this.animationId = requestAnimationFrame(update);
    },

    /**
     🎨 Оновлення DOM-елементів візуалізатора мікрофона.
     */
    updateUI(percentage) {
        const visualizer = document.getElementById('mic-visualizer');
        const levelText = document.getElementById('mic-level-text');

        if (visualizer && levelText) {
            visualizer.style.width = `${percentage}%`;
            levelText.textContent = `${percentage}%`;

            // Динамічна зміна кольору
            if (percentage < 5) {
                visualizer.style.background = '#95a5a6'; // Сірий (тиша)
            } else if (percentage < 30) {
                visualizer.style.background = '#3498db'; // Синій (тихий голос)
            } else if (percentage < 60) {
                visualizer.style.background = '#27ae60'; // Зелений (нормальний голос)
            } else {
                visualizer.style.background = '#e74c3c'; // Червоний (гучний голос)
            }
        }
    },

    /**
     🛑 Зупинка моніторингу та звільнення мікрофона.
     */
    stop() {
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
            this.animationId = null;
        }

        if (this.stream) {
            this.stream.getTracks().forEach(track => track.stop());
            this.stream = null;
        }

        if (this.audioContext && this.audioContext.state !== 'closed') {
            this.audioContext.close();
        }

        this.isInitialized = false;
        Logger.log("🛑 Моніторинг мікрофона зупинено", "INFO");
    }
};