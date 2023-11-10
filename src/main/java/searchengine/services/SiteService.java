package searchengine.services;

import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;

import java.util.List;
import java.util.Optional;

public interface SiteService {

    void deleteSiteByUrl(String url);

    Optional<SiteEntity> findSiteByUrl(String url);

    int findPagesCountByUrl(String url);

    int findLemmasCountByUrl(String url);

    void saveNewSite(String url, String name);

    void save(SiteEntity siteEntity);

    boolean isIndexing();

    List<SiteEntity> getIndexedSites();

    void stopIndexing(SiteEntity siteEntity);

    StatusType getStatus(Site site);
}
