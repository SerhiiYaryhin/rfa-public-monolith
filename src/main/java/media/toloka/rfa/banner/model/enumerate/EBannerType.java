package media.toloka.rfa.banner.model.enumerate;

public enum EBannerType {
    BANNER_TYPE_TEXT("TEXT"),
    BANNER_TYPE_VIDEO("VIDEO"),
    BANNER_TYPE_AUDIO("AUDIO"),
    BANNER_TYPE_IMAGE("IMAGE");

    public final String label;

    private EBannerType(String label) {
        this.label = label;
    }
}

