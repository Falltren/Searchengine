package searchengine.dto.search;

import lombok.Data;

@Data
public class ErrorSearch implements SearchResponse {

    private boolean result;
    private String error;

    public ErrorSearch(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
