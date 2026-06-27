import subprocess
import signal
import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="RFA FFmpeg Orchestrator Agent")

# Внутрішній словник для відстеження активних процесів у пам'яті агента
# Ключ: rtp_port, Значення: Popen об'єкт процесу
active_streams = {}

class StreamRequest(BaseModel):
    stream_url: str  # Потік радіостанції (наприклад, http://icecast.rfa/rock.mp3)
    janus_host: str  # Хост сервера Janus (наприклад, tts.rfa)
    rtp_port: int    # Динамічний UDP порт (наприклад, 10024)

@app.post("/api/stream/start")
def start_stream(req: StreamRequest):
    if req.rtp_port in active_streams:
        raise HTTPException(status_code=400, detail=f"Порт {req.rtp_port} вже використовується цим агентом!")

    # Шаблон команди FFmpeg для перекодування в Opus/RTP без затримок
    cmd = [
        "ffmpeg", "-loglevel", "quiet", "-i", req.stream_url,
        "-acodec", "libopus", "-b:a", "64k", "-vbr", "on", "-ar", "48000", "-ac", "2",
        "-f", "rtp", f"rtp://{req.janus_host}:{req.rtp_port}"
    ]

    try:
        # Запускаємо процес у фоні ОС (asynchronous execution)
        process = subprocess.Popen(cmd, preexec_fn=os.setsid)
        active_streams[req.rtp_port] = process

        return {"success": True, "pid": process.pid, "message": f"FFmpeg запущено для порту {req.rtp_port}"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Не вдалося запустити FFmpeg: {str(e)}")

@app.post("/api/stream/stop/{rtp_port}")
def stop_stream(rtp_port: int):
    if rtp_port not in active_streams:
        return {"success": True, "message": f"Потік на порту {rtp_port} не знайдено або вже зупинено"}

    process = active_streams.pop(rtp_port)
    try:
        # М'яко вбиваємо всю групу процесів FFmpeg через SIGTERM
        os.killpg(os.getpgid(process.pid), signal.SIGTERM)
        process.wait(timeout=2)
        return {"success": True, "message": f"Потік на порту {rtp_port} успішно зупинено"}
    except subprocess.TimeoutExpired:
        # Якщо не зупинився по-хорошому — вбиваємо примусово
        os.killpg(os.getpgid(process.pid), signal.SIGKILL)
        return {"success": True, "message": f"Потік на порту {rtp_port} вбито примусово (SIGKILL)"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Помилка при зупинці процесу: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    # Запускаємо агент на внутрішньому порту 8000
    uvicorn.run(app, host="0.0.0.0", port=8000)