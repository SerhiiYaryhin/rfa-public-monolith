package media.toloka.rfa.radio.newstoradio.model;

public enum ENewsVoice {
    NEWS_VOICE_DMYTRO("Дмитро"),
    NEWS_VOICE_LADA("Лада"),
    NEWS_VOICE_MYKYTA("Микита"),
    NEWS_VOICE_OLEKSA("Олекса"),
    NEWS_VOICE_TETIANA("Тетяна");

    public final String label;
//    public final Boolean rootPage;

    private ENewsVoice(String label) {
        this.label = label;
    }
}
