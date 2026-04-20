package media.toloka.rfa.radio.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Об'єкт-чернетка для універсальної розсилки
 */
@Data
public class EmailDraft {
    /** Список email-адрес отримувачів */
    private List<String> recipients = new ArrayList<>();
    
    /** Тема листа */
    private String subject;
    
    /** Фінальний HTML-контент листа після редагування в TinyMCE */
    private String body;
    
    /** Ім'я обраного шаблону-основи */
    private String selectedTemplate;
}
