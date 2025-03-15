#!/bin/bash
cd /home/toradio/rfa
git switch current
git pull

cp ~/rfa/src/main/resources/ShellFiles/toRadio/* ~/bin/
cd ~/bin
source toRadio/bin/activate
python3 toRadio.py
deactivate
