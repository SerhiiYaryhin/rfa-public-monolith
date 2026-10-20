/**
 📊 Модуль AudioMonitor (v3 - фінальний).
 Єдиний модуль моніторингу аудіо: ефір (віддалений мікс) + мікрофон (локальний трек).

 КЛЮЧОВІ ПРАВИЛА (не порушувати!):
 1. AudioContext створюється ОДИН РАЗ і НІКОЛИ не закривається.
 2. createMediaElementSource() викликається ОДИН РАЗ на елемент (обмеження браузера),
 результат кешується у audioElement.__monitorSource.
 3. При зміні кімнати/треку нічого не перестворюється: джерело прив'язане до
 HTML-елемента і автоматично слідує за новим srcObject.
 */
const AudioMonitor = {
    ctx: null,
    remoteAnalyser: null,
    micAnalyser: null,
    micSource: null,
    attachedTrack: null,
    loopId: null,
    lastDebugLog: 0,

    // ---------------------------------------------------------------
    // Створення/отримання єдиного AudioContext + розблокування на мобільних
    // ---------------------------------------------------------------
    _ensureContext() {
        if (!this.ctx) {
            const AC = window.AudioContext || window.webkitAudioContext;
            this.ctx = new AC();
            Logger.log(`🎛️ AudioContext створено (стан: ${this.ctx.state})`, "DEBUG");

            // Мобільні браузери тримають контекст у 'suspended' до першої взаємодії
            const resume = () => {
                if (this.ctx && this.ctx.state === 'suspended') {
                    this.ctx.resume()
                        .then(() => Logger.log("▶️ AudioContext розблоковано після взаємодії", "INFO"))
                        .catch(() => {});
                }
            };
            document.addEventListener('click', resume);
            document.addEventListener('keydown', resume);
            document.addEventListener('touchend', resume);
        }
        if (this.ctx.state === 'suspended') {
            this.ctx.resume().catch(() => {});
        }
        return this.ctx;
    },

    // ---------------------------------------------------------------
    // 📻 Моніторинг ЕФІРУ: прив'язка до <audio> елемента
    // ---------------------------------------------------------------
    attachRemote(audioElement) {
        if (!audioElement) return;
        try {
            const ctx = this._ensureContext();

            // ДЖЕРЕЛО СТВОРЮЄТЬСЯ ЛИШЕ ОДИН РАЗ на елемент!
            if (!audioElement.__monitorSource) {
                audioElement.__monitorSource = ctx.createMediaElementSource(audioElement);
                Logger.log(`🎛️ Створено MediaElementSource для <${audioElement.id}>`, "DEBUG");
            }

            if (!this.remoteAnalyser) {
                this.remoteAnalyser = ctx.createAnalyser();
                this.remoteAnalyser.fftSize = 256;
                this.remoteAnalyser.smoothingTimeConstant = 0.6;
            }

            // Перепідключення безпечне і може викликатись багато разів
            try { audioElement.__monitorSource.disconnect(); } catch (e) {}
            try { this.remoteAnalyser.disconnect(); } catch (e) {}
            audioElement.__monitorSource.connect(this.remoteAnalyser);
            this.remoteAnalyser.connect(ctx.destination); // звук іде в динаміки

            this._ensureLoop();
            Logger.log("📊 Моніторинг ЕФІРУ підключено", "SUCCESS");
        } catch (e) {
            Logger.log(`❌ attachRemote: ${e.message}`, "ERROR");
        }
    },

    // ---------------------------------------------------------------
    // 🎤 Моніторинг МІКРОФОНА: прив'язка до локального треку Janus
    // ---------------------------------------------------------------
    attachLocalTrack(track) {
        if (!track || track.kind !== 'audio' || track.readyState !== 'live') return;
        if (this.attachedTrack === track) return;
        try {
            const ctx = this._ensureContext();

            if (!this.micAnalyser) {
                this.micAnalyser = ctx.createAnalyser();
                this.micAnalyser.fftSize = 1024;
                this.micAnalyser.smoothingTimeConstant = 0.6;
            }

            if (this.micSource) {
                try { this.micSource.disconnect(); } catch (e) {}
            }
            this.micSource = ctx.createMediaStreamSource(new MediaStream([track]));
            this.micSource.connect(this.micAnalyser);
            // ❗ НЕ підключаємо до destination — інакше буде луна!

            this.attachedTrack = track;
            this._ensureLoop();
            Logger.log("🎤 Моніторинг МІКРОФОНА підключено", "SUCCESS");
        } catch (e) {
            Logger.log(`❌ attachLocalTrack: ${e.message}`, "ERROR");
        }
    },

    // ---------------------------------------------------------------
    // 🔄 Єдиний цикл вимірювання для обох індикаторів
    // ---------------------------------------------------------------
    _ensureLoop() {
        if (this.loopId) return;

        const freq = new Uint8Array(128);   // fftSize 256 / 2
        const time = new Uint8Array(1024);  // fftSize 1024

        const tick = () => {
            this.loopId = requestAnimationFrame(tick);

            let remotePct = 0;
            let micPct = 0;

            if (this.remoteAnalyser) {
                this.remoteAnalyser.getByteFrequencyData(freq);
                let sum = 0;
                for (let i = 0; i < freq.length; i++) sum += freq[i];
                remotePct = Math.min(100, Math.round((sum / freq.length) / 255 * 100 * 1.8));
            }

            if (this.micAnalyser) {
                this.micAnalyser.getByteTimeDomainData(time);
                let sq = 0;
                for (let i = 0; i < time.length; i++) {
                    const v = (time[i] - 128) / 128;
                    sq += v * v;
                }
                const rms = Math.sqrt(sq / time.length);
                micPct = Math.min(100, Math.round(rms * 300));
            }

            this._paint('audio-visualizer', 'audio-level-text', remotePct, 'remote');
            this._paint('mic-visualizer', 'mic-level-text', micPct, 'mic');

            // Діагностичний лог кожні 5 секунд (щоб бачити реальні значення у консолі)
            const now = Date.now();
            if (now - this.lastDebugLog > 5000) {
                this.lastDebugLog = now;
                Logger.log(`📊 [MON] ефір=${remotePct}% мікрофон=${micPct}% ctx=${this.ctx ? this.ctx.state : '-'}`, "DEBUG");
            }
        };

        this.loopId = requestAnimationFrame(tick);
    },

    // ---------------------------------------------------------------
    // 🎨 Малювання смужки
    // ---------------------------------------------------------------
    _paint(barId, textId, pct, scheme) {
        const bar = document.getElementById(barId);
        const txt = document.getElementById(textId);
        if (!bar || !txt) return;

        bar.style.width = pct + '%';
        txt.textContent = pct + '%';

        if (scheme === 'remote') {
            if (pct < 10) bar.style.background = '#e74c3c';
            else if (pct < 40) bar.style.background = '#f1c40f';
            else bar.style.background = '#27ae60';
        } else {
            if (pct < 5) bar.style.background = '#95a5a6';
            else if (pct < 30) bar.style.background = '#3498db';
            else if (pct < 60) bar.style.background = '#27ae60';
            else bar.style.background = '#e74c3c';
        }
    },

    // ---------------------------------------------------------------
    // Сумісність зі старими викликами (stop/init більше НЕ вбивають монітор)
    // ---------------------------------------------------------------
    init(el) { this.attachRemote(el); },
    stop() {
        // НІЧОГО не закриваємо! Лише обнуляємо смужки.
        this._paint('audio-visualizer', 'audio-level-text', 0, 'remote');
        this._paint('mic-visualizer', 'mic-level-text', 0, 'mic');
    }
};