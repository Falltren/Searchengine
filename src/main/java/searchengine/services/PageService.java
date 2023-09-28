package searchengine.services;

import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

public interface PageService {
    void saveNewPage(PageEntity pageEntity, SiteEntity siteEntity, String path, int code, String content);

    public void deletePageEntityBySiteEntityAndPath(SiteEntity siteEntity, String path);
}
