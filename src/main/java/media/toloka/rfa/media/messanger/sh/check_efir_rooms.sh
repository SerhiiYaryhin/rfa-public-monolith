#!/bin/bash
# ==============================================================================
# check_efir_rooms.sh (v5) — діагностика ТА лікування кімнат прямого ефіру.
#
# Можливості:
#   - Звірка кімнат Janus AudioBridge проти таблиці message_room (PostgreSQL).
#   - FIX=1          : створити в Janus усі кімнати, що є в БД, але відсутні
#                      в Janus (з permanent=true, щоб переживали рестарт Janus).
#   - ORPHAN_CLEAN=1 : знищити в Janus кімнати, яких немає в БД (осиротілі,
#                      напр. демо-кімната 1234). УВАГА: permanent-кімнати з
#                      конфігурації повернуться після рестарту Janus — щоб
#                      прибрати їх назавжди, видаліть їх з
#                      /etc/janus/janus.plugin.audiobridge.jcfg
#
# Безпека: скрипт створює власну тимчасову сесію Janus і гарантовано знищує її
# при виході (trap EXIT). Нічого іншого в системі він не змінює (крім FIX/
# ORPHAN_CLEAN, які вмикаються лише прапорцями).
#
# Використання:
#   ./check_efir_rooms.sh                     # лише діагностика
#   FIX=1 ./check_efir_rooms.sh               # діагностика + долікувати кімнати
#   ORPHAN_CLEAN=1 ./check_efir_rooms.sh      # діагностика + прибрати осиротілі
# ==============================================================================

# --------------------------- 1. НАЛАШТУВАННЯ ----------------------------------
JANUS_HOST="${JANUS_HOST:-janus.rfa}"
JANUS_API_PORT="${JANUS_API_PORT:-8088}"
JANUS_ADMIN_PORT="${JANUS_ADMIN_PORT:-7088}"
JANUS_API_SECRET="${JANUS_API_SECRET:-janusrocks}"
JANUS_ADMIN_SECRET="${JANUS_ADMIN_SECRET:-janusoverlord}"

DB_HOST="${DB_HOST:-pg.rfa}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-rfa}"
DB_PASS="${DB_PASS:-toloka2_2018}"
DB_NAME="${DB_NAME:-rfa}"

# Режими лікування (за замовчуванням вимкнені)
FIX="${FIX:-0}"
ORPHAN_CLEAN="${ORPHAN_CLEAN:-0}"

# Параметри створення кімнат у Janus (узгоджені з JanusRoomService.java):
#   sampling_rate 16000 + allow_rtp_participants=true — для прийому Plain RTP
#   permanent=true — кімната зберігається у конфігурації і переживає рестарт
JANUS_SAMPLING_RATE="${JANUS_SAMPLING_RATE:-16000}"

CURL_TIMEOUT="${CURL_TIMEOUT:-10}"

# Ordinal-значення enum EChatRoomType (Hibernate зберігає enum як число!):
#   5 = CHATROOM_TYPE_GREEN, 6 = CHATROOM_TYPE_BROADCAST, 7 = CHATROOM_TYPE_STAFF
RT_GREEN=5; RT_BROADCAST=6; RT_STAFF=7

# Кольори терміналу
C_GREEN='\033[0;32m'; C_RED='\033[0;31m'; C_YELLOW='\033[1;33m'
C_BLUE='\033[0;34m'; C_NC='\033[0m'

JANUS_API="http://${JANUS_HOST}:${JANUS_API_PORT}/janus"
JANUS_ADMIN="http://${JANUS_HOST}:${JANUS_ADMIN_PORT}/admin"

SID=""   # ID тимчасової сесії Janus (для cleanup)
HID=""   # ID тимчасового хендла AudioBridge

# --------------------------- 2. ДОПОМІЖНІ ФУНКЦІЇ ------------------------------

# Генератор унікальних transaction-ідентифікаторів (Janus корелює ними відповіді)
trx() { echo "sh-$1-$(date +%s%N)-$RANDOM"; }

# Синхронний POST-запит до Janus (у вашій конфігурації відповіді приходять
# одразу в тілі POST, без long-poll)
jpost() { # $1=URL, $2=JSON
    curl -s -m "${CURL_TIMEOUT}" -H 'Content-Type: application/json' -X POST "$1" -d "$2"
}

# Безпечне знищення тимчасової сесії при виході зі скрипта
cleanup() {
    if [ -n "$SID" ]; then
        jpost "${JANUS_API}/${SID}" \
            "{\"janus\":\"destroy\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"$(trx destroy)\"}" \
            > /dev/null 2>&1
    fi
}
trap cleanup EXIT

# Перевірка обов'язкових залежностей
for cmd in curl jq psql; do
    if ! command -v "$cmd" > /dev/null 2>&1; then
        echo -e "${C_RED}ПОМИЛКА: не знайдено утиліту '$cmd'. Встановіть її.${C_NC}"
        exit 1
    fi
done

# Обгортка над psql з єдиними параметрами підключення
dbq() { # $1=SQL
    PGPASSWORD="${DB_PASS}" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        -P pager=off "$@"
}

echo -e "${C_BLUE}==============================================================${C_NC}"
echo -e "${C_BLUE} ДІАГНОСТИКА КІМНАТ ПРЯМОГО ЕФІРУ (v5)${C_NC}"
echo -e "${C_BLUE} Janus: ${JANUS_HOST}:${JANUS_API_PORT} | БД: ${DB_HOST}:${DB_PORT}/${DB_NAME}${C_NC}"
echo -e "${C_BLUE} Режими: FIX=${FIX} ORPHAN_CLEAN=${ORPHAN_CLEAN}${C_NC}"
echo -e "${C_BLUE}==============================================================${C_NC}"

# --------------------------- СЕКЦІЯ 1. ADMIN API ------------------------------
echo -e "\n${C_YELLOW}[1/4] Janus Admin API...${C_NC}"
PING_RESP=$(jpost "${JANUS_ADMIN}" \
    "{\"janus\":\"ping\",\"admin_secret\":\"${JANUS_ADMIN_SECRET}\",\"transaction\":\"$(trx ping)\"}")
if [ "$(echo "$PING_RESP" | jq -r '.janus // empty')" = "pong" ]; then
    echo -e "  ${C_GREEN}✔ Admin API відповідає (pong).${C_NC}"
else
    echo -e "  ${C_RED}✘ Admin API не відповів: ${PING_RESP:-<порожньо>}${C_NC}"
fi
jpost "${JANUS_ADMIN}" \
    "{\"janus\":\"info\",\"admin_secret\":\"${JANUS_ADMIN_SECRET}\",\"transaction\":\"$(trx info)\"}" \
    | jq -r '"  Версія: \(.["version_string"]//"?") | Активних сесій: \(.sessions//0)"' 2>/dev/null

# ------------------- СЕКЦІЯ 2. КІМНАТИ ТА УЧАСНИКИ У JANUS --------------------
echo -e "\n${C_YELLOW}[2/4] Аудіокімнати Janus AudioBridge...${C_NC}"
ROOMS_JSON="[]"

# 2.1. Тимчасова сесія
CREATE_RESP=$(jpost "${JANUS_API}" \
    "{\"janus\":\"create\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"$(trx create)\"}")
SID=$(echo "$CREATE_RESP" | jq -r '.data.id // .session_id // empty')
if [ -z "$SID" ]; then
    echo -e "  ${C_RED}✘ Не вдалося створити сесію: ${CREATE_RESP:-<порожньо>}${C_NC}"
else
    echo -e "  ${C_GREEN}✔ Сесія: ${SID}${C_NC}"

    # 2.2. Тимчасовий хендл плагіна AudioBridge
    ATTACH_RESP=$(jpost "${JANUS_API}/${SID}" \
        "{\"janus\":\"attach\",\"plugin\":\"janus.plugin.audiobridge\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"$(trx attach)\"}")
    HID=$(echo "$ATTACH_RESP" | jq -r '.data.id // .handle_id // empty')
    if [ -z "$HID" ]; then
        echo -e "  ${C_RED}✘ Attach не пройшов: ${ATTACH_RESP:-<порожньо>}${C_NC}"
    else
        echo -e "  ${C_GREEN}✔ Handle: ${HID}${C_NC}"

        # 2.3. Повний список кімнат плагіна
        LIST_RESP=$(jpost "${JANUS_API}/${SID}/${HID}" \
            "{\"janus\":\"message\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"$(trx list)\",\"body\":{\"request\":\"list\"}}")
        ROOMS_JSON=$(echo "$LIST_RESP" | jq -c '.plugindata.data.list // []')
        echo -e "  ${C_GREEN}✔ Аудіокімнат у Janus: $(echo "$ROOMS_JSON" | jq 'length')${C_NC}\n"

        # 2.4. Деталі кожної кімнати + список учасників
        echo "$ROOMS_JSON" | jq -c '.[]' | while read -r ROOM; do
            RID=$(echo "$ROOM" | jq -r '.room')
            echo -e "  ${C_BLUE}--- Кімната Janus ID: ${RID} ---${C_NC}"
            echo "$ROOM" | jq '.' | sed 's/^/    /'
            PART_RESP=$(jpost "${JANUS_API}/${SID}/${HID}" \
                "{\"janus\":\"message\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"$(trx part)\",\"body\":{\"request\":\"listparticipants\",\"room\":${RID}}}")
            PARTS=$(echo "$PART_RESP" | jq -c '.plugindata.data.participants // []')
            PCNT=$(echo "$PARTS" | jq 'length')
            if [ "$PCNT" -eq 0 ]; then
                echo -e "    Учасників: ${C_YELLOW}0${C_NC}"
            else
                echo -e "    Учасників: ${C_GREEN}${PCNT}${C_NC}"
                echo "$PARTS" | jq -r '.[] | "      • id=\(.id) display=\(.display // "?") muted=\(.muted // "?") talking=\(.talking // "?")"' | sed 's/^/    /'
            fi
            echo ""
        done
    fi
fi

# --------------------------- СЕКЦІЯ 3. БАЗА ДАНИХ ------------------------------
echo -e "\n${C_YELLOW}[3/4] Кімнати в БД (ordinal: 5=GREEN, 6=BROADCAST, 7=STAFF)...${C_NC}"
dbq -c "
    SELECT id, uuid, roomname,
           CASE roomtype WHEN 5 THEN 'GREEN' WHEN 6 THEN 'BROADCAST' WHEN 7 THEN 'STAFF'
                ELSE roomtype::text END AS type,
           room_online_status AS online
    FROM message_room
    WHERE roomtype IN (${RT_GREEN},${RT_BROADCAST},${RT_STAFF})
    ORDER BY id;"

# ID кімнат ефіру з БД (для звірки) та мапа id|назва (для FIX-режиму)
DB_IDS=$(dbq -t -A -c "
    SELECT id FROM message_room
    WHERE roomtype IN (${RT_GREEN},${RT_BROADCAST},${RT_STAFF});")
DB_MAP=$(dbq -t -A -F '|' -c "
    SELECT id, roomname FROM message_room
    WHERE roomtype IN (${RT_GREEN},${RT_BROADCAST},${RT_STAFF});")

# ----------------------- СЕКЦІЯ 4. ПЕРЕХРЕСНА ЗВІРКА ---------------------------
echo -e "\n${C_YELLOW}[4/4] Перехресна звірка (БД ↔ Janus)...${C_NC}"
JANUS_IDS=$(echo "$ROOMS_JSON" | jq -r '.[].room' 2>/dev/null)
MISSING=""    # кімнати, що є в БД, але немає в Janus
ORPHANS=""    # кімнати, що є в Janus, але немає в БД

if [ -z "$DB_IDS" ] && [ -z "$JANUS_IDS" ]; then
    echo -e "  ${C_YELLOW}Немає даних для звірки.${C_NC}"
else
    for id in $DB_IDS; do
        if echo "$JANUS_IDS" | grep -qx "$id"; then
            echo -e "  ${C_GREEN}✔ ID=${id}: є в БД і є в Janus.${C_NC}"
        else
            echo -e "  ${C_RED}✘ ID=${id}: є в БД, але ВІДСУТНЯ в Janus.${C_NC}"
            MISSING="${MISSING} ${id}"
        fi
    done
    for id in $JANUS_IDS; do
        if echo "$DB_IDS" | grep -qx "$id"; then
            :
        else
            echo -e "  ${C_YELLOW}⚠ ID=${id}: є в Janus, але немає в БД (осиротіла).${C_NC}"
            ORPHANS="${ORPHANS} ${id}"
        fi
    done
fi

# --------------- СЕКЦІЯ 5 (FIX). СТВОРЕННЯ ВІДСУТНІХ КІМНАТ --------------------
if [ "$FIX" = "1" ] && [ -n "$MISSING" ]; then
    echo -e "\n${C_YELLOW}[FIX] Створюємо відсутні кімнати в Janus:${MISSING}${C_NC}"
    if [ -z "$SID" ] || [ -z "$HID" ]; then
        echo -e "  ${C_RED}Немає активної сесії/хендла Janus — FIX неможливий.${C_NC}"
    else
        while IFS='|' read -r RID RNAME; do
            [ -z "$RID" ] && continue
            # Створюємо лише ті кімнати, яких немає в Janus
            echo "$MISSING" | grep -qw "$RID" || continue
            BODY=$(jq -n \
                --argjson room "$RID" \
                --arg desc "$RNAME" \
                --argjson rate "$JANUS_SAMPLING_RATE" \
                '{request:"create", room:$room, description:$desc,
                  permanent:true, sampling_rate:$rate,
                  allow_rtp_participants:true}')
            MSG=$(jq -n \
                --argjson sid "$SID" --argjson hid "$HID" \
                --arg tr "$(trx fix)" --arg secret "$JANUS_API_SECRET" \
                --argjson body "$BODY" \
                '{janus:"message", apisecret:$secret, transaction:$tr,
                  session_id:$sid, handle_id:$hid, body:$body}')
            RESP=$(jpost "${JANUS_API}/${SID}/${HID}" "$MSG")
            ERR=$(echo "$RESP" | jq -r '.error.reason // .plugindata.data.error // empty')
            OK=$(echo "$RESP" | jq -r '.plugindata.data.audiobridge // empty')
            if [ -n "$ERR" ]; then
                echo -e "  ${C_RED}✘ Кімната ${RID}: помилка створення: ${ERR}${C_NC}"
            else
                echo -e "  ${C_GREEN}✔ Кімната ${RID} (${RNAME}) створена [${OK}] permanent=true${C_NC}"
            fi
        done <<< "$DB_MAP"
        echo -e "  ${C_YELLOW}Порада: перезапустіть діагностику, щоб підтвердити створення.${C_NC}"
    fi
fi

# ------------- СЕКЦІЯ 6 (ORPHAN_CLEAN). ЗНИЩЕННЯ ОСИРОТІЛИХ КІМНАТ -------------
if [ "$ORPHAN_CLEAN" = "1" ] && [ -n "$ORPHANS" ]; then
    echo -e "\n${C_YELLOW}[ORPHAN_CLEAN] Знищуємо осиротілі кімнати:${ORPHANS}${C_NC}"
    echo -e "  ${C_YELLOW}УВАГА: permanent-кімнати з конфігурації повернуться після${C_NC}"
    echo -e "  ${C_YELLOW}рестарту Janus. Щоб прибрати назавжди — видаліть їх з${C_NC}"
    echo -e "  ${C_YELLOW}/etc/janus/janus.plugin.audiobridge.jcfg${C_NC}"
    if [ -z "$SID" ] || [ -z "$HID" ]; then
        echo -e "  ${C_RED}Немає активної сесії/хендла Janus — очищення неможливе.${C_NC}"
    else
        for RID in $ORPHANS; do
            BODY=$(jq -n --argjson room "$RID" '{request:"destroy", room:$room}')
            MSG=$(jq -n \
                --argjson sid "$SID" --argjson hid "$HID" \
                --arg tr "$(trx clean)" --arg secret "$JANUS_API_SECRET" \
                --argjson body "$BODY" \
                '{janus:"message", apisecret:$secret, transaction:$tr,
                  session_id:$sid, handle_id:$hid, body:$body}')
            RESP=$(jpost "${JANUS_API}/${SID}/${HID}" "$MSG")
            ERR=$(echo "$RESP" | jq -r '.error.reason // .plugindata.data.error // empty')
            if [ -n "$ERR" ]; then
                echo -e "  ${C_RED}✘ Кімната ${RID}: помилка знищення: ${ERR}${C_NC}"
            else
                echo -e "  ${C_GREEN}✔ Кімната ${RID} знищена.${C_NC}"
            fi
        done
    fi
fi

echo -e "\n${C_BLUE}==============================================================${C_NC}"
echo -e "${C_BLUE} Готово. Тимчасову сесію Janus знищено автоматично.${C_NC}"
echo -e "${C_BLUE}==============================================================${C_NC}"
exit 0