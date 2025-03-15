#!/bin/bash
cd /home/toradio/rfa
git switch current
git pull

cp ~/rfa/src/main/resources/ShellFiles/toRadio/toRadio.sh ~/bin/
rm ~/bin/py/*
cp ~/rfa/src/main/resources/ShellFiles/toRadio/py/* ~/bin/py
cd ~/bin
source toRadio/bin/activate
python3 toRadio.py
deactivate
