package media.toloka.rfa.account.model;

import com.google.gson.annotations.Expose;
import media.toloka.rfa.radio.model.Clientdetail;

import java.util.Date;

public class FatherDocuments {
    @Expose
    private String name; // найменування документу
    @Expose
    private Clientdetail client; // клієнт
    @Expose
    private Clientdetail operator; // оператор
    @Expose
    private AccTemplateTransaction accTT; // типова операція
    @Expose
    private Date docCreate; // дата документа
    @Expose
    private Date docoperation; // дата проводки
//    @Expose
//    @Expose
//    @Expose
}
