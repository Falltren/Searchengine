package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchData {

    private String uri;
    private String title;
    private String snippet;
    private float relevance;
}
