package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.utils.ForkJoinManager;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;

    //    private final SiteRepository siteRepository;
    private final ObjectProvider<CrawlerService> objectProvider;

    private final SiteService siteService;

    @Override
    public void startIndexing() {
        new Thread(() -> {
            for (Site site : sitesList.getSites()) {
                ForkJoinPool forkJoinPool = ForkJoinManager.getForkJoinPool();
                if (CrawlerService.getIsNeedStop()) {
                    System.out.println("STOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOP!!!!");
                    forkJoinPool.shutdownNow();
                    break;
                }
                CrawlerService crawlerService = objectProvider.getIfUnique();
                assert crawlerService != null;
                crawlerService.setUrl(site.getUrl());
                CrawlerService.setSite(site);
                siteService.deleteSiteByUrl(site.getUrl());
                siteService.saveNewSite(site.getUrl(), site.getName());
                forkJoinPool.invoke(crawlerService);
                SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).orElseThrow();
                if (crawlerService.isDone() && !CrawlerService.getIsNeedStop()) {
                    siteEntity.setStatus(StatusType.INDEXED);
                } else {
                    siteEntity.setStatus(StatusType.FAILED);
                }
                siteEntity.setStatusTime(new Date());
                siteService.save(siteEntity);
                System.out.println("=================> " + "статус сайта " + site.getUrl() + "изменен на " + siteEntity.getStatus());
                System.out.println("Parsing: " + site.getUrl() + " ended");
                System.out.println("Collection size: " + CrawlerService.getUniqueLinks().size());
                System.out.println("Time is " + LocalDateTime.now());
                CrawlerService.getUniqueLinks().clear();
            }
            CrawlerService.refreshCrawlerStatus();
        }).start();
    }


    @Override
    public void stopIndexing() {
        CrawlerService.stopCrawler();
        for (Site site : sitesList.getSites()) {
            SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).orElseThrow();
            if (siteEntity.getStatus().equals(StatusType.INDEXING)) {
                siteEntity.setStatus(StatusType.FAILED);
                siteEntity.setStatusTime(new Date());
                siteService.save(siteEntity);
                System.out.println("=================> " + "статус сайта " + site.getUrl() + "изменен на " + siteEntity.getStatus());
            }
        }
    }
}
