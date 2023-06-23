package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

@Component
@Setter
@Getter
@Scope(value = "prototype")
public class CrawlerService extends RecursiveAction {

    private static volatile boolean isNeedStop;

    private String url;

    private static Site site;

    private static Set<String> uniqueLinks = ConcurrentHashMap.newKeySet();

    private SiteRepository siteRepository;

    private PageService pageService;

    private MorphologyService morphologyService;

    private LemmaService lemmaService;

    private IndexService indexService;

    public CrawlerService() {
    }

    public CrawlerService(String url, Site site, SiteRepository siteRepository, PageService pageService,
                          MorphologyService morphologyService, LemmaService lemmaService, IndexService indexService) {
        this.url = url;
        CrawlerService.site = site;
        this.siteRepository = siteRepository;
        this.pageService = pageService;
        this.morphologyService = morphologyService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
    }

    public static void setSite(Site site) {
        CrawlerService.site = site;
    }

    public static void stopCrawler() {
        System.out.println("Вызвана остановка парсинга!!!!");
        isNeedStop = true;
    }

    public static void refreshCrawlerStatus() {
        isNeedStop = false;
    }

    public static boolean getIsNeedStop() {
        return isNeedStop;
    }

    @Autowired
    public void setPageRepository(PageService pageService) {
        this.pageService = pageService;
    }

    @Autowired
    public void setSiteRepository(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Autowired
    public void setMorphologyService(MorphologyService morphologyService) {
        this.morphologyService = morphologyService;
    }

    @Autowired
    public void setLemmaService(LemmaService lemmaService) {
        this.lemmaService = lemmaService;
    }

    @Autowired
    public void setIndexService(IndexService indexService) {
        this.indexService = indexService;
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
//                Thread.currentThread().interrupt();
                return;
            }
            Connection connection = getConnection(url).timeout(120000);
            Document document = connection
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .followRedirects(false)
                    .referrer("http://www.google.com")
                    .get();
            Elements elements = document.select("a");
            for (Element element : elements) {
                String newAbsolutLink = element.absUrl("href");
                if (newAbsolutLink.startsWith(site.getUrl()) && !checkEndsLink(newAbsolutLink) && uniqueLinks.add(newAbsolutLink)) {
                    CrawlerService task = new CrawlerService(newAbsolutLink, site, siteRepository, pageService, morphologyService, lemmaService, indexService);
                    task.fork();
                    crawlerServiceList.add(task);
                    SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).orElseThrow();
                    String content = morphologyService.getContentWithoutHtmlTags(document.outerHtml());
                    PageEntity pageEntity = new PageEntity();
                    pageService.saveNewPage(pageEntity, siteEntity, getRelativeLink(newAbsolutLink), connection.response().statusCode(), content);
                    Map<String, Integer> lemmasFromPage = morphologyService.getLemmas(morphologyService.cleaningText(content));
                    Map<LemmaEntity, Integer> lemmasWithRank = lemmaService.addLemma(lemmasFromPage, siteEntity);
                    indexService.addIndex(pageEntity, lemmasWithRank);
                    System.out.println(newAbsolutLink);
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
        String regex = "(.*\\.[A-Za-z\\d]{3,4})|(.*#.*)";
        return link.matches(regex);
    }

    private Connection getConnection(String url) {
        return Jsoup.connect(url);
    }

    public String getRelativeLink(String absoluteLink) {
        return absoluteLink.substring(site.getUrl().length() - 1);
    }
}


