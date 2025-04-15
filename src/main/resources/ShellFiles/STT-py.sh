#!/bin/bash
# script for STT
# ssh-copy-id git@gate.rfa
# git clone git@gate.rfa:~/rfa
cd ~/rfa
git switch prodact
git pull
cp ~/rfa/src/main/resources/ShellFiles/TTS/* ~/stt/
cd ~/stt
source ~/stt/whisper/bin/activate
python3 consumer.py
deactivate

