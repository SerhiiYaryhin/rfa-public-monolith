package media.toloka.rfa.radio.stt.model;

public enum ESttStatus {
    STT_STATUS_CREATE("Створено"),
    STT_STATUS_SEND("Надіслано"),
    STT_STATUS_DONE("Виконано"),
    STT_STATUS_ERROR("Помилка");

    public final String label;
//    public final Boolean rootPage;

    private ESttStatus(String label) {
        this.label = label;
    }
}
