import pika
import json
from config_loader import config

# Завантаження конфігурації RabbitMQ
rabbitmq_host = config["rabbitmq"]["host"]
rabbitmq_port = config["rabbitmq"]["port"]
rabbitmq_user = config["rabbitmq"]["username"]
rabbitmq_password = config["rabbitmq"]["password"]
rabbitmq_vhost = config["rabbitmq"]["vhost"]
input_queue = config["rabbitmq"]["input_queue"]

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

# Декларація черги
channel.queue_declare(queue=input_queue, durable=True)

# Формування тестового повідомлення
message = {
    "rJobType": "NEWS_PROCESSING",
    "stationUUID": "123e4567-e89b-12d3-a456-426614174000",
    "newsUUID": "987e6543-e21b-34c3-b567-426614174999",
    "text": "Breaking news: Python and RabbitMQ!",
    "rc": 200
}

# Відправка JSON у чергу
message_json = json.dumps(message)
channel.basic_publish(exchange="", routing_key=input_queue, body=message_json)
print(f"📨 Відправлено у {input_queue}: {message_json}")

# Закриття з'єднання 1
connection.close()
