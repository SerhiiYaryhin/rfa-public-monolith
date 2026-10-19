#!/usr/bin/env python3
"""
FFmpeg Agent для RFA - приймає команди від Spring Boot через RabbitMQ
та керує процесами FFmpeg для трансляції зовнішніх потоків в Janus.

Архітектура:
    Spring Boot → RabbitMQ → rfa-ffmpeg-agent.py → FFmpeg → Janus (RTP)

Команди:
    - start: запуск трансляції
    - stop: зупинка трансляції
    - status: перевірка активних трансляцій

Виправлення v2:
    - payload_type змінено з 111 на 100 (стандарт Janus для Opus)
    - Додано підтримку ffmpegOptions з JSON повідомлення
    - Додано логування stdout/stderr FFmpeg
    - Додано моніторинг стану процесу
    - Додано pkt_size=1200 в URL автоматично
    - Додано команду status
    - Автоматичне очищення завершених процесів
"""

import os
import json
import subprocess
import threading
import time
import pika
import sys
from datetime import datetime

# ============================================================
# КОНФІГУРАЦІЯ
# ============================================================

RABBITMQ_HOST = 'rabbit.rfa'
RABBITMQ_VHOST = '/rfa'
RABBITMQ_USER = 'rfa'
RABBITMQ_PASSWORD = 'toloka2_2018'
QUEUE_NAME = 'ffmpeg-agent-commands'

# Стандартний payload_type для Opus в Janus
DEFAULT_PAYLOAD_TYPE = 100
DEFAULT_PKT_SIZE = 1200

# ============================================================
# ГЛОБАЛЬНИЙ СТАН
# ============================================================

# Словник активних процесів FFmpeg
# Key: station_id, Value: dict з інформацією про процес
active_streams = {}

# ============================================================
# ЛОГУВАННЯ
# ============================================================

def log(message, level='INFO'):
    """
    Логування з часовою міткою та рівнем.

    @param message Текст повідомлення
    @param level Рівень: INFO, SUCCESS, ERROR, WARN, DEBUG
    """
    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    prefix = {
        'INFO': 'ℹ️',
        'SUCCESS': '✅',
        'ERROR': '❌',
        'WARN': '⚠️',
        'DEBUG': '🔍'
    }.get(level, 'ℹ️')

    print(f"[{timestamp}] {prefix} {message}", flush=True)

# ============================================================
# УПРАВЛІННЯ FFmpeg
# ============================================================

def build_ffmpeg_command(station_id, stream_url, rtp_url, options=None):
    """
    Будує команду FFmpeg з урахуванням опцій.

    @param station_id ID станції
    @param stream_url URL вхідного потоку (Icecast)
    @param rtp_url URL для виводу (rtp://...)
    @param options Додаткові опції з ffmpegOptions (dict)
    @return list Команда для subprocess
    """

    # Базова команда
    cmd = [
        'ffmpeg',
        '-nostats',                   # Не виводити статистику
        '-loglevel', 'warning',       # Тільки попередження та помилки
        '-re',                        # Читати вхідний потік у реальному часі
        '-i', stream_url,             # Вхідний URL (джерело радіо)
    ]

    # Аудіо параметри (з опцій або дефолтні)
    if options:
        acodec = options.get('acodec', 'libopus')
        bitrate = options.get('b:a', '128k')
        sample_rate = options.get('ar', '48000')
        channels = options.get('ac', '2')
        application = options.get('application', 'audio')
        vbr = options.get('vbr', 'on')
        compression = options.get('compression_level', '10')
    else:
        acodec = 'libopus'
        bitrate = '128k'
        sample_rate = '48000'
        channels = '2'
        application = 'audio'
        vbr = 'on'
        compression = '10'

    # Додаємо аудіо параметри
    cmd.extend([
        '-acodec', acodec,            # Кодек (Opus для WebRTC)
        '-b:a', bitrate,              # Бітрейт
        '-ar', str(sample_rate),      # Частота дискретизації
        '-ac', str(channels),         # Кількість каналів
        '-application', application,  # Режим кодера (audio для музики)
        '-vbr', vbr,                  # Variable bitrate
        '-compression_level', str(compression),  # Рівень стиснення
    ])

    # ✅ ВИПРАВЛЕНО: payload_type = 100 (стандарт для Opus в Janus)
    cmd.extend([
        '-payload_type', str(DEFAULT_PAYLOAD_TYPE),
    ])

    # Формат виводу
    cmd.extend([
        '-f', 'rtp',                  # Формат RTP
    ])

    # ✅ ВИПРАВЛЕНО: Додаємо pkt_size в URL якщо його немає
    if 'pkt_size' not in rtp_url:
        rtp_url = f"{rtp_url}?pkt_size={DEFAULT_PKT_SIZE}"

    cmd.append(rtp_url)

    return cmd


def monitor_ffmpeg_process(station_id, process):
    """
    Моніторить процес FFmpeg в окремому потоці.
    Логує вивід та повідомляє про завершення.
    """
    try:
        # Читаємо stderr для логування помилок
        for line in iter(process.stderr.readline, b''):
            if line:
                log(f"[{station_id}] FFmpeg: {line.decode('utf-8').strip()}", 'DEBUG')

        # Чекаємо завершення процесу
        returncode = process.wait()

        if returncode == 0:
            log(f"[{station_id}] FFmpeg завершився нормально", 'INFO')
        elif returncode == -15:  # SIGTERM
            log(f"[{station_id}] FFmpeg зупинено за запитом (SIGTERM)", 'INFO')
        else:
            log(f"[{station_id}] FFmpeg завершився з кодом {returncode}", 'ERROR')

        # Видаляємо зі списку активних
        if station_id in active_streams:
            del active_streams[station_id]

    except Exception as e:
        log(f"[{station_id}] Помилка моніторингу: {e}", 'ERROR')


def start_ffmpeg(station_id, stream_url, rtp_url, options=None):
    """
    Запуск процесу FFmpeg для трансляції в Janus.

    @param station_id ID станції
    @param stream_url URL вхідного потоку
    @param rtp_url URL для виводу RTP
    @param options Додаткові опції FFmpeg (dict)
    """

    # Перевірка чи вже запущено
    if station_id in active_streams:
        process_info = active_streams[station_id]
        if process_info['process'].poll() is None:
            log(f"[{station_id}] Трансляція вже запущена (PID: {process_info['process'].pid}). Спочатку зупиніть її.", 'WARN')
            return
        else:
            # Процес завершився, видаляємо зі списку
            del active_streams[station_id]

    # Будуємо команду
    cmd = build_ffmpeg_command(station_id, stream_url, rtp_url, options)

    log(f"[{station_id}] Запуск FFmpeg...", 'INFO')
    log(f"[{station_id}] Команда: {' '.join(cmd)}", 'DEBUG')
    log(f"[{station_id}] Вхідний потік: {stream_url}", 'INFO')
    log(f"[{station_id}] Вихідний RTP: {rtp_url}", 'INFO')

    try:
        # Запускаємо процес з перехопленням stderr для логування
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            bufsize=1,  # Рядковий буфер
            universal_newlines=False
        )

        # Зберігаємо інформацію про процес
        active_streams[station_id] = {
            'process': process,
            'pid': process.pid,
            'start_time': datetime.now(),
            'stream_url': stream_url,
            'rtp_url': rtp_url
        }

        log(f"[{station_id}] FFmpeg запущено (PID: {process.pid})", 'SUCCESS')

        # Запускаємо моніторинг в окремому потоці
        monitor_thread = threading.Thread(
            target=monitor_ffmpeg_process,
            args=(station_id, process),
            daemon=True
        )
        monitor_thread.start()

    except FileNotFoundError:
        log(f"[{station_id}] FFmpeg не знайдено! Переконайтесь, що ffmpeg встановлено і в PATH.", 'ERROR')
    except Exception as e:
        log(f"[{station_id}] Помилка запуску FFmpeg: {e}", 'ERROR')


def stop_ffmpeg(station_id):
    """
    Зупинка процесу FFmpeg.
    Надсилає SIGTERM для м'якого завершення. Якщо процес не зупинився
    за 5 секунд — вбиває його примусово.
    """
    if station_id not in active_streams:
        log(f"[{station_id}] Активної трансляції не знайдено.", 'WARN')
        return

    process_info = active_streams[station_id]
    process = process_info['process']

    log(f"[{station_id}] Зупинка процесу FFmpeg (PID: {process.pid})...", 'INFO')

    try:
        # М'яка зупинка (SIGTERM)
        process.terminate()

        try:
            # Чекаємо 5 секунд
            process.wait(timeout=5)
            log(f"[{station_id}] Трансляцію успішно зупинено.", 'SUCCESS')
        except subprocess.TimeoutExpired:
            # Примусова зупинка (SIGKILL)
            log(f"[{station_id}] FFmpeg не відповів за 5 сек. Примусова зупинка...", 'WARN')
            process.kill()
            process.wait()
            log(f"[{station_id}] Трансляцію примусово зупинено.", 'SUCCESS')

        # Видаляємо зі списку
        del active_streams[station_id]

    except Exception as e:
        log(f"[{station_id}] Помилка зупинки: {e}", 'ERROR')


def get_status():
    """Повертає статус всіх активних трансляцій."""
    status = {}
    for station_id, info in active_streams.items():
        process = info['process']
        status[station_id] = {
            'pid': info['pid'],
            'running': process.poll() is None,
            'start_time': info['start_time'].isoformat(),
            'stream_url': info['stream_url'],
            'rtp_url': info['rtp_url']
        }
    return status


# ============================================================
# ОБРОБКА ПОВІДОМЛЕНЬ З RABBITMQ
# ============================================================

def on_message_callback(ch, method, properties, body):
    """
    Обробник повідомлень з черги RabbitMQ.
    Слухає команди від Spring Boot (start/stop/status) та викликає відповідні функції.
    """
    try:
        # Декодуємо JSON-команду від Java-бекенду
        data = json.loads(body.decode('utf-8'))
        log(f"Отримано команду з RabbitMQ: {data}", 'DEBUG')

        command = data.get('command')
        station_id = data.get('stationId')

        if command == 'start':
            stream_url = data.get('streamUrl')
            rtp_url = data.get('rtpUrl')
            options = data.get('ffmpegOptions')  # ✅ НОВЕ: підтримка опцій

            if not stream_url or not rtp_url:
                log(f"[{station_id}] Відсутні обов'язкові параметри: streamUrl або rtpUrl", 'ERROR')
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
                return

            log(f"[{station_id}] Команда: START", 'INFO')
            log(f"[{station_id}] Stream URL: {stream_url}", 'INFO')
            log(f"[{station_id}] RTP URL: {rtp_url}", 'INFO')
            if options:
                log(f"[{station_id}] FFmpeg Options: {options}", 'DEBUG')

            start_ffmpeg(station_id, stream_url, rtp_url, options)

        elif command == 'stop':
            log(f"[{station_id}] Команда: STOP", 'INFO')
            stop_ffmpeg(station_id)

        elif command == 'status':
            log(f"Команда: STATUS", 'INFO')
            status = get_status()
            log(f"Активних трансляцій: {len(status)}", 'INFO')
            for sid, info in status.items():
                log(f"  - {sid}: PID={info['pid']}, running={info['running']}", 'INFO')

        else:
            log(f"Невідома команда: {command}", 'WARN')

        # Підтверджуємо успішну обробку
        ch.basic_ack(delivery_tag=method.delivery_tag)

    except json.JSONDecodeError as e:
        log(f"Помилка декодування JSON: {e}", 'ERROR')
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
    except Exception as e:
        log(f"Помилка обробки повідомлення: {e}", 'ERROR')
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


# ============================================================
# ТОЧКА ВХОДУ
# ============================================================

def main():
    """
    Точка входу Агента. Встановлює з'єднання з RabbitMQ та починає слухати чергу.
    """
    log("=" * 60, 'INFO')
    log("FFmpeg Агент RFA v2.0", 'INFO')
    log("=" * 60, 'INFO')

    # Налаштування підключення до RabbitMQ
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)
    parameters = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        virtual_host=RABBITMQ_VHOST,
        credentials=credentials,
        heartbeat=60,  # Heartbeat для стабільності
        blocked_connection_timeout=300
    )

    try:
        log(f"Підключення до RabbitMQ: {RABBITMQ_HOST}...", 'INFO')
        connection = pika.BlockingConnection(parameters)
        channel = connection.channel()

        # Оголошуємо чергу для команд
        channel.queue_declare(queue=QUEUE_NAME, durable=True)

        log(f" [*] Агент запущено. Очікування команд у черзі '{QUEUE_NAME}'...", 'SUCCESS')
        log(f" [*] Payload Type: {DEFAULT_PAYLOAD_TYPE}", 'INFO')
        log(f" [*] Packet Size: {DEFAULT_PKT_SIZE}", 'INFO')

        # Обробляємо по одному повідомленню за раз
        channel.basic_qos(prefetch_count=1)
        channel.basic_consume(queue=QUEUE_NAME, on_message_callback=on_message_callback)

        # Запускаємо споживання
        channel.start_consuming()

    except pika.exceptions.AMQPConnectionError as e:
        log(f"Помилка підключення до RabbitMQ: {e}", 'ERROR')
        log("Перевірте налаштування мережі та облікові дані.", 'ERROR')
        sys.exit(1)
    except KeyboardInterrupt:
        log("Зупинка агента за запитом користувача...", 'INFO')

        # Зупиняємо всі активні FFmpeg процеси
        for station_id in list(active_streams.keys()):
            stop_ffmpeg(station_id)

        if 'channel' in locals():
            channel.stop_consuming()
        if 'connection' in locals():
            connection.close()

        log("Агент зупинено.", 'SUCCESS')


if __name__ == '__main__':
    main()