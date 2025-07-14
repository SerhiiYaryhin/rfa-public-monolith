package media.toloka.rfa.comments.config;


import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToECommentSourceTypeConverter implements Converter<String, ECommentSourceType> {
    @Override
    public ECommentSourceType convert(String source) {
        return ECommentSourceType.fromLabel(source);
    }
}