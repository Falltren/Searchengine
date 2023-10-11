package searchengine.services;

import searchengine.dto.search.SearchResponse;

public interface SearchService {

    SearchResponse searching(String query, String site, Integer offset, Integer limit);
}
