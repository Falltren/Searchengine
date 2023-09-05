package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.response.FailIndexing;
import searchengine.dto.search.SearchData;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public List<SearchData> createSearch(String query, String site, Integer offset, Integer limit) {
        Map<String, Integer> lemmasMap = getSortedLemmas(site, query);


        return new ArrayList<>();
    }

    public String getRarestLemma(Map<String, Integer> lemmas){
        return lemmas.entrySet().stream().findFirst().toString();
    }

    private List<SiteEntity> selectSite(String site){
        List<SiteEntity> siteEntityList = siteService.getIndexedSites();
        if (siteEntityList.isEmpty()){
            throw new IllegalArgumentException(String.valueOf(new FailIndexing("Отсутствуют проиндексированные сайты")));
        }
        if (site == null){
            return siteEntityList;
        } else {
            return siteEntityList.stream().filter(s -> s.getUrl().contains(site)).collect(Collectors.toList());
        }
    }






}
