package media.toloka.rfa.convertor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.*;

@Data
public class EditorJsData {
    private Long time = System.currentTimeMillis();
    private List<EditorJsBlock> blocks = new ArrayList<>();
    private String version = "2.28.2";

    public void addBlock(String type, Map<String, Object> data) {
        this.blocks.add(new EditorJsBlock(type, data));
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class EditorJsBlock {
    private String type;
    private Map<String, Object> data;
}
