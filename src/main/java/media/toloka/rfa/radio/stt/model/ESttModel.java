package media.toloka.rfa.radio.stt.model;

public enum ESttModel {
    STT_MODEL_TINY("tyni"),
    STT_MODEL_BASE("base"),
    STT_MODEL_SMALL("small"),
    STT_MODEL_MEDIUM("medium"),
    STT_MODEL_LARGE("large"),
    STT_MODEL_TURBO("turbo");

    public final String label;
//    public final Boolean rootPage;

    private ESttModel(String label) {
        this.label = label;
    }
}
