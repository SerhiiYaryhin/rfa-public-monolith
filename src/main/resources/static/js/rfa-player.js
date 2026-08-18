/**
 * ФАЙЛ: static/js/rfa-player.js
 * ОПИС: Клас для керування глобальним аудіоплеєром.
 * Ініціалізація: const player = new RFAudioPlayer();
 */
class RFAudioPlayer {
    constructor() {
        // 1. Отримуємо посилання на DOM-елементи за їх унікальними ID
        this.audio = document.getElementById('rfa-global-audio');
        this.playPauseBtn = document.getElementById('rfa-play-pause-btn');
        this.prevBtn = document.getElementById('rfa-prev-btn');
        this.nextBtn = document.getElementById('rfa-next-btn');
        this.titleEl = document.getElementById('rfa-current-track-title');
        this.authorEl = document.getElementById('rfa-current-track-author');
        this.progressBar = document.getElementById('rfa-progress-bar');
        this.timeDisplay = document.getElementById('rfa-time-display');
        this.volumeSlider = document.getElementById('rfa-volume-slider');
        this.volumeIcon = document.getElementById('rfa-volume-icon');

        // 2. Отримуємо всі елементи треків на сторінці (вони повинні мати клас .rfa-track-item та data-атрибути)
        this.trackItems = document.querySelectorAll('.rfa-track-item');

        // 3. Внутрішній стан (ізольований всередині класу)
        this.currentIndex = -1;
        this.isPlaying = false;
        this.isDragging = false;

        this.init();
    }

    init() {
        if (!this.audio) return; // Захист, якщо плеєра немає на сторінці

        // Прив'язка подій
        this.playPauseBtn.addEventListener('click', () => this.togglePlay());
        this.prevBtn.addEventListener('click', () => this.prev());
        this.nextBtn.addEventListener('click', () => this.next());
        this.audio.addEventListener('ended', () => this.next());
        this.audio.addEventListener('timeupdate', () => this.updateProgress());
        this.audio.addEventListener('loadedmetadata', () => this.updateTimeDisplay());

        this.progressBar.addEventListener('input', () => { this.isDragging = true; this.updateTimeDisplay(); });
        this.progressBar.addEventListener('change', () => {
            this.isDragging = false;
            this.audio.currentTime = (this.progressBar.value / 100) * this.audio.duration;
            if (this.audio.paused && this.currentIndex !== -1) this.play();
        });

        this.volumeSlider.addEventListener('input', () => {
            this.audio.volume = this.volumeSlider.value;
            this.updateVolumeIcon();
        });

        // Клік по треку в списку на сторінці
        this.trackItems.forEach((item, index) => {
            item.addEventListener('click', () => {
                if (this.currentIndex === index && !this.audio.paused) {
                    this.pause();
                } else {
                    this.loadTrack(index, true);
                }
            });
        });

        this.updateVolumeIcon();
    }

    // --- Основні методи ---

    loadTrack(index, autoPlay = true) {
        if (index < 0) index = this.trackItems.length - 1; // Зациклення
        if (index >= this.trackItems.length) index = 0;

        this.currentIndex = index;
        const item = this.trackItems[index];

        // Читаємо дані з data-атрибутів HTML
        this.audio.src = item.getAttribute('data-audio-src');
        this.titleEl.textContent = item.getAttribute('data-track-name') || 'Невідомо';
        this.authorEl.textContent = item.getAttribute('data-track-autor') || '---';

        // Оновлюємо візуальний стан списку
        this.trackItems.forEach(el => el.classList.remove('active'));
        item.classList.add('active');

        if (autoPlay) this.play();
    }

    play() {
        this.audio.play().then(() => {
            this.isPlaying = true;
            this.playPauseBtn.innerHTML = '<i class="bi bi-pause-fill"></i>';
        }).catch(e => console.error("Помилка відтворення:", e));
    }

    pause() {
        this.audio.pause();
        this.isPlaying = false;
        this.playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
    }

    togglePlay() {
        if (this.audio.paused) {
            if (this.currentIndex === -1 && this.trackItems.length > 0) this.loadTrack(0, true);
            else this.play();
        } else {
            this.pause();
        }
    }

    prev() {
        if (this.audio.currentTime > 3) {
            this.audio.currentTime = 0; // Повернення на початок, якщо грає > 3 сек
        } else {
            this.loadTrack(this.currentIndex - 1, true);
        }
    }

    next() {
        this.loadTrack(this.currentIndex + 1, true);
    }

    // --- Допоміжні методи ---

    updateProgress() {
        if (!this.isDragging && !isNaN(this.audio.duration)) {
            this.progressBar.value = (this.audio.currentTime / this.audio.duration) * 100;
            this.updateTimeDisplay();
        }
    }

    updateTimeDisplay() {
        const current = this.isDragging ? (this.progressBar.value / 100) * this.audio.duration : this.audio.currentTime;
        this.timeDisplay.textContent = `${this.formatTime(current)} / ${this.formatTime(this.audio.duration || 0)}`;
    }

    formatTime(seconds) {
        if (isNaN(seconds)) return "0:00";
        const m = Math.floor(seconds / 60);
        const s = Math.floor(seconds % 60);
        return `${m}:${s < 10 ? '0' : ''}${s}`;
    }

    updateVolumeIcon() {
        const v = this.audio.volume;
        if (v === 0) this.volumeIcon.className = 'bi bi-volume-mute-fill text-secondary';
        else if (v < 0.5) this.volumeIcon.className = 'bi bi-volume-down-fill text-secondary';
        else this.volumeIcon.className = 'bi bi-volume-up-fill text-secondary';
    }
}

// Автоматична ініціалізація, якщо DOM завантажений
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('rfa-global-player')) {
        window.rfaPlayer = new RFAudioPlayer(); // Зберігаємо в window для можливого доступу ззовні
    }
});