package searchengine.dto.search;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorSearch extends SearchResponse {

    private boolean result;
    private String error;

    public ErrorSearch(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
