#!/bin/bash
echo $1
cd ~/tts/
source ~/tts/tts_env/bin/activate
python3 ~/bin/tts.py $1
ffmpeg -i /tmp/$1.wav /tmp/$1.mp3
deactivate