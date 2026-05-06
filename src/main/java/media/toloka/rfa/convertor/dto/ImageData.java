package media.toloka.rfa.convertor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageData {
    private ImageFile file;
    private String caption;
    private boolean withBorder = false;
    private boolean withBackground = false;
    private boolean stretched = false;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageFile {
        private String url;
    }
}