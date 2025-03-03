package media.toloka.rfa.radio.newstoradio.model;

public enum ENewsCategory {
    NEWS_CATEGORY_NEWS("Новина"),
    NEWS_ATTENTION("Важливо!"),
    NEWS_LOCAL("Новини громади"),
    NEWS_FREE("Будь що");

    public final String label;
//    public final Boolean rootPage;

    private ENewsCategory(String label) {
        this.label = label;
    }
}
