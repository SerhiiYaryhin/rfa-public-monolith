import os
import shutil
import pika
import json
from config_loader import config
import whisper
import pathlib
import requests
from datetime import datetime

# https://labex.io/tutorials/linux-how-to-install-whisper-cli-on-linux-437909
# https://www.digitalocean.com/community/tutorials/how-to-generate-and-add-subtitles-to-videos-using-python-openai-whisper-and-ffmpeg

# Завантаження конфігурації RabbitMQ
rabbitmq_host = config["rabbitmq"]["host"]
rabbitmq_port = config["rabbitmq"]["port"]
rabbitmq_user = config["rabbitmq"]["username"]
rabbitmq_password = config["rabbitmq"]["password"]
input_queue = config["rabbitmq"]["input_queue"]
rabbitmq_vhost = config["rabbitmq"]["vhost"]
tts_host = config["users"]["tts"]["server"]
tts_user = config["users"]["tts"]["user"]

# Авторизація
credentials = pika.PlainCredentials(rabbitmq_user, rabbitmq_password)

# Встановлення з'єднання
connection = pika.BlockingConnection(
    pika.ConnectionParameters(
        host=rabbitmq_host,
        port=rabbitmq_port,
        virtual_host=rabbitmq_vhost,  # Віртуальний хост
        credentials=credentials,
        heartbeat=600,
        blocked_connection_timeout=300
    )
)
channel = connection.channel()

# Декларація черг
channel.queue_declare(queue=input_queue, durable=True)
resultText = ""


# Завантажуємо файл зі звуком
# 
# def load_voice_file(sttuuuid,voice)

# Функція обробки повідомлення
def process_stt(stt_rpc_obj):
    print(stt_rpc_obj)
    resultText = ""

    # Завантажуємо файл зі звуком
    pathdir = '/tmp/' + stt_rpc_obj['sttUUID']
    isExist = os.path.exists(pathdir)
    if not isExist:
        os.makedirs(pathdir)

    # Завантажуємо файл з голосом для преретворення 
    # https://rfa.toloka.media/store/content/og/c856b21c-3b51-40c5-a010-f9739b474312/8a362736-8965-461f-ad2f-e37433fcad27.mp3
    url = f"{stt_rpc_obj['front']['globalserver']}/store/content/og/{stt_rpc_obj['uuidvoice']}/{stt_rpc_obj['filenamevoice']}"

    try:
        response = requests.get(url, timeout=5)
        response.raise_for_status()  # викликає HTTPError для 4xx/5xx
    except HTTPError as e:
        print(f"❌ HTTP помилка: {e} (код {e.response.status_code})")

    except Timeout:
        print("⏰ Запит перевищив час очікування!")

    except ConnectionError:
        print("🔌 Проблема з підключенням!")

    except RequestException as e:
        print(f"💥 Інша помилка запиту: {e}")

    except ValueError:
        print("⚠️ Відповідь не є JSON")

    # Формуємо імʼя файлу
    # print(f"FileNameVoice: {stt_rpc_obj['filenamevoice']}")
    localVoiceFileName = pathdir + "/" + stt_rpc_obj["sttUUID"] + pathlib.Path(stt_rpc_obj['filenamevoice']).suffix
    # print("Локальний файл з голосом: " + localVoiceFileName)
    # print(pathlib.Path('yourPath.example').suffix)  # '.example'
    # print(pathlib.Path("hello/foo.bar.tar.gz").suffixes)  # ['.bar', '.tar', '.gz']
    # print(pathlib.Path('/foo/bar.txt').stem)  # 'bar'

    try:
        with open(localVoiceFileName, 'wb') as file:
            file.write(response.content)
        print(f"Файл з голосом успішно записано: {localVoiceFileName}")
    except IOError as e:
        print(f"Сталася помилка при записі у файл: {localVoiceFileName}")
        print(f"Помилка при записі у файл: {e}")

    audio = whisper.load_audio(localVoiceFileName)
    model = whisper.load_model(stt_rpc_obj["model"], device='cpu')
    result = model.transcribe(audio)
    # result = "Результат роботи Whisper"

    # видаляємо файл з голосом
    shutil.rmtree(pathdir)

    # зберігаємо результат
    resultText = result["text"]
    # resultText = "Output text for front server"
    # localTextFileName = pathdir + "/" + stt_rpc_obj["sttUUID"] + ".txt"
    # try:
    #     with open(localTextFileName, 'wb') as file:
    #         file.write(resultText.encode('utf-8'))
    #     print(f"Файл з текстом успішно записано: {localTextFileName}")
    # except IOError as e:
    #     print(f"Сталася помилка при записі у файл: {localTextFileName}")
    #     print(f"Помилка при записі у файл: {e}")

    print("🔄 Очікування повідомлень... Натисніть CTRL+C для виходу.")
    # Обробити нештатні ситуації
    return 0, resultText, result;


# Функція зворотного виклику для обробки повідомлення
def callback(ch, method, properties, body):
    print(f"📥 Отримано повідомлення: {body} ")

    startjob = datetime.now()
    # Розбираємо JSON у Python-словник
    rpc_obj = json.loads(body.decode())

    # Обробляємо повідомлення. Викликаємо роботу з whisper
    rc, resultText, resultObject = process_stt(rpc_obj)

    rpc_obj["rc"] = rc
    # print(f"Result text :{resultText}")

    rpc_obj["rJobType"] = "JOB_STT_FILES_READY"
    rpc_obj['stt']['localserver'] = tts_host
    rpc_obj["stt"]["user"] = tts_user
    rpc_obj["text"] = resultText
    rpc_obj['backServer']['addparametrs'] = json.dumps(resultObject)
    endjob = datetime.now()
    rpc_obj["endjob"] = endjob.isoformat()
    rpc_obj["startjob"] = startjob.isoformat()
    # Перетворюємо назад у JSON
    output_json = json.dumps(rpc_obj)
    # print("Output json: " + output_json)

    # Надсилаємо у вихідну чергу
    output_queue = rpc_obj["front"]["localserver"]
    # print("Queue out: " + output_queue)
    channel.queue_declare(queue=rpc_obj["front"]["localserver"], durable=True)
    channel.basic_publish(exchange="", routing_key=output_queue, body=output_json)
    ch.basic_ack(delivery_tag=method.delivery_tag)  # Підтвердження отримання


# audio_file = "./19bcba96-fcc8-4833-b2e5-653c81530d55.mp3"
# audio = whisper.load_audio(audio_file)
# model = whisper.load_model("turbo", device='cpu')
# result = model.transcribe(audio)
# print(result)

# Підписка на вхідну чергу
channel.basic_consume(queue=input_queue, on_message_callback=callback)

print("🔄 Очікування повідомлень... Натисніть CTRL+C для виходу.")
channel.start_consuming()
