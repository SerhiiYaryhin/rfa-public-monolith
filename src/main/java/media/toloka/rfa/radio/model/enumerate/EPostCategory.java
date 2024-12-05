package media.toloka.rfa.radio.model.enumerate;

public enum EPostCategory {
    POST_ANONCE("Анонс",false),
    POST_MUSIC("Музична стаття"),
    POST_GRANTS("Гранти"),
    POST_OTG("Новини громади"),
    POST_NEWS("Новина сайту", true),
    POST_FREE("Будь що");

    public final String label;
    public final Boolean rootPage;

    private EPostCategory(String label) {
        this.label = label;
        this.rootPage = false;


    }
    private EPostCategory(String label, Boolean root) {
        this.label = label;
        this.rootPage = root;
    }
}
