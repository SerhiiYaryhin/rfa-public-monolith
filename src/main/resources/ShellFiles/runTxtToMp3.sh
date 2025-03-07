#!/bin/bash
cd ~/tts/
~/tts/tts_env/bin/activate
pyton3 tts.py $1
ffmpeg -i /tmp/$1.wav $1.mp3
deactivate