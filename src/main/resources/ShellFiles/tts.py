import sys
import nltk
import tempfile
from pydub import AudioSegment
from ukrainian_tts.tts import TTS, Voices, Stress

nltk.download("punkt")
from nltk.tokenize import sent_tokenize

tts = TTS(device="cpu")  # Можна змінити на "gpu" або "mps" для швидшої генерації

with open("/tmp/"+sys.argv[1]+".tts", "r", encoding="utf-8") as f:
    text = f.read()

sentences = sent_tokenize(text, language="russian")  # Для української використовуємо "russian"

final_audio = AudioSegment.silent(duration=1000)  # Додаємо коротку паузу перед початком

for i, sentence in enumerate(sentences, 1):
    #print(f"{len(sentence)} - {i}: {sentence} ")
    with tempfile.NamedTemporaryFile(delete=True, suffix=".wav") as temp_wav:
        with open(temp_wav.name, mode="wb") as file:
            _, output_text = tts.tts(sentence, Voices.Dmytro.value, Stress.Dictionary.value, file)
        temp_wav.seek(0)
        audio_segment = AudioSegment.from_wav(temp_wav.name)
        final_audio += audio_segment + AudioSegment.silent(duration=500)
final_audio += AudioSegment.silent(duration=1000)
final_audio.export("/tmp/"+sys.argv[1]+".wav", format="wav")
#print("✅ Готово! Файл 'test.wav' збережено.")