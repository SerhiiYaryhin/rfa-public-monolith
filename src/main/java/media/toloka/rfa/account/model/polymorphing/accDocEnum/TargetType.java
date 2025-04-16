package media.toloka.rfa.account.model.polymorphing.accDocEnum;


import media.toloka.rfa.account.model.Documents.AccInFlowToBankDocument;
import media.toloka.rfa.account.model.Documents.AccOrderedWorkDocument;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;
import media.toloka.rfa.account.model.AccInvoice;

public enum TargetType {
//    USER("FLOWTOBANK", "Приход від клієнта", AccInFlowToBankDocument.class),
//    INVOICE("INVOICE", "Рахунок клієнту", AccInvoice.class),
//    PROJECT("ORDERWORK", "Замовлення роботи", AccOrderedWorkDocument.class);
//
//    private final String code;
//    private final String doctypename;
//    private final Class<? extends PolymorphicTarget> clazz;
//
//    TargetType(String code, String doctypename, Class<? extends PolymorphicTarget> clazz) {
//        this.code = code;
//        this.doctypename = doctypename;
//        this.clazz = clazz;
//    }

//    public String getCode() {
//        return code;
//    }
//
//    public Class<? extends PolymorphicTarget> getClazz() {
//        return clazz;
//    }

//    // ✅ Пошук по класу
//    public static String getCodeForClass(Class<? extends PolymorphicTarget> clazz) {
//        for (TargetType type : values()) {
//            if (type.getClazz().equals(clazz)) return type.getCode();
//        }
//        throw new IllegalArgumentException("Unknown class: " + clazz);
//    }
//
//    // ✅ Пошук по коду
//    public static Class<? extends PolymorphicTarget> getClassForCode(String code) {
//        for (TargetType type : values()) {
//            if (type.getCode().equals(code)) return type.getClazz();
//        }
//        throw new IllegalArgumentException("Unknown type code: " + code);
//    }
}
