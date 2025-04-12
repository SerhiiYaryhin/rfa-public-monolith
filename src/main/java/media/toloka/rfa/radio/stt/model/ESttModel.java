package media.toloka.rfa.radio.stt.model;

public enum ESttModel {
    STT_MODEL_TINY("tiny"),
    STT_MODEL_BASE("base"),
    STT_MODEL_SMALL("small"),
    STT_MODEL_MEDIUM("medium"),
    STT_MODEL_LARGE("large"),
    STT_MODEL_TURBO("turbo"),
    STT_MODEL_LARGEV1("large-v1"),
    STT_MODEL_LARGEV2("large-v2"),
    STT_MODEL_LARGEV3("'large-v3'"),
    STT_MODEL_LARGEV3TURBO("large-v3-turbo");

    public final String label;

    private ESttModel(String label) {
        this.label = label;
    }
}
