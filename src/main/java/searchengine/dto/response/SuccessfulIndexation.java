package searchengine.dto.response;

import lombok.Data;

@Data
public class SuccessfulIndexation extends IndexingResponse {

    private boolean result = true;
}
