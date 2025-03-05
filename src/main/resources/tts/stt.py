import nltk
from ukrainian_tts.tts import TTS, Voices, Stress

nltk.download("punkt")  # Завантажує токенізатор
from nltk.tokenize import sent_tokenize


tts = TTS(device="cpu") # can try gpu, mps
with open("text.txt", "r", encoding="utf-8") as f:
  text = f.read()
print(text)
sentences = sent_tokenize(text, language="russian")  # Для української використовуємо "russian"

with open("test.wav", mode="wb") as file:
    for i, sentence in enumerate(sentences, 1):
        print(f"{i} {sentence}")
        _, output_text = tts.tts(sentence, Voices.Dmytro.value, Stress.Dictionary.value, file)
print("Accented text:", output_text)



