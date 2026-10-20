/**
 📊 Модуль AudioMonitor.
 Відповідає за візуалізацію рівня гучності аудіопотоку за допомогою Web Audio API.
 Виправлено: безпечна повторна ініціалізація без помилки InvalidStateError.
 */
const AudioMonitor = {
    audioContext: null,
    analyser: null,
    source: null,
    animationId: null,
    isInitialized: false,
    targetElement: null, // Зберігаємо посилання на елемент, який моніторимо

    init(audioElement) {
        if (!audioElement) {
            Logger.log("❌ audioElement не передано", "ERROR");
            return;
        }

        // Якщо вже ініціалізовано і це той самий елемент, просто переконуємось, що цикл запущений
        if (this.isInitialized && this.targetElement === audioElement) {
            if (!this.animationId) {
                Logger.log("🔄 Відновлення циклу моніторингу", "DEBUG");
                this.startLoop();
            }
            return;
        }

        try {
            // 1. Створюємо або використовуємо існуючий AudioContext
            if (!this.audioContext) {
                const AudioContext = window.AudioContext || window.webkitAudioContext;
                this.audioContext = new AudioContext();
            }

            // Розблокування для мобільних браузерів
            if (this.audioContext.state === 'suspended') {
                this.audioContext.resume().catch(e => console.warn("AudioContext resume failed:", e));
            }

            // 2. Створюємо джерело ТІЛЬКИ якщо його ще немає або змінився елемент
            if (!this.source || this.targetElement !== audioElement) {
                try {
                    this.source = this.audioContext.createMediaElementSource(audioElement);
                } catch (e) {
                    // Якщо елемент вже підключено, ігноруємо помилку і використовуємо існуючу конфігурацію
                    Logger.log("⚠️ Елемент вже підключено, використовуємо існуючий ланцюжок", "WARN");
                }
            }

            // 3. Створюємо аналізатор (лише один раз)
            if (!this.analyser) {
                this.analyser = this.audioContext.createAnalyser();
                this.analyser.fftSize = 256;
            }

            // 4. Перепідключаємо ланцюжок (безпечно, з перевіркою)
            if (this.source && this.analyser) {
                try { this.source.disconnect(); } catch (e) {}
                try { this.analyser.disconnect(); } catch (e) {}

                this.source.connect(this.analyser);
                this.analyser.connect(this.audioContext.destination);
            }

            this.targetElement = audioElement;
            this.isInitialized = true;
            Logger.log("📊 Моніторинг аудіо сигналу активовано", "SUCCESS");

            this.startLoop();

        } catch (e) {
            Logger.log(`❌ Помилка ініціалізації AudioMonitor: ${e.message}`, "ERROR");
        }
    },

    startLoop() {
        if (!this.analyser) return;

        const bufferLength = this.analyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);

        const update = () => {
            if (!this.analyser) return;

            this.analyser.getByteFrequencyData(dataArray);

            let sum = 0;
            for (let i = 0; i < bufferLength; i++) {
                sum += dataArray[i];
            }
            const average = sum / bufferLength;
            const percentage = Math.min(100, Math.round((average / 255) * 100 * 1.5));

            this.updateUI(percentage);
            this.animationId = requestAnimationFrame(update);
        };

        this.animationId = requestAnimationFrame(update);
    },

    updateUI(percentage) {
        const visualizer = document.getElementById('audio-visualizer');
        const levelText = document.getElementById('audio-level-text');

        if (visualizer && levelText) {
            visualizer.style.width = `${percentage}%`;
            levelText.textContent = `${percentage}%`;

            if (percentage < 10) {
                visualizer.style.background = '#e74c3c';
            } else if (percentage < 40) {
                visualizer.style.background = '#f1c40f';
            } else {
                visualizer.style.background = '#27ae60';
            }
        }
    },

    stop() {
        // ❗ ВАЖЛИВО: Ми ТІЛЬКИ зупиняємо анімацію.
        // Ми НЕ знищуємо AudioContext і НЕ відключаємо source,
        // щоб уникнути InvalidStateError при наступному виклику init().
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
            this.animationId = null;
        }
        Logger.log("🛑 Моніторинг аудіо сигналу призупинено", "INFO");
    }
};