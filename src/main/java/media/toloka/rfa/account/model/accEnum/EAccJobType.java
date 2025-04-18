package media.toloka.rfa.account.model.accEnum;

public enum EAccJobType {
    EACCJOBTYPE_ClientPayment("Оплата від користувача"),
    EACCJOBTYPE_TransferToUserAccount("Поповнення рахунку користувача"),
    EACCJOBTYPE_PaymentSTT("Оплата послуги голос в текст"),
    EACCJOBTYPE_PaymentTTS("Оплата послуги текст в голос"),
    EACCJOBTYPE_PaymentStation("Оплата роботи радіостанції"),
    EACCJOBTYPE_Returning_unused("Повернення невикористаних ресурсів клієнту"),
    EACCJOBTYPE_Charity("Благодійність на потреби Толоки"),
    EACCJOBTYPE_TestPperiod("Тестування сервісу"),
    EACCJOBTYPE_Reserv1("Резерв 1"),
    EACCJOBTYPE_Reserv2("Резерв 2"),
    EACCJOBTYPE_Reserv3("Резерв 3"),
    EACCJOBTYPE_Reserv4("Резерв 4"),
    EACCJOBTYPE_Reserv5("Резерв 5"),
    EACCJOBTYPE_LASTTYPE("Останній");

    public final String label;

    private EAccJobType(String label) {
        this.label = label;
    }
}
