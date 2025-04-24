# PipeLine створення субтитрів і україномовної озвучки файлу з відео
#
#
#

import os
import subprocess
import srt
import pysrt
import tempfile
from pydub import AudioSegment
#from TTS.api import TTS
import nltk
from ukrainian_tts.tts import TTS, Voices, Stress
import argparse

VIDEO_FILE = "foxnewsexsample.mp4"
EN_SRT = "english.srt"
UK_SRT = "ukrainian.srt"
AUDIO_FOLDER = "tts_parts"
AUDIO_FOLDER_SPEED = "tts_speed"
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

def translate_srt_with_ollama(chunk_set):
#    print(">> 🌐 Переклад субтитрів через Ollama...")
#    with open(EN_SRT, "r", encoding="utf-8") as f:
#        srt_text = f.read()
    prompt = (
            "Переклади субтитри з англійської на українську повністю без стилістичних та структурних змін, зберігаючи структуру субтитрів формату srt:\n"
#            
#            "Переклади наступні субтитри з англійської на українську повністю зберігаючи формат .srt:\n" + srt_text
    )
    for lchunk in chunk_set:
        prompt = prompt + str(lchunk.index) + "\n"
        prompt = prompt + str(lchunk.start) + " --> "+ str(lchunk.end) +"\n"
        prompt = prompt + lchunk.text + "\n\n"
    print(prompt)
    #result = subprocess.run(["ollama", "run", "mistral"], input=prompt, text=True, capture_output=True)
    #result = subprocess.run(["ollama", "run", "llama3"], input=prompt, text=True, capture_output=True)
    result = subprocess.run(["ollama", "run", "phi4"], input=prompt, text=True, capture_output=True)
    translated_srt = result.stdout

    with open(str(chunk_set[0].index).zfill(5) +"-" + UK_SRT, "w", encoding="utf-8") as f:
        f.write(translated_srt)
    return translated_srt
#==============================================================
def is_end_of_sentence(text):
    return text.strip()[-1:] in '.!?'

def split_subtitles(subs, min_len=7, max_len=12):
    chunks = []
    chunk = []

    i = 0
    while i < len(subs):
        chunk_start = i
        chunk = []
        while i < len(subs) and len(chunk) < max_len:
            chunk.append(subs[i])
            i += 1
            if len(chunk) >= min_len and is_end_of_sentence(chunk[-1].text):
                break

        if i == len(subs) and len(chunk) < min_len and chunks:
            chunks[-1].extend(chunk)
        else:
            chunks.append(chunk)

    return [pysrt.SubRipFile(items=chunk) for chunk in chunks]

def splitSRT():
    # Завантаження субтитрів
    subs = pysrt.open('english.srt', encoding='utf-8')

    # Отримання фрагментів у пам'яті
    chunks_in_memory = split_subtitles(subs)
    out_translate_subtitle = ""
    for idx, chunk in enumerate(chunks_in_memory, 1):
        out_translate_subtitle = out_translate_subtitle + translate_srt_with_ollama(chunk)
    with open(UK_SRT, "w", encoding="utf-8") as f:
        f.write(out_translate_subtitle)



def synthesize_segmented_speech(voice_gender="female", speed=1.0, gain_db=0.0, model_name=None):
    print(">> 🗣️ Генерація озвучення з таймінгами...")


    with open(UK_SRT, "r", encoding="utf-8") as f:
        subs = list(srt.parse(f.read()))

    nltk.download("punkt")
    from nltk.tokenize import sent_tokenize
    tts = TTS(device="cpu")

    if not os.path.exists(AUDIO_FOLDER):
        os.makedirs(AUDIO_FOLDER)
    if not os.path.exists(AUDIO_FOLDER_SPEED):
        os.makedirs(AUDIO_FOLDER_SPEED)

    for i, sub in enumerate(subs):
        text = sub.content.strip()
        duration = sub.end.total_seconds() - sub.start.total_seconds()
        filename = os.path.join(AUDIO_FOLDER, f"part_{i:04d}.wav")
        temp_path = os.path.join(AUDIO_FOLDER, f"temp_{i:04d}.wav")

        with open(temp_path, mode="wb") as file:
            _, output_text = tts.tts(text, Voices.Oleksa.value, Stress.Dictionary.value, file)
        segment = AudioSegment.from_wav(temp_path)
        segment.export(filename, format="wav")

# записали відповідні сегменти в temp_path
def prepared_speach(voice_gender="female", speed=1.0, gain_db=0.0, model_name=None):
    if not os.path.exists(AUDIO_FOLDER):
        os.makedirs(AUDIO_FOLDER)
    if not os.path.exists(AUDIO_FOLDER_SPEED):
        os.makedirs(AUDIO_FOLDER_SPEED)

    with open(UK_SRT, "r", encoding="utf-8") as f:
        subs = list(srt.parse(f.read()))

    for i, sub in enumerate(subs):
        text = sub.content.strip()
        duration = sub.end.total_seconds() - sub.start.total_seconds()
        filename = os.path.join(AUDIO_FOLDER, f"part_{i:04d}.wav")
        # temp_path = os.path.join(AUDIO_FOLDER, f"temp_{i:04d}.wav")
        filename_speed = os.path.join(AUDIO_FOLDER_SPEED, f"part_{i:04d}.wav")
        temp_path_speed = os.path.join(AUDIO_FOLDER_SPEED, f"temp_{i:04d}.wav")

        subprocess.run([
            'ffmpeg',
            '-y',
            '-i', 
            filename,
            '-filter_complex', '[0:a]atempo=1.45[a]',
            '-map', '[a]',
            filename_speed
        ])

        segment = AudioSegment.from_wav(filename_speed)

        # Зміну швидкості зробив ffmpeg-ом
        if speed != 1.0:
            segment = segment._spawn(segment.raw_data, overrides={
                "frame_rate": int(segment.frame_rate * speed)
            }).set_frame_rate(segment.frame_rate)

        target_duration_ms = duration * 1000 # Тривалість за часом субтитру
        diff = target_duration_ms - len(segment) # Різниця між тривалістю субтитру та синтезованим фрагментом.

        if diff > 0:
            segment += AudioSegment.silent(duration=diff) # Додаємо тишу до довжини сегиента у випадку, коли синтезований коротше
        elif diff < 0:
            segment = segment[:target_duration_ms] # обрізаємо синтезований сегмент

        segment += gain_db
        segment.export(filename_speed, format="wav")
        # os.remove(temp_path)

    print(">> 🔊 Об'єднання фрагментів в один аудіофайл...")

    combined = AudioSegment.silent(duration=0) # combined - це результуючий файл, який наповнюємо сегментами
    for i, sub in enumerate(subs):
        start_ms = sub.start.total_seconds() * 1000
        current_len = len(combined)
        if start_ms > current_len:
            # якщо кінець результуючого файлу менше по часу ніж початок - додаємо тишу
            combined += AudioSegment.silent(duration=start_ms - current_len)
        # считуємо прискорений файл
        part_path = os.path.join(AUDIO_FOLDER_SPEED, f"part_{i:04d}.wav")
        voice = AudioSegment.from_wav(part_path)
        combined += voice
    # Записуємо фінальний файл
    combined.export(FINAL_AUDIO, format="wav")

def combine_video_audio_subs():
    print(">> 🎬 Компіляція фінального відео...")
    temp_video = "temp_video.mp4"
    temp_video_edge = "temp_videoi_edge.mp4"

    subprocess.run([
        "ffmpeg", "-y", "-i", VIDEO_FILE, "-i", FINAL_AUDIO,
        "-c:v", "copy", "-map", "0:v:0", "-map", "1:a:0", "-shortest", temp_video
    ])

#        "ffmpeg", "-y", "-i", temp_video,
    subprocess.run([
        "ffmpeg", "-y", "-i", temp_video,
        "-filter_complex", f"[0:v]edgedetect=low=0.1:high=0.4", temp_video_edge
    ])


    subprocess.run([
        "ffmpeg", "-y", "-i", temp_video_edge, "-vf", f"subtitles={UK_SRT}:force_style='Borderstyle=4,Fontsize=16,BackColour=&H00000000,PrimaryColour=&H0000FFFF'", FINAL_VIDEO
    ])
#    os.remove(temp_video)


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

#    transcribe_video()
#    splitSRT()
#
#    synthesize_segmented_speech(
#        voice_gender=args.gender,
#        speed=args.speed,
#        gain_db=args.gain,
#        model_name=args.model
#    )
    prepared_speach()
    combine_video_audio_subs()

    print(f"\n✅ Готово! Результат у файлі: {FINAL_VIDEO}")


if __name__ == "__main__":
    main()
