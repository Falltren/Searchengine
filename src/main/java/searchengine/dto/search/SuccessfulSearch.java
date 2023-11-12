package searchengine.dto.search;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SuccessfulSearch extends SearchResponse {

    private boolean result;
    private int count;
    private List<SearchData> data = new ArrayList<>();
}
