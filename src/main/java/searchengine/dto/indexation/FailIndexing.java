package searchengine.dto.indexation;

import lombok.Data;

@Data
public class FailIndexing implements IndexingResponse {
    private final boolean result;
    private final String error;

    public FailIndexing(String error) {
        result = false;
        this.error = error;
    }
}
