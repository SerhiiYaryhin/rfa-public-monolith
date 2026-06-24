import os
import json
import subprocess
import pika
import sys

# Словник для зберігання активних процесів ffmpeg (key: station_id, value: subprocess.Popen)
# Це дозволяє агенту пам'ятати, які трансляції зараз запущені, та зупиняти їх за потреби.
active_streams = {}

def start_ffmpeg(station_id, stream_url, rtp_url):
    """
    Запуск процесу FFmpeg для трансляції в Janus.
    
    Механіка:
    1. Читає вхідний потік (Icecast/MP3/AAC) у реальному часі (-re).
    2. Перекодує його в формат Opus (-acodec libopus), який є стандартом для WebRTC.
    3. Виставляє частоту 48кГц (-ar 48000) та стерео (-ac 2) для високої якості.
    4. Надсилає результат як сирий RTP потік на вказаний UDP порт сервера Janus.
    """
    if station_id in active_streams:
        print(f"[{station_id}] Трансляція вже запущена. Спочатку зупиніть її.")
        return

    # Команда FFmpeg для містка між Icecast та WebRTC
    cmd = [
        'ffmpeg',
        '-re',                         # Читати вхідний потік зі швидкістю реального часу
        '-i', stream_url,              # Вхідний URL (джерело радіо)
        '-acodec', 'libopus',          # Обов'язковий кодек для WebRTC
        '-ar', '48000',                # Частота дискретизації (AudioBridge стандарт)
        '-ac', '2',                    # Стерео звук
        '-payload_type', '111',         # (Ваше виправлення) Явно маркуємо RTP пакети як 111
        '-f', 'rtp',                   # Формат виводу — RTP
        rtp_url                        # Ціль (наприклад, rtp://janus.rfa:10010)
    ]

    print(f"[{station_id}] Запуск FFmpeg: {' '.join(cmd)}")

    # Запускаємо як фоновий процес ОС
    process = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    active_streams[station_id] = process

def stop_ffmpeg(station_id):
    """
    Зупинка процесу FFmpeg.
    
    Надсилає SIGTERM для м'якого завершення. Якщо процес не зупинився 
    за 5 секунд — вбиває його примусово.
    """
    if station_id in active_streams:
        process = active_streams[station_id]
        print(f"[{station_id}] Зупинка процесу FFmpeg (PID: {process.pid})...")
        process.terminate() 
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill() # Примусова зупинка

        del active_streams[station_id]
        print(f"[{station_id}] Трансляцію успішно зупинено.")
    else:
        print(f"[{station_id}] Активної трансляції не знайдено.")

def on_message_callback(ch, method, properties, body):
    """
    Обробник повідомлень з черги RabbitMQ.
    
    Слухає команди від Spring Boot (start/stop) та викликає відповідні функції.
    """
    try:
        # Декодуємо JSON-команду від Java-бекенда
        data = json.loads(body.decode('utf-8'))
        print(f"Отримано команду з RabbitMQ: {data}")

        command = data.get("command") 
        station_id = data.get("stationId")

        if command == "start":
            stream_url = data.get("streamUrl")
            rtp_url = data.get("rtpUrl") 
            start_ffmpeg(station_id, stream_url, rtp_url)

        elif command == "stop":
            stop_ffmpeg(station_id)

        # Підтверджуємо успішну обробку (Acknowledge)
        ch.basic_ack(delivery_tag=method.delivery_tag)

    except Exception as e:
        print(f"Помилка обробки повідомлення: {e}", file=sys.stderr)
        # У разі помилки повідомлення видаляється з черги (без перевідправки)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

def main():
    """
    Точка входу Агента. Встановлює з'єднання з RabbitMQ та починає слухати чергу.
    """
    # Налаштування підключення до RabbitMQ (використовує внутрішню адресу rabbit.rfa)
    credentials = pika.PlainCredentials('rfa', 'toloka2_2018') 
    parameters = pika.ConnectionParameters(
        host='rabbit.rfa', 
        virtual_host='/rfa-dev', # Працюємо у тому ж VHost, що і Janus
        credentials=credentials
    )

    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()

    # Оголошуємо чергу для команд
    queue_name = 'ffmpeg-agent-commands'
    channel.queue_declare(queue=queue_name, durable=True)

    print(f" [*] FFmpeg Агент RFA запущено. Очікування команд у черзі '{queue_name}'...")

    # Обробляємо по одному повідомленню за раз для стабільності
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=queue_name, on_message_callback=on_message_callback)

    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        print("Зупинка агента за запитом користувача...")
        channel.stop_consuming()
        # Перед виходом гасимо всі активні FFmpeg процеси
        for station in list(active_streams.keys()):
            stop_ffmpeg(station)
    connection.close()

if __name__ == '__main__':
    main()
