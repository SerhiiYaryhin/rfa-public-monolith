#
# обслуговуємо чергу для відтворення на радіо звукових файлів
#
import os
import pika
import json
import hexdump # https://stackoverflow.com/questions/12214801/print-a-string-as-hexadecimal-bytes
from config_loader import config
from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP
import base64
from config_loader import config

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
    with open(os.path.expanduser(locateDir) + "/" + guiServer + ".priv", "r") as key_file:
        # key_bytes = base64.b64decode(key_file.read())
        fileRead = key_file.read()
        print(fileRead)
        private_key = RSA.import_key(fileRead)
        # return RSA.import_key(key_bytes)
        return private_key
        #return RSA.import_key(key_bytes)

# Функція для розшифрування повідомлення
def decrypt_rsa(encrypted_message, private_key):
    cipher_rsa = PKCS1_OAEP.new(private_key)
    # encrypted_bytes = base64.b64decode(encrypted_message_base64)
    decrypted_data = cipher_rsa.decrypt(encrypted_message)
    return decrypted_data.decode()

# Зберігаєио приватний ключ від gui сервера
def SavePrivateKey(news_rpc_obj):
    #breakpoint()
    ldir = os.path.expanduser(locateDir)

    # Create the directory
    try:
        os.mkdir(ldir)
        print(f"Directory '{ldir}' created successfully.")
    except FileExistsError:
        print(f"Directory '{ldir}' already exists.")
    except PermissionError:
        print(f"Permission denied: Unable to create '{ldir}'.")
    except Exception as e:
        print(f"An error occurred: {e}")


    with open(ldir+"/"+news_rpc_obj["guiserver"]+".priv", "w") as private_file:
        private_file.write(news_rpc_obj["key"])
        #private_file.write(news_rpc_obj["key"].encode("utf-8"))
    return 0


# віправляємо зі сторе в ефір
def ToRadio(news_rpc_obj):
    print (news_rpc_obj)
    criptopsw =  news_rpc_obj["cpsw"]
    #breakpoint()
    # print (criptopsw)
    guiserver =  news_rpc_obj["guiserver"]
    print(guiserver)
    baseSiteAddress =  news_rpc_obj["baseSiteAddress"]
    newsUUID =  news_rpc_obj["newsStoreUUID"]
    username =  news_rpc_obj["username"]
    mainport =  news_rpc_obj["mainport"]
    mainpoint =  news_rpc_obj["mainpoint"]

    # private_key = load_private_key(guiserver)
    # 1️⃣ Читаємо приватний ключ із файлу (PEM-формат)
    with open(os.path.expanduser(locateDir) + "/" + guiServer + ".priv", "r") as key_file:
        private_key = RSA.import_key(key_file.read())
        # 3️⃣ Розшифровуємо дані
        cipher_rsa = PKCS1_OAEP.new(private_key)
        decrypted_message = cipher_rsa.decrypt(criptopsw)


    # print("====================== criptopsw")
    # print (criptopsw)
    # print("====================== private_key")
    # print(private_key)
    # decrypted_message = decrypt_rsa(criptopsw, private_key)
    print("====================== decrypted_message")
    print(decrypted_message)
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
             SavePrivateKey(news_rpc_obj)
        case "toRadio":
             ToRadio(news_rpc_obj)
        case _:
            print("Якісь фігня прелетіла")

    print("🔄 Очікування повідомлень... Натисніть CTRL+C для виходу.")
    # Обробити нештатні ситуації
    return 0;

# Функція зворотного виклику для обробки повідомлення
def callback(ch, method, properties, body):
    print(f"📥 Отримано повідомлення.")

    # Розбираємо JSON у Python-словник
    news_rpc_obj = json.loads(body.decode())

    # Обробляємо повідомлення
    news_rpc_obj["rc"] = process_toRadio(news_rpc_obj)

    # Перетворюємо назад у JSON
    #output_json = json.dumps(news_rpc_obj)

    # Надсилаємо у вихідну чергу
    #output_queue = news_rpc_obj["front"]["server"]

#    channel.queue_declare(queue=news_rpc_obj["front"]["server"], durable=True)
    #print(f"📤 output_queue: {rabbitmq_vhost} - {output_queue}")
    #ch.basic_publish(exchange="", routing_key=output_queue, body=output_json)
    #print(f"📤 Відправлено у {output_queue}: {output_json}")

    ch.basic_ack(delivery_tag=method.delivery_tag)  # Підтвердження отримання

# Підписка на вхідну чергу
channel.basic_consume(queue=input_queue, on_message_callback=callback)

print("🔄 Очікування повідомлень... Натисніть CTRL+C для виходу.")
channel.start_consuming()
