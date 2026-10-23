#!/bin/bash
# ==============================================================================
# check_efir_rooms.sh (v4)
# Ключова зміна: ваш Janus повертає відповіді СИНХРОННО в тілі POST-запитів,
# а long-poll GET віддає 403. Тому v4 читає відповіді прямо з POST і не робить GET.
# Додатково: FIX=1 + TOKEN=<JWT> — викликає REST /api/v1/broadcast/sync/{stationUuid},
# щоб перестворити в Janus кімнати, яких там немає (після рестарту Janus).
# ==============================================================================

# --------------------------- 1. НАЛАШТУВАННЯ ----------------------------------
JANUS_HOST="${JANUS_HOST:-localhost}"
JANUS_API_PORT="${JANUS_API_PORT:-8088}"
JANUS_ADMIN_PORT="${JANUS_ADMIN_PORT:-7088}"
JANUS_API_SECRET="${JANUS_API_SECRET:-janusrocks}"
JANUS_ADMIN_SECRET="${JANUS_ADMIN_SECRET:-janusoverlord}"

DB_HOST="${DB_HOST:-pg.rfa}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-postgres}"
DB_PASS="${DB_PASS:-}"
DB_NAME="${DB_NAME:-rfa}"

# Режим лікування: FIX=1 TOKEN=<JWT> ./check_efir_rooms.sh
FIX="${FIX:-0}"
BASE_URL="${BASE_URL:-https://rfa.toloka.media}"
STATION_UUID="${STATION_UUID:-e5044caa-6eba-4af7-9f95-788b1dd238a9}"
TOKEN="${TOKEN:-}"

CURL_TIMEOUT="${CURL_TIMEOUT:-10}"
POLL_TIMEOUT="${POLL_TIMEOUT:-10}"
RT_GREEN=5; RT_BROADCAST=6; RT_STAFF=7

C_GREEN='\033[0;32m'; C_RED='\033[0;31m'; C_YELLOW='\033[1;33m'; C_BLUE='\033[0;34m'; C_NC='\033[0m'
JANUS_API="http://${JANUS_HOST}:${JANUS_API_PORT}/janus"
JANUS_ADMIN="http://${JANUS_HOST}:${JANUS_ADMIN_PORT}/admin"
SID=""

trx()  { echo "sh-$1-$(date +%s%N)-$RANDOM"; }
jpost() { # $1=URL $2=JSON — синхронний POST, відповідь одразу в тілі
    curl -s -m "${CURL_TIMEOUT}" -H 'Content-Type: application/json' -X POST "$1" -d "$2"
}
cleanup() {
    if [ -n "$SID" ]; then
        jpost "${JANUS_API}/${SID}" \
          "{\"janus\":\"destroy\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"$(trx destroy)\"}" > /dev/null 2>&1
    fi
}
trap cleanup EXIT

for cmd in curl jq; do
    command -v "$cmd" > /dev/null 2>&1 || { echo -e "${C_RED}Немає утиліти $cmd${C_NC}"; exit 1; }
done

echo -e "${C_BLUE}==============================================================${C_NC}"
echo -e "${C_BLUE} ДІАГНОСТИКА КІМНАТ ПРЯМОГО ЕФІРУ (v4, синхронні POST)${C_NC}"
echo -e "${C_BLUE}==============================================================${C_NC}"

# --------------------------- СЕКЦІЯ 1. ADMIN API ------------------------------
echo -e "\n${C_YELLOW}[1/4] Janus Admin API...${C_NC}"
PING=$(jpost "${JANUS_ADMIN}" "{\"janus\":\"ping\",\"admin_secret\":\"${JANUS_ADMIN_SECRET}\",\"transaction\":\"$(trx ping)\"}")
[ "$(echo "$PING" | jq -r '.janus // empty')" = "pong" ] \
    && echo -e "  ${C_GREEN}✔ pong${C_NC}" || echo -e "  ${C_RED}✘ немає pong: ${PING}${C_NC}"
jpost "${JANUS_ADMIN}" "{\"janus\":\"info\",\"admin_secret\":\"${JANUS_ADMIN_SECRET}\",\"transaction\":\"$(trx info)\"}" \
  | jq -r '"  Версія: \(.["version_string"]//"?") | Активних сесій: \(.sessions//0)"' 2>/dev/null

# ------------------- СЕКЦІЯ 2. КІМНАТИ ТА УЧАСНИКИ У JANUS --------------------
echo -e "\n${C_YELLOW}[2/4] Аудіокімнати Janus AudioBridge (синхронні POST)...${C_NC}"
ROOMS_JSON="[]"

# 2.1. Створення тимчасової сесії (відповідь синхронна)
R1=$(jpost "${JANUS_API}" "{\"janus\":\"create\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"$(trx create)\"}")
SID=$(echo "$R1" | jq -r '.data.id // .session_id // empty')
if [ -z "$SID" ]; then
    echo -e "  ${C_RED}✘ Не вдалося створити сесію: ${R1}${C_NC}";
else
    echo -e "  ${C_GREEN}✔ Сесія: ${SID}${C_NC}"

    # 2.2. Attach до AudioBridge (відповідь синхронна: data.id = handle_id)
    T2=$(trx attach)
    R2=$(jpost "${JANUS_API}/${SID}" "{\"janus\":\"attach\",\"plugin\":\"janus.plugin.audiobridge\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"${T2}\"}")
    HID=$(echo "$R2" | jq -r '.data.id // .handle_id // empty')
    if [ -z "$HID" ]; then
        echo -e "  ${C_RED}✘ Attach не пройшов: ${R2}${C_NC}"
    else
        echo -e "  ${C_GREEN}✔ Handle: ${HID}${C_NC}"

        # 2.3. Список усіх кімнат AudioBridge
        R3=$(jpost "${JANUS_API}/${SID}/${HID}" \
            "{\"janus\":\"message\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"$(trx list)\",\"body\":{\"request\":\"list\"}}")
        ROOMS_JSON=$(echo "$R3" | jq -c '.plugindata.data.list // []')
        echo -e "  ${C_GREEN}✔ Аудіокімнат у Janus: $(echo "$ROOMS_JSON" | jq 'length')${C_NC}\n"

        # 2.4. Деталі кожної кімнати + учасники
        echo "$ROOMS_JSON" | jq -c '.[]' | while read -r ROOM; do
            RID=$(echo "$ROOM" | jq -r '.room')
            echo -e "  ${C_BLUE}--- Кімната Janus ID: ${RID} ---${C_NC}"
            echo "$ROOM" | jq '.' | sed 's/^/    /'
            RP=$(jpost "${JANUS_API}/${SID}/${HID}" \
                "{\"janus\":\"message\",\"apisecret\":\"${JANUS_API_SECRET}\",\"transaction\":\"$(trx part)\",\"body\":{\"request\":\"listparticipants\",\"room\":${RID}}}")
            PARTS=$(echo "$RP" | jq -c '.plugindata.data.participants // []')
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

# --------------------------- СЕКЦІЯ 3. БАЗА ДАНИХ -------------------------------
echo -e "\n${C_YELLOW}[3/4] Кімнати в БД (ordinal: 5=GREEN, 6=BROADCAST, 7=STAFF)...${C_NC}"
DB_IDS=""; MISSING=""
if command -v psql > /dev/null 2>&1; then
    export PGPASSWORD="${DB_PASS}"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -P pager=off -c "
        SELECT id, uuid, roomname,
               CASE roomtype WHEN 5 THEN 'GREEN' WHEN 6 THEN 'BROADCAST' WHEN 7 THEN 'STAFF'
                    ELSE roomtype::text END AS type,
               room_online_status AS online
        FROM message_room
        WHERE roomtype IN (${RT_GREEN},${RT_BROADCAST},${RT_STAFF}) ORDER BY id;"
    # ID кімнат ефіру з БД (для звірки)
    DB_IDS=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -A -c "
        SELECT id FROM message_room WHERE roomtype IN (${RT_GREEN},${RT_BROADCAST},${RT_STAFF});")
else
    echo -e "  ${C_YELLOW}psql не знайдено — секцію БД пропущено.${C_NC}"
fi

# ----------------------- СЕКЦІЯ 4. ПЕРЕХРЕСНА ЗВІРКА ----------------------------
echo -e "\n${C_YELLOW}[4/4] Перехресна звірка (БД ↔ Janus)...${C_NC}"
JANUS_IDS=$(echo "$ROOMS_JSON" | jq -r '.[].room' 2>/dev/null)
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
        echo "$DB_IDS" | grep -qx "$id" \
            || echo -e "  ${C_YELLOW}⚠ ID=${id}: є в Janus, але немає в БД (осиротіла).${C_NC}"
    done
fi

# ------------------- СЕКЦІЯ 5 (ОПЦІЙНО). ЛІКУВАННЯ (FIX=1) ---------------------
if [ "$FIX" = "1" ] && [ -n "$MISSING" ]; then
    echo -e "\n${C_YELLOW}[FIX] Знайдено відсутні кімнати в Janus:${MISSING}${C_NC}"
    if [ -z "$TOKEN" ]; then
        echo -e "  ${C_RED}Вкажіть TOKEN (JWT), щоб викликати REST sync: FIX=1 TOKEN=eyJ... ./check_efir_rooms.sh${C_NC}"
    else
        echo -e "  Виклик POST ${BASE_URL}/api/v1/broadcast/sync/${STATION_UUID} ..."
        SYNC_RESP=$(curl -s -m 30 -X POST "${BASE_URL}/api/v1/broadcast/sync/${STATION_UUID}" \
            -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json")
        echo "$SYNC_RESP" | jq . 2>/dev/null || echo "$SYNC_RESP"
    fi
fi

echo -e "\n${C_BLUE}==============================================================${C_NC}"
echo -e "${C_BLUE} Готово. Тимчасову сесію Janus знищено автоматично.${C_NC}"
echo -e "${C_BLUE}==============================================================${C_NC}"
exit 0