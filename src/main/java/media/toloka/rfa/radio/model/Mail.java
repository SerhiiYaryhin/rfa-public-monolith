package media.toloka.rfa.radio.model;
// Напевно клас для надсилання пошти
// що він тут робить - уяви не маю.

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Mail {

    @Getter
    @Setter
    public static class HtmlTemplate {
        private String template;
        private Map<String, Object> props;

        public HtmlTemplate(String template, Map<String, Object> props) {
            this.template = template;
            this.props = props;
        }
    }

    private String from;
    private String to;
    private String subject;
    private HtmlTemplate htmlTemplate;
}
