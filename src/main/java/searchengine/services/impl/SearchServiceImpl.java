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
            if (lemmas.stream().anyMatch(e -> entry.getValue().contains(e))) {
                words.add(entry.getKey());
                lemmas.removeAll(entry.getValue());
            }
        }
        String[] wordsArray = text.split(" ");
        Map<Integer, String> wordsWithPosition = getWordsWithPosition(words, wordsArray);
        Map<Integer, String> wordsForSnippet = removingAdjacentPosition(wordsWithPosition);
//        int countFoundWords = wordsForSnippet.size();
//        StringBuilder stringBuilder = new StringBuilder();
//        for (Map.Entry<Integer, String> entry : wordsForSnippet.entrySet()) {
//            String[] wordArrayLine = getWordsArrayForOneLine(entry, wordsArray, countFoundWords);
//            String line = createWordsLine(wordArrayLine, words);
//            stringBuilder.append(line).append("...").append("\n");
//            System.out.println(line);
//        }
        return concatenateLines(wordsForSnippet, wordsArray, words);
    }

    private String concatenateLines(Map<Integer, String> wordsForSnippet, String[] wordsArray, List<String> words) {
        StringBuilder stringBuilder = new StringBuilder();
        int countFoundWords = Math.min(wordsForSnippet.size(), 3);
        int linesCount = 0;
        for (Map.Entry<Integer, String> entry : wordsForSnippet.entrySet()) {
            if (linesCount >= 3) {
                break;
            }
            String[] wordArrayLine = getWordsArrayForOneLine(entry, wordsArray, countFoundWords);
            String line = createWordsLine(wordArrayLine, words);
            stringBuilder.append(line).append("...").append("\n");
            System.out.println(line);
            linesCount++;
        }
        return stringBuilder.toString();
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
            if (testedWord.toLowerCase(Locale.ROOT).equals(word) && words.length == 1 || isMatchWord) {
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

    private String[] getWordsArrayForOneLine(Map.Entry<Integer, String> wordWithPosition,
                                             String[] wordsArray, int countFoundWords) {
        int startPosition = findStartPositionForLine(wordWithPosition.getKey(), countFoundWords, wordsArray);
        if (countFoundWords == 1) {
            String[] line = new String[36];
            System.arraycopy(wordsArray, startPosition, line, 0, 36);
            return line;
        } else if (countFoundWords == 2) {
            String[] line = new String[18];
            System.arraycopy(wordsArray, startPosition, line, 0, 18);
            return line;
        } else {
            String[] line = new String[12];
            System.arraycopy(wordsArray, startPosition, line, 0, 12);
            return line;
        }
    }

    private String createWordsLine(String[] wordsArrayLine, List<String> foundWords) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String w : wordsArrayLine) {
            String checkingWord = checkWordInText(w, foundWords);
            if (!checkingWord.equals("")) {
                String word = underlineWord(w, checkingWord);
                stringBuilder.append(word).append(" ");
            } else {
                stringBuilder.append(w).append(" ");
            }
        }
        return stringBuilder.toString();
    }

    private int findStartPositionForLine(int position, int countFoundWords, String[] textArray) {
        if (position == 0) {
            return position;
        }
        for (int i = position - 1; i >= 0; i--) {
            if (textArray[i].contains(".")) {
                return i + 1;
            }
            if (countFoundWords == 1 && i <= position - 30) {
                return position;
            } else if (countFoundWords == 2 && i <= position - 15) {
                return position;
            } else if (i <= position - 9) {
                return position;
            }
        }
        return position;
    }

    private Map<Integer, String> getWordsWithPosition(List<String> words, String[] wordsArray) {
        Map<Integer, String> wordsWithPosition = new HashMap<>();
        for (int i = 0; i < wordsArray.length; i++) {
            String checkingWord = checkWordInText(wordsArray[i], words);
            if (!checkingWord.equals("")) {
                wordsWithPosition.put(i, wordsArray[i]);
            }
        }
        return wordsWithPosition;
    }

    private Map<Integer, String> removingAdjacentPosition(Map<Integer, String> wordsWithPosition) {
        Map<Integer, String> result = new TreeMap<>(wordsWithPosition);
        Map.Entry<Integer, String> previous = result.entrySet().stream().findFirst().orElse(null);
        boolean firstCompare = true;
        Iterator<Map.Entry<Integer, String>> iterator = result.entrySet().iterator();
        while (iterator.hasNext()) {
            if (firstCompare) {
                iterator.next();
                firstCompare = false;
                continue;
            }
            Map.Entry<Integer, String> next = iterator.next();
            if (next.getKey() < previous.getKey() + 10) {
                iterator.remove();
            } else {
                previous = next;
            }
        }
        return result;
    }
}
