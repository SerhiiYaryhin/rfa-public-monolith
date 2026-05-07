package media.toloka.rfa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${media.toloka.rfa.server.globalname:https://rfa.toloka.media}")
    private String siteUrl;

    @ModelAttribute("siteUrl")
    public String getSiteUrl() {
        return siteUrl;
    }
}
