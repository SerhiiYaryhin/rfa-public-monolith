package media.toloka.rfa.radio.model.enumerate;

public enum EAlbumStatus {
    DRAFT("Чернетка"),
    PENDING("На модерації"),
    APPROVED("Схвалено"),
    REJECTED("Відхилено");

    public final String label;

    private EAlbumStatus(String label) {
        this.label = label;
    }
}