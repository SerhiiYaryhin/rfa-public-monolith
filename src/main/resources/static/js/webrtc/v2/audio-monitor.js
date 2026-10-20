/**
 📊 Модуль AudioMonitor.
 Відповідає за візуалізацію рівня гучності аудіопотоку за допомогою Web Audio API.
 Аналізує HTML <audio> елемент і оновлює відповідні DOM-елементи інтерфейсу.
 Моніторинг працює тільки для поточної кімнати, в якій знаходиться користувач.
 */
const AudioMonitor = {
    // Внутрішній стан модуля
    audioContext: null,
    analyser: null,
    source: null,
    animationId: null,
    isInitialized: false,

    /**
     🎛️ Ініціалізація аналізатора для конкретного <audio> елемента.
     @param {HTMLAudioElement} audioElement - Елемент, з якого береться звук.
     */
    init(audioElement) {
        Logger.log("🎛️ AudioMonitor.init() викликано", "DEBUG");

        if (this.isInitialized) {
            Logger.log("⚠️ AudioMonitor вже ініціалізовано", "WARN");
            return;
        }

        if (!audioElement) {
            Logger.log("❌ audioElement не передано", "ERROR");
            return;
        }

        Logger.log(`🎛️ Audio element: ${audioElement.id}, srcObject: ${!!audioElement.srcObject}`, "DEBUG");

        try {
            // Ініціалізуємо AudioContext (з підтримкою старих браузерів)
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            this.audioContext = new AudioContext();

            // 🎯 КРИТИЧНО ДЛЯ МОБІЛЬНИХ: Перевіряємо стан AudioContext
            Logger.log(`🎛️ AudioContext стан: ${this.audioContext.state}`, "DEBUG");

            if (this.audioContext.state === 'suspended') {
                Logger.log("️ AudioContext призупинено, намагаємося відновити...", "WARN");
                this.audioContext.resume().then(() => {
                    Logger.log("✅ AudioContext відновлено", "SUCCESS");
                }).catch(e => {
                    Logger.log(`❌ Не вдалося відновити AudioContext: ${e.message}`, "ERROR");
                });
            }

            // Створюємо джерело з існуючого HTML <audio> елемента
            Logger.log("️ Створюємо MediaElementSource...", "DEBUG");
            this.source = this.audioContext.createMediaElementSource(audioElement);

            // Створюємо аналізатор
            this.analyser = this.audioContext.createAnalyser();
            this.analyser.fftSize = 256;
            Logger.log(`🎛️ Analyser створено, fftSize: ${this.analyser.fftSize}`, "DEBUG");

            // З'єднуємо ланцюжок: Джерело -> Аналізатор -> Вихід (динаміки)
            this.source.connect(this.analyser);
            this.analyser.connect(this.audioContext.destination);
            Logger.log("🎛️ Ланцюжок з'єднано", "DEBUG");

            this.isInitialized = true;
            Logger.log("📊 Моніторинг аудіо сигналу активовано", "SUCCESS");

            // Запускаємо цикл оновлення
            this.startLoop();
        } catch (e) {
            Logger.log(`❌ Помилка ініціалізації AudioMonitor: ${e.message}`, "ERROR");
            Logger.log(`❌ Stack: ${e.stack}`, "ERROR");
        }
    },

    /**
     🔄 Головний цикл аналізу частот (викликається ~60 разів на секунду).
     */
    startLoop() {
        if (!this.analyser) {
            Logger.log("❌ Analyser відсутній в startLoop()", "ERROR");
            return;
        }

        const bufferLength = this.analyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);
        Logger.log(`🔄 startLoop запущено, bufferLength: ${bufferLength}`, "DEBUG");

        const update = () => {
            if (!this.analyser) return;

            // Отримуємо частотні дані (значення від 0 до 255)
            this.analyser.getByteFrequencyData(dataArray);

            // Обчислюємо середній рівень гучності
            let sum = 0;
            for (let i = 0; i < bufferLength; i++) {
                sum += dataArray[i];
            }
            const average = sum / bufferLength;

            // Масштабуємо 0-255 у відсотки (0-100%).
            // Множник 1.5 додається для кращої чутливості до тихих звуків
            const percentage = Math.min(100, Math.round((average / 255) * 100 * 1.5));

            // Оновлюємо інтерфейс
            this.updateUI(percentage);

            // Запускаємо наступний кадр
            this.animationId = requestAnimationFrame(update);
        };

        this.animationId = requestAnimationFrame(update);
    },

    /**
     🎨 Оновлення DOM-елементів візуалізатора.
     @param {number} percentage Рівень гучності від 0 до 100
     */
    updateUI(percentage) {
        const visualizer = document.getElementById('audio-visualizer');
        const levelText = document.getElementById('audio-level-text');

        if (!visualizer || !levelText) {
            // Логуємо тільки перший раз, щоб не засмічувати консоль
            if (!this._domWarningShown) {
                Logger.log(` DOM елементи не знайдено: visualizer=${!!visualizer}, levelText=${!!levelText}`, "ERROR");
                this._domWarningShown = true;
            }
            return;
        }

        visualizer.style.width = `${percentage}%`;
        levelText.textContent = `${percentage}%`;

        // Динамічна зміна кольору залежно від гучності
        if (percentage < 10) {
            visualizer.style.background = '#e74c3c'; // Червоний (тиша)
        } else if (percentage < 40) {
            visualizer.style.background = '#f1c40f'; // Жовтий (нормальний рівень)
        } else {
            visualizer.style.background = '#27ae60'; // Зелений (гучний сигнал)
        }
    },

    /**
     Зупинка моніторингу та очищення ресурсів.
     */
    stop() {
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
            this.animationId = null;
        }
        if (this.audioContext && this.audioContext.state !== 'closed') {
            this.audioContext.close();
        }
        this.isInitialized = false;
        Logger.log("🛑 Моніторинг аудіо сигналу зупинено", "INFO");
    }
};