package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.utils.ForkJoinManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;

    private final ObjectProvider<CrawlerService> objectProvider;

    private final SiteService siteService;

    private final PageService pageService;

    private final MorphologyService morphologyService;

    private final CrawlerService crawlerService;

    @Override
    public void startIndexing() {
        new Thread(() -> {
            for (Site site : sitesList.getSites()) {
                ForkJoinPool forkJoinPool = ForkJoinManager.getForkJoinPool();
//                if (CrawlerService.getIsNeedStop()) {
//                    System.out.println("STOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOP!!!!");
//                    forkJoinPool.shutdownNow();
//                    break;
//                }
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
        new Thread(() -> {
            CrawlerService.stopCrawler();
            for (Site site : sitesList.getSites()) {
                SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).orElseThrow();
                if (siteEntity.getStatus().equals(StatusType.INDEXING)) {
                    siteEntity.setStatus(StatusType.FAILED);
                    siteEntity.setStatusTime(new Date());
                    siteEntity.setLastError("Индексация остановлена пользователем.");
                    siteService.save(siteEntity);
                    System.out.println("=================> " + "статус сайта " + site.getUrl() + "изменен на " + siteEntity.getStatus());
                }
            }
        }).start();
    }

    public void indexingOnePage(String url) {
        new Thread(() -> {
            Site site = isPageFromSiteList(url);
            if (site.getUrl() != null) {
                try {
                    Connection connection = Jsoup.connect(url);
                    Document document = connection
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                    "(KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                            .ignoreHttpErrors(true)
                            .ignoreContentType(true)
                            .referrer("http://www.google.com")
                            .get();
                    String contentWithoutHtmlTags = morphologyService.getContentWithoutHtmlTags(document.outerHtml());
                    String content = morphologyService.cleaningText(
                            morphologyService.cleaningText(contentWithoutHtmlTags));
                    SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).get();
                    PageEntity pageEntity = new PageEntity();
                    pageService.saveNewPage(pageEntity, siteEntity, crawlerService.getRelativeLink(url),
                            connection.response().statusCode(), content);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public Site isPageFromSiteList(String url) {
        for (Site site : sitesList.getSites()) {
            if (url.contains(site.getUrl())) {
                return site;
            }
        }
        return new Site();
    }
}
