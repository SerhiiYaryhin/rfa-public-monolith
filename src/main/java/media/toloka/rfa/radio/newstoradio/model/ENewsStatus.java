package media.toloka.rfa.radio.newstoradio.model;

public enum ENewsStatus {
    NEWS_STATUS_CREATE("Створено"),
    NEWS_STATUS_SEND("Надіслано"),
    NEWS_STATUS_READY("Готово"),
    NEWS_STATUS_DONE("Виконано"),
    NEWS_STATUS_ERROR("Помилка");

    public final String label;
//    public final Boolean rootPage;

    private ENewsStatus(String label) {
        this.label = label;
    }
}
