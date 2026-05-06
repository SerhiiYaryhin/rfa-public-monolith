package media.toloka.rfa.convertor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditorJsBlock {
    private String type;
    private Object data; // сюди Jackson покладе ImageData, HeaderData тощо
}