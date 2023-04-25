package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.Date;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final ObjectProvider<CrawlerService> objectProvider;

    private final SiteService siteService;


    @Override
    public void startIndexing() {
        for (Site site : sitesList.getSites()) {
            CrawlerService crawlerService = objectProvider.getIfUnique();
            assert crawlerService != null;
            crawlerService.setUrl(site.getUrl());
            CrawlerService.setSite(site);
            siteService.deleteSiteByUrl(site.getUrl());
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setUrl(site.getUrl());
            siteEntity.setName(site.getName());
            siteEntity.setStatusTime(new Date());
            siteEntity.setStatus(StatusType.INDEXING);
            siteRepository.save(siteEntity);
            new ForkJoinPool().invoke(crawlerService);
            if (crawlerService.isDone()) {
                siteEntity.setStatus(StatusType.INDEXED);
            } else {
                siteEntity.setStatus(StatusType.FAILED);
            }
            System.out.println("Parsing: " + site.getUrl() + " ended");
            System.out.println("Collection size: " + CrawlerService.getUniqueLinks().size());
            CrawlerService.getUniqueLinks().clear();
        }
    }


    @Override
    public void stopIndexing() {

    }


}
