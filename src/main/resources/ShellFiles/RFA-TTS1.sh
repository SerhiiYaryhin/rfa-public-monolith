#!/bin/bash
# script for TTS/STT
cd ~/rfa
git switch prodact
git pull
cp ~/rfa/src/main/resources/ShellFiles/* ~/bin/
./gradlew build -x test
#/bin/sh -c 'until ping -c1 8.8.8.8; do sleep 5; done;'
#/bin/sh -c 'until ping -c4 pg.rfa; do sleep 1; done;'
export QRFA=/rfa
export TELEGRAMBOTNAME=TestTolokaMediaBot
export TELEGRAMBOTKEY=7319689514:AAEwGibd0JOdvqOvZnG0STL_hAGffEqQdCA
java -jar -Dspring.profiles.active=current,tts build/libs/RFA-1.0.0.war
#java -jar -Dspring.profiles.active=current build/libs/RFA-1.0.0.war