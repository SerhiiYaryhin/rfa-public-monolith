import os
import subprocess
import srt
import tempfile
from pydub import AudioSegment
from TTS.api import TTS
import argparse

VIDEO_FILE = "your_video.mp4"
EN_SRT = "english.srt"
UK_SRT = "ukrainian.srt"
AUDIO_FOLDER = "tts_parts"
FINAL_AUDIO = "final_audio.wav"
FINAL_VIDEO = "final_output.mp4"


def transcribe_video():
    print(">> 🎙️ Транскрипція відео...")
    subprocess.run([
        "whisper", VIDEO_FILE,
        "--language", "English",
        "--model", "medium",
        "--output_format", "srt"
    ])
    os.rename(VIDEO_FILE.replace(".mp4", ".srt"), EN_SRT)


def translate_srt_with_ollama():
    print(">> 🌐 Переклад субтитрів через Ollama...")
    with open(EN_SRT, "r", encoding="utf-8") as f:
        srt_text = f.read()

    prompt = (
            "Переклади наступні субтитри з англійської на українську, зберігаючи формат .srt:\n" + srt_text
    )

    result = subprocess.run(["ollama", "run", "llama3"], input=prompt, text=True, capture_output=True)
    translated_srt = result.stdout

    with open(UK_SRT, "w", encoding="utf-8") as f:
        f.write(translated_srt)


def synthesize_segmented_speech(voice_gender="female", speed=1.0, gain_db=0.0, model_name=None):
    print(">> 🗣️ Генерація озвучення з таймінгами...")

    if not os.path.exists(AUDIO_FOLDER):
        os.makedirs(AUDIO_FOLDER)

    with open(UK_SRT, "r", encoding="utf-8") as f:
        subs = list(srt.parse(f.read()))

    tts = TTS(model_name=model_name or "tts_models/multilingual/multi-dataset/your_model", progress_bar=False)
    selected_speaker = None

    if tts.speakers:
        print(f">> Доступні голоси: {tts.speakers}")
        for s in tts.speakers:
            if voice_gender.lower() in s.lower():
                selected_speaker = s
                break
        print(f">> Обрано голос: {selected_speaker}")
    else:
        print(">> ⚠️ Модель не підтримує мульти-голоси — буде дефолтний голос.")

    for i, sub in enumerate(subs):
        text = sub.content.strip()
        duration = sub.end.total_seconds() - sub.start.total_seconds()
        filename = os.path.join(AUDIO_FOLDER, f"part_{i:04d}.wav")
        temp_path = os.path.join(AUDIO_FOLDER, f"temp_{i:04d}.wav")

        tts.tts_to_file(
            text=text,
            speaker=selected_speaker if selected_speaker else None,
            file_path=temp_path
        )

        segment = AudioSegment.from_wav(temp_path)

        if speed != 1.0:
            segment = segment._spawn(segment.raw_data, overrides={
                "frame_rate": int(segment.frame_rate * speed)
            }).set_frame_rate(segment.frame_rate)

        target_duration_ms = duration * 1000
        diff = target_duration_ms - len(segment)

        if diff > 0:
            segment += AudioSegment.silent(duration=diff)
        elif diff < 0:
            segment = segment[:target_duration_ms]

        segment += gain_db
        segment.export(filename, format="wav")
        os.remove(temp_path)

    print(">> 🔊 Об'єднання фрагментів в один аудіофайл...")

    combined = AudioSegment.silent(duration=0)
    for i, sub in enumerate(subs):
        start_ms = sub.start.total_seconds() * 1000
        current_len = len(combined)
        if start_ms > current_len:
            combined += AudioSegment.silent(duration=start_ms - current_len)

        part_path = os.path.join(AUDIO_FOLDER, f"part_{i:04d}.wav")
        voice = AudioSegment.from_wav(part_path)
        combined += voice

    combined.export(FINAL_AUDIO, format="wav")


def combine_video_audio_subs():
    print(">> 🎬 Компіляція фінального відео...")
    temp_video = "temp_video.mp4"

    subprocess.run([
        "ffmpeg", "-y", "-i", VIDEO_FILE, "-i", FINAL_AUDIO,
        "-c:v", "copy", "-map", "0:v:0", "-map", "1:a:0", "-shortest", temp_video
    ])

    subprocess.run([
        "ffmpeg", "-y", "-i", temp_video, "-vf", f"subtitles={UK_SRT}", FINAL_VIDEO
    ])
    os.remove(temp_video)


def main():
    parser = argparse.ArgumentParser(description="🎥 Відео → Субтитри → Переклад → Озвучка → Відео")
    parser.add_argument("--video", type=str, default="your_video.mp4", help="Назва відеофайлу")
    parser.add_argument("--speed", type=float, default=1.0, help="Швидкість голосу (1.0 = нормально)")
    parser.add_argument("--gain", type=float, default=0.0, help="Гучність у dB (0 = як є)")
    parser.add_argument("--gender", type=str, default="female", help="Голос: female або male")
    parser.add_argument("--model", type=str, default="tts_models/multilingual/multi-dataset/your_model", help="Назва TTS-моделі")
    args = parser.parse_args()

    global VIDEO_FILE
    VIDEO_FILE = args.video

    transcribe_video()
    translate_srt_with_ollama()
    synthesize_segmented_speech(
        voice_gender=args.gender,
        speed=args.speed,
        gain_db=args.gain,
        model_name=args.model
    )
    combine_video_audio_subs()

    print(f"\n✅ Готово! Результат у файлі: {FINAL_VIDEO}")


if __name__ == "__main__":
    main()
