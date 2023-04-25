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
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.concurrent.RecursiveAction;

@Component
@Setter
@Getter
@Scope(value = "prototype")
public class CrawlerService extends RecursiveAction {

    private String url;

    private static Site site;

    private static Set<String> uniqueLinks = Collections.synchronizedSet(new HashSet<>());

    private SiteRepository siteRepository;

    private PageRepository pageRepository;

    public CrawlerService() {
    }

    public CrawlerService(String url, Site site, SiteRepository siteRepository, PageRepository pageRepository) {
        this.url = url;
        CrawlerService.site = site;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    public static void setSite(Site site) {
        CrawlerService.site = site;
    }

    @Autowired
    public void setPageRepository(PageRepository pageRepository){
        this.pageRepository = pageRepository;
    }

    @Autowired
    public void setSiteRepository(SiteRepository siteRepository){
        this.siteRepository = siteRepository;
    }

    public static Set<String> getUniqueLinks() {
        return uniqueLinks;
    }

    @Override
    protected void compute() {
        List<CrawlerService> crawlerServiceList = new ArrayList<>();
        try {
            Thread.sleep(100);
            Connection connection = getConnection(url);
            Document document = connection
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .referrer("http://www.google.com")
                    .get();
            Elements elements = document.select("a");
            for (Element element : elements) {
                String newUrl = element.absUrl("href");
                if (newUrl.startsWith(site.getUrl()) && !checkEndsLink(newUrl) && uniqueLinks.add(newUrl)) {
                    CrawlerService task = new CrawlerService(newUrl, site, siteRepository, pageRepository);
                    task.fork();
                    crawlerServiceList.add(task);
                    SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).get();
                    siteEntity.setStatusTime(new Date());
                    siteRepository.save(siteEntity);
                    PageEntity pageEntity = new PageEntity();
                    pageEntity.setSiteEntity(siteEntity);
                    pageEntity.setPath(newUrl);
                    pageEntity.setCode(connection.response().statusCode());
                    pageEntity.setContent(document.outerHtml());
                    pageRepository.save(pageEntity);
                    System.out.println(newUrl);
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
}


