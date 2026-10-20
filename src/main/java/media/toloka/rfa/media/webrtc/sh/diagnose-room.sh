#!/bin/bash

# ============================================================================
# Діагностика параметрів кімнати та RTP-учасника
# ============================================================================

echo "🔍 Діагностика кімнати з трансляцією радіо"
echo "============================================"
echo ""

# 1. Отримання JSESSIONID через логін
echo "📝 Отримання токена сесії..."
LOGIN_RESPONSE=$(curl -s -c - "https://rfa.toloka.media/api/v1q/webrtc/stream/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"ysv@toloka.kiev.ua","password":"2B154Lar"}')

if [ -z "$LOGIN_RESPONSE" ]; then
    echo "❌ Не вдалося отримати токен. Перевірте URL та облікові дані."
    exit 1
fi

# Витягуємо JSESSIONID з cookies
JSESSIONID=$(echo "$LOGIN_RESPONSE" | grep JSESSIONID | awk '{print $NF}')

if [ -z "$JSESSIONID" ]; then
    echo "❌ JSESSIONID не знайдено у відповіді."
    echo "Відповідь сервера:"
    echo "$LOGIN_RESPONSE"
    exit 1
fi

echo "✅ Токен отримано: ${JSESSIONID:0:20}..."
echo ""

# 2. Отримання списку всіх кімнат
echo "📡 Отримання списку кімнат..."
ROOMS_DATA=$(curl -s "https://rfa.toloka.media/api/v1q/webrtc/stream/rooms" \
  -H "Cookie: JSESSIONID=$JSESSIONID")

if [ -z "$ROOMS_DATA" ]; then
    echo "❌ Не вдалося отримати дані кімнат."
    exit 1
fi

# 3. Пошук кімнат з активною трансляцією (stationstate = true)
echo ""
echo "🔴 Кімнати з активною трансляцією:"
echo "=================================="
echo "$ROOMS_DATA" | jq -r '.[] | select(.stationstate == true) | "  📻 Кімната: \(.name) (ID: \(.id))"'

# 4. Для кожної активної кімнати показуємо детальну інформацію
echo ""
echo "📊 Детальна інформація про активні кімнати:"
echo "============================================"

ACTIVE_ROOMS=$(echo "$ROOMS_DATA" | jq -r '.[] | select(.stationstate == true) | .id')

for ROOM_ID in $ACTIVE_ROOMS; do
    echo ""
    echo "🎙️  Кімната ID: $ROOM_ID"
    echo "----------------------------------------"

    # Загальна інформація про кімнату
    echo "$ROOMS_DATA" | jq -r ".[] | select(.id == $ROOM_ID) | \"  📛 Назва: \(.name)\n  🔴 Трансляція: \(.stationstate)\n  👥 Учасників: \(.participants | length)\""

    # Список учасників
    echo ""
    echo "  👤 Учасники:"
    echo "$ROOMS_DATA" | jq -r ".[] | select(.id == $ROOM_ID) | .participants[] | \"    - \(.display) (ID: \(.id))\""

    # Детальна інформація про RTP-учасника "Радіо Ефір"
    echo ""
    echo "  📻 Деталі RTP-учасника 'Радіо Ефір':"
    echo "    ----------------------------------------"
    RADIO_PARTICIPANT=$(echo "$ROOMS_DATA" | jq ".[] | select(.id == $ROOM_ID) | .participants[] | select(.display == \"Радіо Ефір\")")

    if [ -z "$RADIO_PARTICIPANT" ] || [ "$RADIO_PARTICIPANT" = "null" ]; then
        echo "    ⚠️  Учасник 'Радіо Ефір' НЕ знайдений у кімнаті!"
        echo "    Це означає, що Plain RTP учасник не приєднався."
    else
        echo "$RADIO_PARTICIPANT" | jq -r "
          \"    🆔 ID: \(.id)\n\" +
          \"    🎙️  Muted: \(.muted)\n\" +
          \"    🗣️  Talking: \(.talking // \"N/A\")\n\" +
          \"    ⚙️  Setup: \(.setup)\n\" +
          \"    🔊 Audio Level: \(.audio_level // \"N/A\")\n\" +
          \"    📡 RTP Port: \(.rtp_port // \"N/A\")\n\" +
          \"    🌐 IP: \(.ip // \"N/A\")\n\" +
          \"    🎵 Codec: \(.codec // \"N/A\")\n\" +
          \"    🔢 Payload Type: \(.pt // \"N/A\")\"
        "

        # Перевірка критичних параметрів
        echo ""
        echo "    🔍 Аналіз стану:"
        MUTED=$(echo "$RADIO_PARTICIPANT" | jq -r '.muted')
        TALKING=$(echo "$RADIO_PARTICIPANT" | jq -r '.talking // "null"')
        SETUP=$(echo "$RADIO_PARTICIPANT" | jq -r '.setup')

        if [ "$MUTED" = "true" ]; then
            echo "    ❌ Учасник ЗАМ'ЮЧЕНИЙ! Це причина відсутності звуку."
        else
            echo "    ✅ Учасник НЕ зам'ючений"
        fi

        if [ "$TALKING" = "true" ]; then
            echo "    ✅ Janus детектить аудіо активність (talking=true)"
        elif [ "$TALKING" = "false" ]; then
            echo "    ⚠️  Janus НЕ детектить аудіо активність (talking=false)"
            echo "       Це означає, що RTP пакети або не доходять, або містять тишу."
        else
            echo "    ℹ️  Talking status недоступний"
        fi

        if [ "$SETUP" = "true" ]; then
            echo "    ✅ Учасник налаштований (setup=true)"
        else
            echo "    ⚠️  Учасник НЕ налаштований (setup=false)"
        fi
    fi
done

echo ""
echo "============================================"
echo "✅ Діагностика завершена"
