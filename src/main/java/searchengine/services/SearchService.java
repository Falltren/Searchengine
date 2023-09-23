package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final MorphologyService morphologyService;
    private final SiteService siteService;
    private final LemmaService lemmaService;

    private final IndexService indexService;

    public List<String> splitTextIntoLemmas(String query) {
        return new ArrayList<>(morphologyService.getLemmas(query).keySet());
    }

    public Map<String, Integer> getSortedLemmas(String site, String text) {
        SiteEntity siteEntity = siteService.findSiteByUrl(site + "/").orElseThrow();
        Map<String, Integer> lemmasMap = lemmaService.findLemmasList(siteEntity, splitTextIntoLemmas(text))
                .stream()
                .collect(Collectors.toMap(LemmaEntity::getLemma, LemmaEntity::getFrequency));
        return lemmasMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public List<PageEntity> findPages(String query, String site) {
        List<PageEntity> pageEntities = new ArrayList<>();
        for (SiteEntity siteEntity : selectSite(site)) {
            List<String> lemmas = splitTextIntoLemmas(query);
            List<LemmaEntity> lemmaEntities = lemmaService.findLemmasList(siteEntity, lemmas);
            lemmaEntities.sort(Comparator.comparing(LemmaEntity::getFrequency));
            List<IndexEntity> indexEntities = indexService.getIndexesByLemma(lemmaEntities.get(0));
            pageEntities = indexEntities
                    .stream()
                    .map(IndexEntity::getPageEntity)
                    .collect(Collectors.toList());
            for (int i = 1; i < lemmaEntities.size(); i++) {
                List<IndexEntity> indexes = indexService.getIndexesByLemma(lemmaEntities.get(i));
                List<PageEntity> pages = indexes.stream().map(IndexEntity::getPageEntity).collect(Collectors.toList());
                pageEntities = pageEntities.stream().filter(pages::contains).collect(Collectors.toList());
            }
        }
        return pageEntities;
    }

    public void calculateRelevance(String query, String site) {
        List<PageEntity> pageEntities = findPages(query, site);
        if (pageEntities.size() > 0) {

        }
    }

    private List<SiteEntity> selectSite(String site) {
        List<SiteEntity> siteEntityList = siteService.getIndexedSites();
        if (siteEntityList.isEmpty() || siteService.findSiteByUrl(site).orElseThrow().getStatus() != StatusType.INDEXED) {
            throw new IllegalArgumentException("Отсутствуют проиндексированные сайты");
        }
        if (site == null) {
            return siteEntityList;
        } else {
            return siteEntityList.stream().filter(s -> s.getUrl().contains(site)).collect(Collectors.toList());
        }
    }
}
