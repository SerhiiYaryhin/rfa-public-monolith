#!/bin/bash
# script for TTS/STT
cd ~/rfa
git switch current
git pull
cp ~/rfa/src/main/resources/ShellFiles/TTS/* ~/tts/
cd ~/tts
source ~/tts/tts_env/bin/activate
python3 consumer.py
deactivate

