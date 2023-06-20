package searchengine.services;

import searchengine.config.Site;

public interface IndexingService {

    void startIndexing();

    void stopIndexing();

    void indexingOnePage(String url);

    Site isPageFromSiteList(String url);
}
