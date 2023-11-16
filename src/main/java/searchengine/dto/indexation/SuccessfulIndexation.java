package searchengine.dto.indexation;

import lombok.Data;

@Data
public class SuccessfulIndexation implements IndexingResponse {

    private boolean result = true;
}
