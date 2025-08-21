package media.toloka.rfa.banner.model.enumerate;

public enum EBannerType {
    TEXT("TEXT"),
    VIDEO("VIDEO"),
    AUDIO("AUDIO"),
    IMAGE("IMAGE");

    public final String label;

    private EBannerType(String label) {
        this.label = label;
    }
}

