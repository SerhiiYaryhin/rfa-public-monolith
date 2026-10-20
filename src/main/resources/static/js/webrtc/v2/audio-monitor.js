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
        if (this.isInitialized || !audioElement) return;

        try {
            // Ініціалізуємо AudioContext (з підтримкою старих браузерів)
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            this.audioContext = new AudioContext();

            // 🎯 КРИТИЧНЕ ВИПРАВЛЕННЯ ДЛЯ МОБІЛЬНИХ БРАУЗЕРІВ:
            // Примусове відновлення AudioContext, якщо браузер його призупинив до взаємодії
            if (this.audioContext.state === 'suspended') {
                this.audioContext.resume().then(() => {
                    Logger.log("🔄 AudioContext відновлено (потрібна взаємодія зі сторінкою)", "INFO");
                }).catch(e => {
                    Logger.log(`⚠️ Не вдалося відновити AudioContext: ${e.message}`, "WARN");
                });
            }

            // Створюємо джерело з існуючого HTML <audio> елемента
            this.source = this.audioContext.createMediaElementSource(audioElement);

            // Створюємо аналізатор
            this.analyser = this.audioContext.createAnalyser();
            this.analyser.fftSize = 256; // Розмір вибірки (256 дає 128 частотних бінів)

            // З'єднуємо ланцюжок: Джерело -> Аналізатор -> Вихід (динаміки)
            this.source.connect(this.analyser);
            this.analyser.connect(this.audioContext.destination);

            this.isInitialized = true;
            Logger.log("📊 Моніторинг аудіо сигналу активовано", "INFO");

            // Запускаємо цикл оновлення
            this.startLoop();
        } catch (e) {
            Logger.log(`❌ Помилка ініціалізації AudioMonitor: ${e.message}`, "ERROR");
        }
    },

    /**
     🔄 Головний цикл аналізу частот (викликається ~60 разів на секунду).
     */
    startLoop() {
        if (!this.analyser) return;

        const bufferLength = this.analyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);

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

        if (visualizer && levelText) {
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
        }
    },

    /**
     🛑 Зупинка моніторингу та очищення ресурсів.
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