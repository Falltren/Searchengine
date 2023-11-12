package searchengine.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FailIndexing extends IndexingResponse {
    private final boolean result;
    private final String error;

    public FailIndexing(String error) {
        result = false;
        this.error = error;
    }
}
