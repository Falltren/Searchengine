package searchengine.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.JsoupConnection;
import searchengine.config.Site;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.services.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Getter
@RequiredArgsConstructor
public class CrawlerService extends RecursiveAction {

    private static volatile boolean isNeedStop;
    private static Date date;
    @Setter
    private String url;
    private final Site site;
    private static final Set<String> uniqueLinks = ConcurrentHashMap.newKeySet();
    private final SiteService siteService;
    private final PageService pageService;
    private final MorphologyService morphologyService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final JsoupConnection jsoupConnection;

    public static void stopCrawler() {
        log.warn("invoking stop indexing");
        isNeedStop = true;
    }

    public static void refreshCrawlerStatus() {
        isNeedStop = false;
    }

    public static boolean getIsNeedStop() {
        return isNeedStop;
    }

    public static Set<String> getUniqueLinks() {
        return uniqueLinks;
    }

    @Override
    protected void compute() {
        List<CrawlerService> crawlerServiceList = new ArrayList<>();
        try {
            Thread.sleep(100);
            if (isNeedStop) {
                return;
            }
            uniqueLinks.add(site.getUrl());
            Connection connection = jsoupConnection.getConnection(url);
            Document document = jsoupConnection.getDocument(connection);
            Elements elements = document.select("body").select("a");
            SiteEntity siteEntity = siteService.findSiteByUrl(site.getUrl()).orElseThrow();
            String content = document.toString();
            PageEntity pageEntity = new PageEntity();
            int statusCode = connection.response().statusCode();
            if (statusCode != 200) {
                content = "";
            }
            pageService.saveNewPage(pageEntity, siteEntity, getRelativeLink(url), connection.response().statusCode(), content);
            log.info("added page with url {}", url);
            String text = document.body().text();
            Map<String, Integer> lemmasFromPage = morphologyService.collectLemmas(morphologyService.cleaningText(text));
            Map<LemmaEntity, Integer> lemmasWithRank = lemmaService.addLemma(lemmasFromPage, siteEntity);
            indexService.addIndex(pageEntity, lemmasWithRank);
            siteEntity.setStatusTime(getCurrentDate());
            if (!isNeedStop) {
                siteService.save(siteEntity);
            }
            for (Element element : elements) {
                String newAbsolutLink = element.absUrl("href");
                if (newAbsolutLink.startsWith(site.getUrl()) && !checkEndsLink(newAbsolutLink)
                        && uniqueLinks.add(newAbsolutLink.toLowerCase())) {
                    CrawlerService task = new CrawlerService(getSite(), siteService, pageService, morphologyService,
                            lemmaService, indexService, jsoupConnection);
                    task.setUrl(newAbsolutLink);
                    task.fork();
                    crawlerServiceList.add(task);
                }
            }
            for (CrawlerService crawlerService : crawlerServiceList) {
                crawlerService.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkEndsLink(String link) {
        if (link.endsWith("html")) {
            return false;
        }
        String regex = "(.*\\.[A-Za-z\\d]{3,4})|(.*#.*)|(.+/?.+=[^/]+)";
        return link.matches(regex);
    }

    public String getRelativeLink(String absoluteLink) {
        return absoluteLink.substring(site.getUrl().length() - 1);
    }

    private static Date getCurrentDate() {
        date = new Date();
        return date;
    }
}


