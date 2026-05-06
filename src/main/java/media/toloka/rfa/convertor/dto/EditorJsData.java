package media.toloka.rfa.convertor.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class EditorJsData {
    private Long time = System.currentTimeMillis();
    private List<EditorJsBlock> blocks = new ArrayList<>(); // Ініціалізація списку
    private String version = "2.28.2";

    // Додаємо цей метод для вирішення помилки
    public void addBlock(String type, Object data) {
        this.blocks.add(new EditorJsBlock(type, data));
    }
}