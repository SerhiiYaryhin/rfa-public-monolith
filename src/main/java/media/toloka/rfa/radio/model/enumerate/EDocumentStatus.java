package media.toloka.rfa.radio.model.enumerate;

public enum EDocumentStatus {

    // TODO Винести найменування статусу документа до application-default.properties для інтернаціоналізації
    // 0-невідомо, 1-завантажено, 2-на розгляді, 3 схвалено, 4 відхилено

    STATUS_UNKNOWN("Невідомо"),
    STATUS_LOADED("Завантажено"),
    STATUS_REVIEW("На розгляді"),
    STATUS_REJECTED("Відхилено"),
    STATUS_APPROVED("Схвалено");


    public final String label;

    private EDocumentStatus(String label) {
        this.label = label;
    }
}