package searchengine.services;

import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface SiteService {

    public void deleteSiteByUrl(String url);

    public Optional<SiteEntity> findSiteByUrl(String url);

    public int findPagesCountByUrl(String url);

    public int findLemmasCountByUrl(String url);

    public void saveNewSite(String url, String name);

    public void save(SiteEntity siteEntity);

    public boolean isIndexing();

    public List<SiteEntity> getIndexedSites();
}
