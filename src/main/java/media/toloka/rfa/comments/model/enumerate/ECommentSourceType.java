package media.toloka.rfa.comments.model.enumerate;

public enum ECommentSourceType {
    COMMENT_POST("POST"),
    COMMENT_COLUMN("COLUMN"),
    COMMENT_TRACK("TRACK"),
    COMMENT_PODCAST("PODCAST"),
    COMMENT_EPISODE("EPISODE"),
    COMMENT_RESERV1("RESERV1"),
    COMMENT_RESERV2("RESERV2"),
    COMMENT_RESERV3("RESERV3"),
    COMMENT_RESERV4("RESERV4"),
    COMMENT_RESERV5("RESERV5");

    public final String label;

    private ECommentSourceType(String label) {
        this.label = label;
    }

    public static ECommentSourceType fromLabel(String label) {
        for (ECommentSourceType type : values()) {
            if (type.label.equalsIgnoreCase(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown label: " + label);
    }
}
