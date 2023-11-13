package searchengine.dto.response;

import lombok.Data;

@Data
public class Response {

    private final boolean result;
    private final String error;
}
