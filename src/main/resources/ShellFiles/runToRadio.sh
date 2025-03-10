#!/bin/bash

echo $TORADIOSERVER
echo $TORADIOUSER
echo $TORADIOPASSWORD
echo $TORADIOPORT
echo $NEWSUUID

ffmpeg -re -i https://front.rfa.toloka.media/store/audio/$NEWSUUID -f mp3 icecast://$TORADIOSERVER:$TORADIOUSER@$TORADIOSERVER:$TORADIOPORT/main

