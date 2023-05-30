package searchengine.dto.response;

import lombok.Data;

@Data
public class FailIndexing extends IndexingResponse {
    private final boolean result;
    private final String error;

    public FailIndexing(String error){
        result = false;
        this.error = error;
    }
}
