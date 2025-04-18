package media.toloka.rfa.account.model.accEnum;

public enum EAccActivePassive {
    EACC_ACTIVE("Активний"),
    EACC_PASSIVE("Пасивний"),
    EACC_ACTIVE_PASSIVE("Активно-Пасивний");

    public final String label;

    private EAccActivePassive(String label) {
        this.label = label;
    }
}
