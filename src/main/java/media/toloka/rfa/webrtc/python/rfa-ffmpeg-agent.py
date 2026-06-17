import os
import json
import subprocess
import pika
import sys

# Словник для зберігання активних процесів ffmpeg (key: station_id, value: subprocess.Popen)
active_streams = {}

def start_ffmpeg(station_id, stream_url, rtp_url):
    """Запуск процесу FFmpeg для трансляції в Janus"""
    if station_id in active_streams:
        print(f"[{station_id}] Трансляція вже запущена. Спочатку зупиніть її.")
        return

    # Команда FFmpeg для перекодування MP3/AAC з Icecast в Opus (RTP) для Janus
    cmd = [
        'ffmpeg',
        '-re',                         # Читати потік у реальному часі
        '-i', stream_url,              # Вхідний URL (Icecast)
        '-acodec', 'libopus',          # Кодек Opus (стандарт WebRTC)
        '-ar', '48000',                # Частота дискретизації 48kHz
        '-ac', '2',                    # Стерео
        '-f', 'rtp',                   # Формат виводу RTP
        rtp_url                        # Куди лити (наприклад, rtp://janus.rfa:10010)
    ]

    print(f"[{station_id}] Запуск FFmpeg: {' '.join(cmd)}")

    # Запускаємо як фоновий процес
    process = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    active_streams[station_id] = process

def stop_ffmpeg(station_id):
    """Зупинка процесу FFmpeg"""
    if station_id in active_streams:
        process = active_streams[station_id]
        print(f"[{station_id}] Зупинка процесу FFmpeg (PID: {process.pid})...")
        process.terminate() # М'яка зупинка
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill() # Примусова зупинка, якщо не зупинився

        del active_streams[station_id]
        print(f"[{station_id}] Трансляцію успішно зупинено.")
    else:
        print(f"[{station_id}] Активної трансляції не знайдено.")

def on_message_callback(ch, method, properties, body):
    """Обробник повідомлень з черги RabbitMQ"""
    try:
        # Декодуємо JSON-команду від Spring Boot
        data = json.loads(body.decode('utf-8'))
        print(f"Отримано команду з RabbitMQ: {data}")

        command = data.get("command") # start або stop
        station_id = data.get("stationId")

        if command == "start":
            stream_url = data.get("streamUrl")
            rtp_url = data.get("rtpUrl") # Формується на стороні Java (IP Janus + порт кімнати)
            start_ffmpeg(station_id, stream_url, rtp_url)

        elif command == "stop":
            stop_ffmpeg(station_id)

        # Підтверджуємо успішну обробку повідомлення
        ch.basic_ack(delivery_tag=method.delivery_tag)

    except Exception as e:
        print(f"Помилка обробки повідомлення: {e}", file=sys.stderr)
        # У разі помилки повертаємо повідомлення в чергу (або логуємо)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

def main():
    # Налаштування підключення до вашого RabbitMQ
    credentials = pika.PlainCredentials('guest', 'guest') # Змініть на ваші, якщо змінені
    parameters = pika.ConnectionParameters(host='rabbit.rfa', credentials=credentials)

    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()

    # Оголошуємо чергу
    queue_name = 'ffmpeg-agent-commands'
    channel.queue_declare(queue=queue_name, durable=True)

    print(f" [*] FFmpeg Агент запущено. Очікування команд у черзі '{queue_name}'...")

    # Налаштовуємо prefetch, щоб обробляти по одному повідомленню за раз
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=queue_name, on_message_callback=on_message_callback)

    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        print("Зупинка агента...")
        channel.stop_consuming()
        # Перед виходом гасимо всі стріми
        for station in list(active_streams.keys()):
            stop_ffmpeg(station)
    connection.close()

if __name__ == '__main__':
    main()