/**
 📊 Модуль AudioMonitor (v5 - фінальний).
 Єдиний модуль моніторингу: ЕФІР (віддалений трек Janus) + МІКРОФОН (локальний трек Janus).
 Обидва індикатори аналізують MediaStreamTrack напряму (createMediaStreamSource).
 НЕ використовується createMediaElementSource — немає конфліктів маршрутизації <audio>.
 Правила: один AudioContext на сторінку, ніколи не закривається.
 */
const AudioMonitor = {
    ctx: null,
    remoteAnalyser: null,
    remoteSource: null,
    attachedRemoteTrack: null,
    micAnalyser: null,
    micSource: null,
    attachedMicTrack: null,
    loopId: null,
    lastDebugLog: 0,

    _ensureContext() {
        if (!this.ctx) {
            const AC = window.AudioContext || window.webkitAudioContext;
            this.ctx = new AC();
            Logger.log(`🎛️ AudioContext створено (стан: ${this.ctx.state})`, "DEBUG");
            const resume = () => {
                if (this.ctx && this.ctx.state === 'suspended') {
                    this.ctx.resume().catch(() => {});
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
    // 📻 Моніторинг ЕФІРУ: аналіз віддаленого треку Janus напряму
    // ---------------------------------------------------------------
    attachRemoteTrack(track) {
        if (!track || track.kind !== 'audio') return;
        if (this.attachedRemoteTrack === track) return;
        try {
            const ctx = this._ensureContext();
            if (!this.remoteAnalyser) {
                this.remoteAnalyser = ctx.createAnalyser();
                this.remoteAnalyser.fftSize = 256;
                this.remoteAnalyser.smoothingTimeConstant = 0.6;
            }
            if (this.remoteSource) {
                try { this.remoteSource.disconnect(); } catch (e) {}
            }
            this.remoteSource = ctx.createMediaStreamSource(new MediaStream([track]));
            this.remoteSource.connect(this.remoteAnalyser);
            // ❗ НЕ підключаємо до destination — звук і так грає через <audio> елемент
            this.attachedRemoteTrack = track;
            this._ensureLoop();
            Logger.log(`📊 Моніторинг ЕФІРУ підключено (трек: ${track.readyState})`, "SUCCESS");
        } catch (e) {
            Logger.log(`❌ attachRemoteTrack: ${e.message}`, "ERROR");
        }
    },

    // ---------------------------------------------------------------
    // 🎤 Моніторинг МІКРОФОНА: аналіз локального треку Janus
    // ---------------------------------------------------------------
    attachLocalTrack(track) {
        if (!track || track.kind !== 'audio' || track.readyState !== 'live') return;
        if (this.attachedMicTrack === track) return;
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
            this.attachedMicTrack = track;
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

        const freq = new Uint8Array(128);
        const time = new Uint8Array(1024);

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

            const now = Date.now();
            if (now - this.lastDebugLog > 5000) {
                this.lastDebugLog = now;
                Logger.log(`📊 [MON] ефір=${remotePct}% мікрофон=${micPct}% ctx=${this.ctx ? this.ctx.state : '-'}`, "DEBUG");
            }
        };

        this.loopId = requestAnimationFrame(tick);
    },

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
    // ⚠️ Застарілі виклики: голосно сигналізують про невідповідність файлів
    // ---------------------------------------------------------------
    init(el) {
        Logger.log("⚠️ AudioMonitor.init() застаріло! Оновіть janus-connection.js до v5 (attachRemoteTrack)!", "WARN");
    },
    attachRemote(el) {
        Logger.log("⚠️ AudioMonitor.attachRemote() застаріло! Оновіть janus-connection.js до v5 (attachRemoteTrack)!", "WARN");
    },
    stop() {
        this._paint('audio-visualizer', 'audio-level-text', 0, 'remote');
        this._paint('mic-visualizer', 'mic-level-text', 0, 'mic');
    }

    setVolume(gain) {
        if (this.remoteGain) {
            this.remoteGain.gain.value = gain;
            Logger.log(`🔊 Гучність ефіру: ${Math.round(gain * 100)}%`, "INFO");
        }
    }
};