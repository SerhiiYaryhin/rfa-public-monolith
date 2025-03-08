#!/bin/bash
echo $1
cd ~/tts/
source ~/tts/tts_env/bin/activate
rm /tmp/$1.wav
python3 ~/bin/tts.py $1
rm /tmp/$1.mp3
ffmpeg -i /tmp/$1.wav /tmp/$1.mp3
deactivate