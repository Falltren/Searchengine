package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.services.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final MorphologyService morphologyService;
    private final SiteService siteService;
    private final LemmaService lemmaService;
    private final IndexService indexService;

    private List<String> splitTextIntoLemmas(String query) {
        return new ArrayList<>(morphologyService.collectLemmas(query).keySet());
    }

    private Map<PageEntity, Set<IndexEntity>> findPages(String query, String site) {
        Map<PageEntity, Set<IndexEntity>> result = new HashMap<>();
        for (SiteEntity siteEntity : selectSite(site)) {
            List<LemmaEntity> lemmaEntities = getSortedLemmaEntities(query, siteEntity);
            Map<PageEntity, Set<IndexEntity>> pagesWithIndexes = new HashMap<>();
            for (int i = 0; i < lemmaEntities.size(); i++) {
                List<IndexEntity> indexes = indexService.getIndexesByLemma(lemmaEntities.get(i));
                Map<PageEntity, IndexEntity> pair = new HashMap<>();
                for (IndexEntity index : indexes) {
                    PageEntity pageEntity = index.getPageEntity();
                    pair.put(pageEntity, index);
                    if (i == 0) {
                        pagesWithIndexes.put(pageEntity, Stream.of(index).collect(Collectors.toSet()));
                    }
                }
                pagesWithIndexes = calculateResultMap(pair, pagesWithIndexes);
            }
            result.putAll(pagesWithIndexes);
        }
        return result;
    }


    private Map<PageEntity, Float> calculateRelevance(Map<PageEntity, Set<IndexEntity>> pages) {
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
        if (siteEntityList.isEmpty()) {
            throw new NoSuchElementException("Отсутствуют проиндексированные сайты");
        }
        if (site == null) {
            return siteEntityList;
        } else {
            return List.of(siteService.findSiteByUrl(site).orElseThrow());
        }

    }

    private List<LemmaEntity> getSortedLemmaEntities(String query, SiteEntity siteEntity) {
        List<String> lemmas = splitTextIntoLemmas(query);
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        for (String lemma : lemmas) {
            Optional<LemmaEntity> lemmaEntity = lemmaService.findLemmaEntityByLemmaAndSiteEntity(lemma, siteEntity);
            if (lemmaEntity.isEmpty()) {
                return new ArrayList<>();
            }
            lemmaEntities.add(lemmaEntity.get());
        }
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
        List<SearchData> data = searchResponse.getData();
        for (Map.Entry<PageEntity, Float> entry : pagesResult.entrySet()) {
            SearchData searchData = new SearchData();
            searchData.setUri(entry.getKey().getPath());
            String sitePath = entry.getKey().getSiteEntity().getUrl().substring(0, entry.getKey().getSiteEntity().getUrl().length() - 1);
            searchData.setSite(sitePath);
            searchData.setSiteName(entry.getKey().getSiteEntity().getName());
            searchData.setTitle(getTitleFromPage(entry.getKey()));
            searchData.setRelevance(entry.getValue());
            searchData.setSnippet(getSnippet(entry.getKey().getContent(), query));
            if (data.size() == limit) {
                break;
            }
            data.add(searchData);
        }
        searchResponse.setData(data.stream().skip(offset).collect(Collectors.toList()));
        return searchResponse;
    }

    private String getTitleFromPage(PageEntity pageEntity) {
        String content = pageEntity.getContent();
        return Jsoup.parse(content).title();
    }

    private String getSnippet(String content, String query) {
        String text = Jsoup.parse(content).select("body").text();
        Map<String, Set<String>> wordsWithLemmas = morphologyService.getWordsWithLemmas(text);
        Set<String> lemmas = new HashSet<>(splitTextIntoLemmas(query));
        List<String> words = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : wordsWithLemmas.entrySet()) {
            if (lemmas.containsAll(entry.getValue()) && !entry.getValue().isEmpty()) {
                words.add(entry.getKey());
            }
        }
        String[] textArray = text.split(" ");
        Map<Integer, String> wordsWithPosition = new TreeMap<>();
        for (int i = 0; i < textArray.length; i++) {
            String checkingWord = checkWordInText(textArray[i], words);
            if (!checkingWord.equals("")) {
                String underlineWord = underlineWord(textArray[i], checkingWord);
                wordsWithPosition.put(i, underlineWord);
            }
        }
        return createSnippet(wordsWithPosition, textArray);
    }

    private String checkWordInText(String testedWord, List<String> foundWords) {
        testedWord = morphologyService.cleaningText(testedWord);
        String[] words = testedWord.split(" ");
        for (String word : foundWords) {
            boolean isMatchWord = false;
            if (words.length > 1) {
                List<String> wordsFrom = Arrays.asList(words);
                isMatchWord = foundWords.stream().anyMatch(e1 -> wordsFrom.stream().anyMatch(e1::equals));
            }
            if (testedWord.toLowerCase(Locale.ROOT).equals(word) && words.length == 1
                    || isMatchWord) {
                return word;
            }
        }
        return "";
    }

    private String underlineWord(String word, String checkWord) {
        String result;
        if (word.length() == checkWord.length()) {
            return "<b>" + word + "</b>";
        }
        int start = word.toLowerCase(Locale.ROOT).indexOf(checkWord);
        int end = findNoLetterSymbol(start, word);
        String underlineWord = word.substring(start, end);
        if (start == 0) {
            result = "<b>" + underlineWord + "</b>" + word.substring(end);
        } else {
            result = word.substring(0, start) + "<b>" + underlineWord + "</b>" + word.substring(end);
        }
        return result;
    }

    private int findNoLetterSymbol(int startIndex, String word) {
        for (int i = startIndex; i < word.length(); i++) {
            if (!Character.isLetter(word.charAt(i))) {
                return i;
            }
        }
        return word.length();
    }

    private String createSnippet(Map<Integer, String> wordWithIndex, String[] textArray) {
        StringBuilder stringBuilder = new StringBuilder();
        int lineNumber = 1;
        for (Map.Entry<Integer, String> entry : wordWithIndex.entrySet()) {
            if (lineNumber > 3) {
                break;
            }
            int start = entry.getKey() - 2;
            for (int i = start; i < start + 10; i++) {
                if (i == start + 2) {
                    stringBuilder.append(entry.getValue()).append(" ");
                } else {
                    stringBuilder.append(textArray[i]).append(" ");
                }
            }
            stringBuilder.append("...").append("\n");
            lineNumber++;
        }
        return stringBuilder.toString();
    }
}
