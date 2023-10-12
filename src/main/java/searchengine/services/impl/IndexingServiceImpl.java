package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.FailIndexing;
import searchengine.dto.response.IndexingResponse;
import searchengine.dto.response.SuccessfulIndexation;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.services.*;
import searchengine.utils.ForkJoinManager;
import searchengine.utils.JsoupConnection;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final ObjectProvider<CrawlerService> objectProvider;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final MorphologyService morphologyService;
    private final JsoupConnection jsoupConnection;

    @Override
    public IndexingResponse startIndexing() {
        if (siteService.isIndexing()) {
            return new FailIndexing("Индексация уже запущена");
        }
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
                log.info("статус сайта " + site.getUrl() + " изменен на " + siteEntity.getStatus());
                System.out.println("Collection size: " + CrawlerService.getUniqueLinks().size());
                CrawlerService.getUniqueLinks().clear();
            }
            CrawlerService.refreshCrawlerStatus();
        }).start();
        return new SuccessfulIndexation();
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!siteService.isIndexing()) {
            return new FailIndexing("Индексация не запущена");
        }
        new Thread(() -> {
            CrawlerService.stopCrawler();
            for (Site site : sitesList.getSites()) {
                SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).orElseThrow();
                if (siteEntity.getStatus().equals(StatusType.INDEXING)) {
                    siteEntity.setStatus(StatusType.FAILED);
                    siteEntity.setStatusTime(new Date());
                    siteEntity.setLastError("Индексация остановлена пользователем.");
                    siteService.save(siteEntity);
                    log.info("Статус сайта " + site.getUrl() + "изменен на " + siteEntity.getStatus());
                }
            }
        }).start();
        return new SuccessfulIndexation();
    }

    @Transactional
    public IndexingResponse indexingOnePage(String url) {
        Site site = isPageFromSiteList(url);
        if (site.getUrl() == null) {
            return new FailIndexing("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }
        new Thread(() -> {
            SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).orElseThrow();
            String path = url.substring(site.getUrl().length() - 1); //need changing on service
            pageService.deletePageEntityBySiteEntityAndPath(siteEntity, path);
            try {
                Connection connection = jsoupConnection.getConnection(url);
                Document document = jsoupConnection.getDocument(connection);
                String content = document.toString();
                PageEntity pageEntity = new PageEntity();
                pageService.saveNewPage(pageEntity, siteEntity, path, connection.response().statusCode(), content);
                Map<String, Integer> lemmasFromPage = morphologyService.collectLemmas(morphologyService.cleaningText(content));
                Map<LemmaEntity, Integer> lemmasWithRank = lemmaService.addLemma(lemmasFromPage, siteEntity);
                indexService.addIndex(pageEntity, lemmasWithRank);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return new SuccessfulIndexation();
    }

    public Site isPageFromSiteList(String url) {
        return sitesList.getSites().stream().filter(s -> url.contains(s.getUrl())).findAny().orElse(new Site());
    }
}
