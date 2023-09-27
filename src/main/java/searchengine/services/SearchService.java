package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public Map<PageEntity, Set<IndexEntity>> findPages(String query, String site) {
        Map<PageEntity, Set<IndexEntity>> result = new HashMap<>();
        for (SiteEntity siteEntity : selectSite(site)) {
            List<LemmaEntity> lemmaEntities = getSortedLemmaEntities(query, siteEntity);
            for (int i = 0; i < lemmaEntities.size(); i++) {
                Map<PageEntity, IndexEntity> pair = new HashMap<>();
                List<IndexEntity> indexes = indexService.getIndexesByLemma(lemmaEntities.get(i));
                for (IndexEntity index : indexes) {
                    PageEntity pageEntity = index.getPageEntity();
                    pair.put(pageEntity, index);
                    if (i == 0) {
                        result.put(pageEntity, Stream.of(index).collect(Collectors.toSet()));
                    }
                }
                result = calculateResultMap(pair, result);
            }
        }
        return result;
    }


    public Map<PageEntity, Float> calculateRelevance(Map<PageEntity, Set<IndexEntity>> pages) {
        Map<PageEntity, Float> relevance = new HashMap<>();
        if (pages.size() > 0) {
            float maxAbsoluteRelevance = 0;
            for (Map.Entry<PageEntity, Set<IndexEntity>> entry : pages.entrySet()) {
                float sumRelevance = (float) entry.getValue().stream().mapToDouble(IndexEntity::getRank).sum();
                relevance.put(entry.getKey(), sumRelevance);
                if (sumRelevance > maxAbsoluteRelevance) {
                    maxAbsoluteRelevance = sumRelevance;
                }
            }
            float finalMaxAbsoluteRelevance = maxAbsoluteRelevance;
            relevance = relevance.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / finalMaxAbsoluteRelevance));
        }
        return relevance;
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

    private List<LemmaEntity> getSortedLemmaEntities(String query, SiteEntity siteEntity) {
        List<String> lemmas = splitTextIntoLemmas(query);
        List<LemmaEntity> lemmaEntities = lemmaService.findLemmasList(siteEntity, lemmas);
        Collections.sort(lemmaEntities, Comparator.comparing(LemmaEntity::getFrequency));
        return lemmaEntities;
    }

    private Map<PageEntity, Set<IndexEntity>> calculateResultMap(Map<PageEntity, IndexEntity> from,
                                                                 Map<PageEntity, Set<IndexEntity>> to) {
        Map<PageEntity, Set<IndexEntity>> result = new HashMap<>();
        for (Map.Entry<PageEntity, Set<IndexEntity>> entry : to.entrySet()) {
            if (from.containsKey(entry.getKey())) {
                Set<IndexEntity> indexEntities = entry.getValue();
                indexEntities.add(from.get(entry.getKey()));
                result.put(entry.getKey(), indexEntities);
            }
        }
        return result;
    }

    private Map<PageEntity, Float> getSortedPagesByRelevance(Map<PageEntity, Float> pages) {
        return pages.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public SearchResponse searching(String query, String site, Integer offset, Integer limit) {
        Map<PageEntity, Set<IndexEntity>> pagesWithIndexes = findPages(query, site);
        Map<PageEntity, Float> pagesWithRelevance = calculateRelevance(pagesWithIndexes);
        Map<PageEntity, Float> pagesResult = getSortedPagesByRelevance(pagesWithRelevance);
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        if (limit > 0) {
            searchResponse.setCount(pagesResult.keySet().size());
        } else {
            searchResponse.setCount(limit);
        }
        for (Map.Entry<PageEntity, Float> entry : pagesResult.entrySet()) {
            SearchData searchData = new SearchData();
            searchData.setUri(entry.getKey().getPath());
            String sitePath = entry.getKey().getSiteEntity().getUrl().substring(0, entry.getKey().getSiteEntity().getUrl().length() - 1);
            searchData.setSite(sitePath);
            searchData.setSiteName(entry.getKey().getSiteEntity().getName());
            searchData.setTitle(entry.getKey().getPath());
            searchData.setRelevance(entry.getValue());
            searchData.setSnippet("some snippet");
            List<SearchData> data = searchResponse.getData();
            if (data.size() == limit) {
                break;
            }
            data.add(searchData);
        }
        return searchResponse;
    }
}
