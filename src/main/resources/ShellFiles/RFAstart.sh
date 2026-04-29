#!/bin/bash
cd /home/rfa/prodact
git switch current
#git switch prodact
git pull

cp ~/prodact/src/main/resources/ShellFiles/* ~/bin/
./gradlew build -x test
/bin/sh -c 'until ping -c1 8.8.8.8; do sleep 5; done;'
/bin/sh -c 'until ping -c4 pg.rfa; do sleep 1; done;'
export QRFA=/rfa
QRFA=/rfa TELEGRAMBOTNAME=TestTolokaMediaBot TELEGRAMBOTKEY=7319689514:AAEwGibd0JOdvqOvZnG0STL_hAGffEqQdCA SPRING_PROFILES_ACTIVE=current,Front,Telegram ./gradlew bootRun --args='--server.port=3080 --spring.datasource.url=jdbc:postgresql://pg.rfa:5432/rfa'
#java -jar -Dspring.profiles.active=default,Front build/libs/RFA-1.0.0.war
