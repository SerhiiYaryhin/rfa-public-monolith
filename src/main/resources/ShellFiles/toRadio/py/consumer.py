import nltk
import tempfile
import os
from pydub import AudioSegment
from ukrainian_tts.tts import TTS, Voices, Stress
import pika
import json
from config_loader import config


# Завантаження конфігурації RabbitMQ
rabbitmq_host = config["rabbitmq"]["host"]
rabbitmq_port = config["rabbitmq"]["port"]
rabbitmq_user = config["rabbitmq"]["username"]
rabbitmq_password = config["rabbitmq"]["password"]
input_queue = config["rabbitmq"]["input_queue"]
rabbitmq_vhost = config["rabbitmq"]["vhost"]
tts_host = config["users"]["tts"]["server"]
tts_user = config["users"]["tts"]["user"]
#print(f"📤 input_queue: {rabbitmq_vhost} - {input_queue}")
#output_queue = config["rabbitmq"]["output_queue"]

# Авторизація
credentials = pika.PlainCredentials(rabbitmq_user, rabbitmq_password)

# Встановлення з'єднання
connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=rabbitmq_host,
            port=rabbitmq_port,
            virtual_host=rabbitmq_vhost,  # Віртуальний хост
            credentials=credentials
        )
)
channel = connection.channel()

# Декларація черг
channel.queue_declare(queue=input_queue, durable=True)
#channel.queue_declare(queue=output_queue, durable=True)

nltk.download("punkt")
from nltk.tokenize import sent_tokenize
tts = TTS(device="cpu")  # Можна змінити на "gpu" або "mps" для швидшої генерації



# def process_message(news_rpc_obj):
#     """ Функція обробки отриманого JSON """
#     news_rpc_obj["text"] = news_rpc_obj["text"].upper()  # Робимо текст заголовними літерами
#     news_rpc_obj["rc"] = 201  # Оновлюємо код результату
#     return news_rpc_obj

# Функція обробки повідомлення
def process_tts(news_rpc_obj):
    text =  news_rpc_obj["text"]
    sentences = sent_tokenize(text, language="russian")  # Для української використовуємо "russian"
    final_audio = AudioSegment.silent(duration=500)  # Додаємо коротку паузу перед початком

    for i, sentence in enumerate(sentences, 1):
        #print(f"{len(sentence)} - {i}: {sentence} ")
        with tempfile.NamedTemporaryFile(delete=True, suffix=".wav") as temp_wav:
            with open(temp_wav.name, mode="wb") as file:
                _, output_text = tts.tts(sentence, Voices.Dmytro.value, Stress.Dictionary.value, file)
            # _, output_text = tts.tts(sentence, Voices.Dmytro.value, Stress.Dictionary.value, file)
            temp_wav.seek(0)
            audio_segment = AudioSegment.from_wav(temp_wav.name)
            final_audio += audio_segment + AudioSegment.silent(duration=500)

#     final_audio.export("/tmp/"+news_rpc_obj["newsUUID"]+".wav", format="wav")
    final_audio += audio_segment + AudioSegment.silent(duration=500)
    final_audio.export("/tmp/"+news_rpc_obj["newsUUID"]+".mp3", format="mp3", bitrate="48k")
    print("🔄 Очікування повідомлень... Натисніть CTRL+C для виходу.")
    # Обробити нештатні ситуації
    return 0;

# Функція зворотного виклику для обробки повідомлення
def callback(ch, method, properties, body):
    print(f"📥 Отримано повідомлення.")

    # Розбираємо JSON у Python-словник
    news_rpc_obj = json.loads(body.decode())

    # Обробляємо повідомлення
    news_rpc_obj["rc"] = process_tts(news_rpc_obj)
    news_rpc_obj["rJobType"] = "JOB_TTS_FILES_READY"
    news_rpc_obj["tts"]["server"] = tts_host
    news_rpc_obj["tts"]["user"] = tts_user
    news_rpc_obj["text"] = " "
    # Перетворюємо назад у JSON
    output_json = json.dumps(news_rpc_obj)

    # Надсилаємо у вихідну чергу
    output_queue = news_rpc_obj["front"]["server"]

    channel.queue_declare(queue=news_rpc_obj["front"]["server"], durable=True)
    #print(f"📤 output_queue: {rabbitmq_vhost} - {output_queue}")
    ch.basic_publish(exchange="", routing_key=output_queue, body=output_json)
    #print(f"📤 Відправлено у {output_queue}: {output_json}")

    ch.basic_ack(delivery_tag=method.delivery_tag)  # Підтвердження отримання

# Підписка на вхідну чергу
channel.basic_consume(queue=input_queue, on_message_callback=callback)

print("🔄 Очікування повідомлень... Натисніть CTRL+C для виходу.")
channel.start_consuming()
