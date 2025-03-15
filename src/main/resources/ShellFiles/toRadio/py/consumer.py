import os
import pika
import json
from config_loader import config
from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP
import base64


# Завантаження конфігурації RabbitMQ
rabbitmq_host = config["rabbitmq"]["host"]
rabbitmq_port = config["rabbitmq"]["port"]
rabbitmq_user = config["rabbitmq"]["username"]
rabbitmq_password = config["rabbitmq"]["password"]
input_queue = config["rabbitmq"]["input_queue"]
rabbitmq_vhost = config["rabbitmq"]["vhost"]
locateDir = config["locateDir"]

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

# Завантажуємо приватний ключ із файлу
def load_private_key(guiServer):
    with open(locateDir+"/"+guiServer+".priv", "rb") as key_file:
        key_bytes = base64.b64decode(key_file.read())
        return RSA.import_key(key_bytes)

# Функція для розшифрування повідомлення
def decrypt_rsa(encrypted_message_base64, private_key):
    cipher_rsa = PKCS1_OAEP.new(private_key)
    encrypted_bytes = base64.b64decode(encrypted_message_base64)
    decrypted_bytes = cipher_rsa.decrypt(encrypted_bytes)
    return decrypted_bytes.decode('utf-8')

# Зберігаєио приватний ключ від gui сервера
def SavePrivateKey(news_rpc_obj)
    #breakpoint()
    with open(locateDir+"/"+guiServer+".priv", "wb") as private_file:
        public_file.write(news_rpc_obj["key"])


# віправляємо зі сторе в ефір
def ToRadio(news_rpc_obj)
    #breakpoint()
    private_key = load_private_key("private_key.pem")
    criptpsw =  news_rpc_obj["password"]
    guiserver =  news_rpc_obj["guiserver"]
    private_key = load_private_key(guiserver)
    decrypted_message = decrypt_rsa(criptpsw, private_key)
    # формуємо командну строку
    cmd = "ffmpeg -re -v quiet -stats -i https://front.rfa.toloka.media/store/audio/" + news_rpc_obj["newsUUID"]
    +   "https://front.rfa.toloka.media:" + news_rpc_obj["mainport"]
    + "/" +  news_rpc_obj["mainpoint"]
    print (cmd)
# Функція обробки повідомлення
def process_toRadio(news_rpc_obj):
    #breakpoint()
    match news_rpc_obj["job"]:
        case "toRadiokey":
             SavePrivKey(news_rpc_obj)
        case "toRadio":
             ToRadio(news_rpc_obj)
        case _:
            print("Якісь фігня прелктіла")



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
