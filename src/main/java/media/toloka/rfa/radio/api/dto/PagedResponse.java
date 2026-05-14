package media.toloka.rfa.radio.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PagedResponse<T> {
    private List<T> content;
    private PageMetadata pageable;
    private int totalPages;
    private long totalElements;
    private boolean last;

    @Data
    @Builder
    public static class PageMetadata {
        private int pageNumber;
        private int pageSize;
    }
}
