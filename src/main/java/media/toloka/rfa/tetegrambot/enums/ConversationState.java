package media.toloka.rfa.tetegrambot.enums;

public enum ConversationState {
    CONVERSATION_STARTED,
    WAITING_FOR_CITY,
    WAITING_FOR_TEXT,
    WAITING_CD_FOR_TELEGRAM_LINK_UUID,
    WAITING_SEND_TRACK,
    // Процес реєстрації
    WAITING_LOGIN_FNAME, // Create Login Step 1
    WAITING_LOGIN_SNAME, // Create Login Step 2
    WAITING_LOGIN_FIRMNAME, // Create Login Step 3
    WAITING_LOGIN_PASSWORD, // Create Login Step 4
    WAITING_LOGIN_EMAIL, // Create Login Step 5
    WAITING_LOGIN_CHECK_INFOMATION, // Create Login Step 6
    WAITING_LOGIN_CHECK_REPLY // Create Login Step 7
    // Кінець процесу Реєстрації
}
