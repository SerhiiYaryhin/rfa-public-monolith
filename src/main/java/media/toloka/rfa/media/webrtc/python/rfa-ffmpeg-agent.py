#!/usr/bin/env python3
"""
FFmpeg Agent для RFA v3.1 - БЕЗПЕЧНИЙ контроль процесів

Ключова відмінність від v3.0:
- Завершує ТІЛЬКИ FFmpeg процеси, запущені цим агентом
- НЕ чіпає інші FFmpeg процеси на сервері
- Використовує PID-файли + змінну оточення + аналіз cmdline для ідентифікації

Архітектура:
    Spring Boot → RabbitMQ → rfa-ffmpeg-agent.py → FFmpeg → Janus (RTP)
"""

import os
import json
import subprocess
import threading
import time
import signal
import sys
from datetime import datetime, timedelta
from pathlib import Path

import pika

# ============================================================
# КОНФІГУРАЦІЯ
# ============================================================

RABBITMQ_HOST = 'rabbit.rfa'
RABBITMQ_VHOST = '/rfa'
RABBITMQ_USER = 'rfa'
RABBITMQ_PASSWORD = 'toloka2_2018'
QUEUE_NAME = 'ffmpeg-agent-commands'

DEFAULT_PAYLOAD_TYPE = 100
DEFAULT_PKT_SIZE = 1200

HEARTBEAT_TIMEOUT = 300
HEARTBEAT_CHECK_INTERVAL = 60

PID_DIR = '/tmp/rfa-ffmpeg-agent'
STATUS_FILE = '/tmp/rfa-ffmpeg-agent/status.json'

# Унікальний маркер для ідентифікації наших FFmpeg процесів
RFA_AGENT_MARKER = 'RFA_FFMPEG_AGENT'

# ============================================================
# ГЛОБАЛЬНИЙ СТАН
# ============================================================

active_streams = {}
last_heartbeat_time = datetime.now()
agent_running = True

# ============================================================
# УТИЛІТИ
# ============================================================

def ensure_pid_dir():
    Path(PID_DIR).mkdir(parents=True, exist_ok=True)

def write_pid_file(station_id, pid):
    pid_file = os.path.join(PID_DIR, f"ffmpeg-{station_id}.pid")
    with open(pid_file, 'w') as f:
        f.write(str(pid))
    # Додатково записуємо cmdline для перевірки
    cmdline_file = os.path.join(PID_DIR, f"ffmpeg-{station_id}.cmdline")
    with open(cmdline_file, 'w') as f:
        f.write(f"rtp janus {station_id}")

def remove_pid_file(station_id):
    for ext in ['.pid', '.cmdline']:
        f = os.path.join(PID_DIR, f"ffmpeg-{station_id}{ext}")
        if os.path.exists(f):
            os.remove(f)

def update_status_file():
    status = {
        'agent_running': agent_running,
        'last_heartbeat': last_heartbeat_time.isoformat(),
        'active_streams': {}
    }
    for station_id, info in active_streams.items():
        process = info['process']
        status['active_streams'][station_id] = {
            'pid': info['pid'],
            'running': process.poll() is None,
            'start_time': info['start_time'].isoformat(),
            'stream_url': info['stream_url'],
            'rtp_url': info['rtp_url']
        }
    try:
        with open(STATUS_FILE, 'w') as f:
            json.dump(status, f, indent=2)
    except Exception as e:
        log(f"Помилка запису статус-файлу: {e}", 'ERROR')

def log(message, level='INFO'):
    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    prefix = {
        'INFO': 'ℹ️',
        'SUCCESS': '✅',
        'ERROR': '❌',
        'WARN': '⚠️',
        'DEBUG': '🔍'
    }.get(level, 'ℹ️')
    print(f"[{timestamp}] {prefix} {message}", flush=True)

def is_our_ffmpeg_process(pid):
    """
    Перевіряє, чи належить FFmpeg процес нашому агенту.
    Використовує трирівневу перевірку:
    1. PID-файл в нашій директорії
    2. Змінна оточення RFA_AGENT=1
    3. Наявність RTP URL Janus в командному рядку
    """
    try:
        # Перевірка 1: PID-файл
        pid_file = os.path.join(PID_DIR, f"ffmpeg-*.pid")
        for pf in Path(PID_DIR).glob("ffmpeg-*.pid"):
            try:
                with open(pf, 'r') as f:
                    if int(f.read().strip()) == pid:
                        return True
            except:
                pass

        # Перевірка 2: змінна оточення
        environ_file = f"/proc/{pid}/environ"
        if os.path.exists(environ_file):
            with open(environ_file, 'rb') as f:
                environ = f.read().decode('utf-8', errors='ignore')
                if f"{RFA_AGENT_MARKER}=1" in environ:
                    return True

        # Перевірка 3: командний рядок містить RTP URL Janus
        cmdline_file = f"/proc/{pid}/cmdline"
        if os.path.exists(cmdline_file):
            with open(cmdline_file, 'rb') as f:
                cmdline = f.read().decode('utf-8', errors='ignore')
                # Шукаємо RTP URL з портом Janus (наприклад, rtp://10.4.10.110:10002)
                if 'rtp://' in cmdline and 'janus' in cmdline.lower():
                    return True
                # Або перевіряємо payload_type 100 (наш стандарт)
                if 'payload_type' in cmdline and '100' in cmdline:
                    return True

        return False
    except (PermissionError, FileNotFoundError, ProcessLookupError):
        return False

# ============================================================
# STARTUP CLEANUP (БЕЗПЕЧНИЙ)
# ============================================================

def cleanup_orphaned_processes():
    """
    При старті агента завершує ТІЛЬКИ осиротілі FFmpeg процеси,
    які належать цьому агенту. Інші FFmpeg процеси НЕ чіпає.
    """
    log("Перевірка осиротілих FFmpeg процесів (ТІЛЬКИ RFA)...", 'INFO')

    if not os.path.exists(PID_DIR):
        log("PID директорія не існує, очищення не потрібне", 'INFO')
        return

    orphaned_count = 0
    skipped_count = 0

    for pid_file in Path(PID_DIR).glob("ffmpeg-*.pid"):
        try:
            with open(pid_file, 'r') as f:
                pid = int(f.read().strip())

            # Перевіряємо, чи процес існує
            try:
                os.kill(pid, 0)
            except OSError:
                # Процес не існує, просто видаляємо PID-файл
                pid_file.unlink(missing_ok=True)
                cmdline_file = pid_file.with_suffix('.cmdline')
                cmdline_file.unlink(missing_ok=True)
                continue

            # Процес існує - перевіряємо, чи це наш FFmpeg
            if not is_our_ffmpeg_process(pid):
                log(f"⚠️  PID {pid} не належить RFA агенту, пропускаємо", 'WARN')
                skipped_count += 1
                pid_file.unlink(missing_ok=True)
                continue

            # Це наш осиротілий процес - завершуємо
            station_id = pid_file.stem.replace("ffmpeg-", "")
            log(f"Знайдено осиротілий RFA процес: {station_id} (PID: {pid})", 'WARN')

            try:
                os.kill(pid, signal.SIGTERM)
                time.sleep(2)

                try:
                    os.kill(pid, 0)
                    log(f"Примусове завершення PID {pid}", 'WARN')
                    os.kill(pid, signal.SIGKILL)
                except OSError:
                    pass

                orphaned_count += 1
                log(f"Осиротілий RFA процес {station_id} завершено", 'SUCCESS')

            except Exception as e:
                log(f"Помилка завершення PID {pid}: {e}", 'ERROR')

        except (ValueError, OSError) as e:
            log(f"Помилка обробки PID-файлу {pid_file}: {e}", 'ERROR')
        finally:
            pid_file.unlink(missing_ok=True)
            cmdline_file = pid_file.with_suffix('.cmdline')
            cmdline_file.unlink(missing_ok=True)

    if orphaned_count > 0:
        log(f"✅ Завершено {orphaned_count} осиротілих RFA процесів", 'SUCCESS')
    if skipped_count > 0:
        log(f"⏭️  Пропущено {skipped_count} процесів (не RFA)", 'INFO')
    if orphaned_count == 0 and skipped_count == 0:
        log("Осиротілих RFA процесів не знайдено", 'INFO')

# ============================================================
# УПРАВЛІННЯ FFmpeg
# ============================================================

def build_ffmpeg_command(station_id, stream_url, rtp_url, options=None):
    cmd = [
        'ffmpeg',
        '-nostats',
        '-loglevel', 'warning',
        '-re',
        '-i', stream_url,
    ]

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

    cmd.extend([
        '-acodec', acodec,
        '-b:a', bitrate,
        '-ar', str(sample_rate),
        '-ac', str(channels),
        '-application', application,
        '-vbr', vbr,
        '-compression_level', str(compression),
        '-payload_type', str(DEFAULT_PAYLOAD_TYPE),
        '-f', 'rtp',
    ])

    if 'pkt_size' not in rtp_url:
        rtp_url = f"{rtp_url}?pkt_size={DEFAULT_PKT_SIZE}"

    cmd.append(rtp_url)

    return cmd

def monitor_ffmpeg_process(station_id, process):
    try:
        for line in iter(process.stderr.readline, ''):
            if line.strip():
                log(f"[{station_id}] FFmpeg: {line.strip()}", 'DEBUG')

        returncode = process.wait()

        if returncode == 0:
            log(f"[{station_id}] FFmpeg завершився нормально", 'INFO')
        elif returncode == -15:
            log(f"[{station_id}] FFmpeg зупинено за запитом (SIGTERM)", 'INFO')
        else:
            log(f"[{station_id}] FFmpeg завершився з кодом {returncode}", 'ERROR')

        if station_id in active_streams:
            del active_streams[station_id]
            remove_pid_file(station_id)
            update_status_file()

    except Exception as e:
        log(f"[{station_id}] Помилка моніторингу: {e}", 'ERROR')

def start_ffmpeg(station_id, stream_url, rtp_url, options=None):
    if station_id in active_streams:
        process_info = active_streams[station_id]
        if process_info['process'].poll() is None:
            log(f"[{station_id}] Трансляція вже запущена (PID: {process_info['process'].pid})", 'WARN')
            return
        else:
            del active_streams[station_id]
            remove_pid_file(station_id)

    cmd = build_ffmpeg_command(station_id, stream_url, rtp_url, options)

    log(f"[{station_id}] Запуск FFmpeg...", 'INFO')
    log(f"[{station_id}] Команда: {' '.join(cmd)}", 'DEBUG')

    try:
        #  КЛЮЧОВЕ: Додаємо змінну оточення RFA_AGENT=1 для ідентифікації
        env = os.environ.copy()
        env[RFA_AGENT_MARKER] = '1'

        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            env=env  # ← Передаємо модифіковане оточення
        )

        active_streams[station_id] = {
            'process': process,
            'pid': process.pid,
            'start_time': datetime.now(),
            'stream_url': stream_url,
            'rtp_url': rtp_url
        }

        write_pid_file(station_id, process.pid)
        update_status_file()

        log(f"[{station_id}] FFmpeg запущено (PID: {process.pid})", 'SUCCESS')

        monitor_thread = threading.Thread(
            target=monitor_ffmpeg_process,
            args=(station_id, process),
            daemon=True
        )
        monitor_thread.start()

    except FileNotFoundError:
        log(f"[{station_id}] FFmpeg не знайдено!", 'ERROR')
    except Exception as e:
        log(f"[{station_id}] Помилка запуску: {e}", 'ERROR')

def stop_ffmpeg(station_id):
    if station_id not in active_streams:
        log(f"[{station_id}] Активної трансляції не знайдено", 'WARN')
        return

    process_info = active_streams[station_id]
    process = process_info['process']

    log(f"[{station_id}] Зупинка FFmpeg (PID: {process.pid})...", 'INFO')

    try:
        process.terminate()

        try:
            process.wait(timeout=5)
            log(f"[{station_id}] Трансляцію зупинено", 'SUCCESS')
        except subprocess.TimeoutExpired:
            log(f"[{station_id}] Примусова зупинка...", 'WARN')
            process.kill()
            process.wait()
            log(f"[{station_id}] Трансляцію примусово зупинено", 'SUCCESS')

        del active_streams[station_id]
        remove_pid_file(station_id)
        update_status_file()

    except Exception as e:
        log(f"[{station_id}] Помилка зупинки: {e}", 'ERROR')

def stop_all_ffmpeg():
    log("Зупинка всіх RFA трансляцій...", 'INFO')
    for station_id in list(active_streams.keys()):
        stop_ffmpeg(station_id)
    log("Всі RFA трансляції зупинено", 'SUCCESS')

# ============================================================
# HEARTBEAT МЕХАНІЗМ
# ============================================================

def heartbeat_checker():
    global agent_running

    while agent_running:
        time.sleep(HEARTBEAT_CHECK_INTERVAL)

        if not agent_running:
            break

        time_since_heartbeat = datetime.now() - last_heartbeat_time

        if time_since_heartbeat > timedelta(seconds=HEARTBEAT_TIMEOUT):
            log(f"⚠️ HEARTBEAT TIMEOUT! Останній heartbeat: {time_since_heartbeat.seconds} сек тому", 'ERROR')
            log("Автоматична зупинка всіх RFA FFmpeg процесів...", 'WARN')
            stop_all_ffmpeg()
            log("⚠️ Агент перейшов у режим очікування відновлення зв'язку", 'WARN')

# ============================================================
# ОБРОБКА СИГНАЛІВ
# ============================================================

def signal_handler(signum, frame):
    global agent_running

    signal_name = signal.Signals(signum).name
    log(f"Отримано сигнал {signal_name} ({signum})", 'INFO')
    log("Graceful shutdown...", 'INFO')

    agent_running = False
    stop_all_ffmpeg()

    log("Агент зупинено", 'SUCCESS')
    sys.exit(0)

# ============================================================
# ОБРОБКА ПОВІДОМЛЕНЬ RABBITMQ
# ============================================================

def on_message_callback(ch, method, properties, body):
    global last_heartbeat_time

    try:
        data = json.loads(body.decode('utf-8'))
        log(f"Отримано команду: {data}", 'DEBUG')

        command = data.get('command')
        station_id = data.get('stationId')

        if command == 'start':
            stream_url = data.get('streamUrl')
            rtp_url = data.get('rtpUrl')
            options = data.get('ffmpegOptions')

            if not stream_url or not rtp_url:
                log(f"[{station_id}] Відсутні параметри", 'ERROR')
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
                return

            log(f"[{station_id}] START", 'INFO')
            start_ffmpeg(station_id, stream_url, rtp_url, options)

        elif command == 'stop':
            log(f"[{station_id}] STOP", 'INFO')
            stop_ffmpeg(station_id)

        elif command == 'status':
            log(f"STATUS: {len(active_streams)} активних RFA трансляцій", 'INFO')
            for sid, info in active_streams.items():
                log(f"  - {sid}: PID={info['pid']}, running={info['process'].poll() is None}", 'INFO')

        elif command == 'heartbeat':
            last_heartbeat_time = datetime.now()
            log(f"Heartbeat отримано", 'DEBUG')

        else:
            log(f"Невідома команда: {command}", 'WARN')

        ch.basic_ack(delivery_tag=method.delivery_tag)

    except json.JSONDecodeError as e:
        log(f"Помилка JSON: {e}", 'ERROR')
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
    except Exception as e:
        log(f"Помилка обробки: {e}", 'ERROR')
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

# ============================================================
# ТОЧКА ВХОДУ
# ============================================================

def main():
    global agent_running, last_heartbeat_time

    log("=" * 60, 'INFO')
    log("FFmpeg Агент RFA v3.1 (БЕЗПЕЧНИЙ)", 'INFO')
    log("=" * 60, 'INFO')

    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGINT, signal_handler)

    ensure_pid_dir()

    # 🎯 БЕЗПЕЧНИЙ cleanup - завершує тільки RFA процеси
    cleanup_orphaned_processes()

    update_status_file()

    heartbeat_thread = threading.Thread(target=heartbeat_checker, daemon=True)
    heartbeat_thread.start()
    log(f"Heartbeat checker запущено (timeout: {HEARTBEAT_TIMEOUT} сек)", 'INFO')

    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)
    parameters = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        virtual_host=RABBITMQ_VHOST,
        credentials=credentials,
        heartbeat=60,
        blocked_connection_timeout=300
    )

    try:
        log(f"Підключення до RabbitMQ: {RABBITMQ_HOST}...", 'INFO')
        connection = pika.BlockingConnection(parameters)
        channel = connection.channel()

        channel.queue_declare(queue=QUEUE_NAME, durable=True)

        log(f"Агент запущено. Очікування команд у '{QUEUE_NAME}'...", 'SUCCESS')
        log(f"Payload Type: {DEFAULT_PAYLOAD_TYPE}", 'INFO')
        log(f"Packet Size: {DEFAULT_PKT_SIZE}", 'INFO')

        channel.basic_qos(prefetch_count=1)
        channel.basic_consume(queue=QUEUE_NAME, on_message_callback=on_message_callback)

        last_heartbeat_time = datetime.now()

        channel.start_consuming()

    except pika.exceptions.AMQPConnectionError as e:
        log(f"Помилка підключення до RabbitMQ: {e}", 'ERROR')
        sys.exit(1)
    except KeyboardInterrupt:
        log("Зупинка за запитом користувача...", 'INFO')
        agent_running = False
        stop_all_ffmpeg()
        log("Агент зупинено", 'SUCCESS')

if __name__ == '__main__':
    main()
