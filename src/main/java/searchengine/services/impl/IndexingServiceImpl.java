package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexation.FailIndexing;
import searchengine.dto.indexation.IndexingResponse;
import searchengine.dto.indexation.SuccessfulIndexation;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.services.*;
import searchengine.utils.CrawlerService;
import searchengine.config.JsoupConnection;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final MorphologyService morphologyService;
    private final JsoupConnection jsoupConnection;
    private final ForkJoinPool forkJoinPool;

    @Override
    public IndexingResponse startIndexing() {
        log.info("indexing of all sites has started");
        CrawlerService.refreshCrawlerStatus();
        if (siteService.isIndexing()) {
            return new FailIndexing("Индексация уже запущена");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(sitesList.getSites().size());
        for (Site site : sitesList.getSites()) {
            executorService.execute(() -> {
                if (CrawlerService.getIsNeedStop()) {
                    return;
                }
                siteService.deleteSiteByUrl(site.getUrl());
                siteService.createNewSite(site.getUrl(), site.getName());
                CrawlerService crawlerService = new CrawlerService(site, siteService, pageService, morphologyService,
                        lemmaService, indexService, jsoupConnection);
                crawlerService.setUrl(site.getUrl());
                forkJoinPool.invoke(crawlerService);
                changeSiteEntityStatus(site, crawlerService);
                log.info("статус сайта {} изменен на {}", site.getUrl(), siteService.getStatus(site));
            });
            CrawlerService.getUniqueLinks().clear();
        }
        executorService.shutdown();
        return new SuccessfulIndexation();
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!siteService.isIndexing()) {
            return new FailIndexing("Индексация не запущена");
        }
        CrawlerService.stopCrawler();
        for (Site site : sitesList.getSites()) {
            SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).orElseThrow();
            if (siteEntity.getStatus().equals(StatusType.INDEXING)) {
                siteService.stopIndexing(siteEntity);
            }
        }
        return new SuccessfulIndexation();
    }

    public IndexingResponse indexingOnePage(String url) {
        Site site = isPageFromSiteList(url);
        if (site.getUrl() == null) {
            return new FailIndexing("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }
        new Thread(() -> {
            SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).orElseThrow();
            String path = getPagePath(site, url);
            pageService.deletePageEntity(siteEntity, path);
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

    private void changeSiteEntityStatus(Site site, CrawlerService crawlerService) {
        SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).orElseThrow();
        if (crawlerService.isDone() && !CrawlerService.getIsNeedStop()) {
            siteEntity.setStatus(StatusType.INDEXED);
            siteService.save(siteEntity);
        }
    }

    private String getPagePath(Site site, String url) {
        return url.substring(site.getUrl().length() - 1);
    }
}
