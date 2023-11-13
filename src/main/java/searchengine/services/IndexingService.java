package searchengine.services;

import searchengine.config.Site;
import searchengine.dto.indexation.IndexingResponse;

public interface IndexingService {

    IndexingResponse startIndexing();

    IndexingResponse stopIndexing();

    IndexingResponse indexingOnePage(String url);

    Site isPageFromSiteList(String url);
}
