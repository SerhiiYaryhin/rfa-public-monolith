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
output_queue = config["rabbitmq"]["output_queue"]

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
channel.queue_declare(queue=input_queue)
channel.queue_declare(queue=output_queue)

# Функція обробки повідомлення
def process_message(news_rpc_obj):
    """ Функція обробки отриманого JSON """
    news_rpc_obj["text"] = news_rpc_obj["text"].upper()  # Робимо текст заголовними літерами
    news_rpc_obj["rc"] = 201  # Оновлюємо код результату
    return news_rpc_obj

# Функція зворотного виклику для обробки повідомлення
def callback(ch, method, properties, body):
    print(f"📥 Отримано повідомлення: {body.decode()}")

    # Розбираємо JSON у Python-словник
    news_rpc_obj = json.loads(body.decode())

    # Обробляємо повідомлення
    processed_message = process_message(news_rpc_obj)

    # Перетворюємо назад у JSON
    output_json = json.dumps(processed_message)

    # Надсилаємо у вихідну чергу
    ch.basic_publish(exchange="", routing_key=output_queue, body=output_json)
    print(f"📤 Відправлено у {output_queue}: {output_json}")

    ch.basic_ack(delivery_tag=method.delivery_tag)  # Підтвердження отримання

# Підписка на вхідну чергу
channel.basic_consume(queue=input_queue, on_message_callback=callback)

print("🔄 Очікування повідомлень... Натисніть CTRL+C для виходу.")
channel.start_consuming()
