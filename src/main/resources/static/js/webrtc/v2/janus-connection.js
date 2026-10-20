/**
 🔊 Обробка віддаленого аудіо треку WebRTC (Mixed audio).
 @param {MediaStreamTrack} track Медіа трек від Janus
 @param {string} mid Ідентифікатор медіа-каналу
 @param {boolean} on Флаг підключення (true) чи відключення (false)
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

        // Спочатку зупиняємо старий цикл малювання (якщо він був)
        AudioMonitor.stop();

        audio.play().then(() => {
            Logger.log("✅ Аудіо відтворення розпочато успішно", "SUCCESS");
            // Ініціалізуємо моніторинг (тепер це безпечно навіть при повторних викликах)
            AudioMonitor.init(audio);
        }).catch(e => {
            Logger.log(`⚠️ Автопрогравання заблоковано: ${e.message}`, "WARN");
            AudioMonitor.init(audio);
        });
    } else if (!on) {
        // Якщо трек видалено, зупиняємо візуалізацію
        AudioMonitor.stop();
    }
}