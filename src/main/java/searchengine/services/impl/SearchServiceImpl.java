package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.search.ErrorSearch;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SuccessfulSearch;
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
        Map<PageEntity, Set<IndexEntity>> foundPages = new HashMap<>();
        for (SiteEntity siteEntity : selectSite(site)) {
            List<LemmaEntity> lemmaEntities = getSortedLemmaEntities(query, siteEntity);
            Map<PageEntity, Set<IndexEntity>> pagesWithIndexes = new HashMap<>();
            for (int i = 0; i < lemmaEntities.size(); i++) {
                List<IndexEntity> indexes = indexService.getIndexesByLemma(lemmaEntities.get(i));
                Map<PageEntity, IndexEntity> pair = new HashMap<>();
                addPages(indexes, pair, pagesWithIndexes, i);
                pagesWithIndexes = getMatchesPages(pair, pagesWithIndexes);
            }
            foundPages.putAll(pagesWithIndexes);
        }
        return foundPages;
    }

    private void addPages(List<IndexEntity> indexes, Map<PageEntity, IndexEntity> pair,
                          Map<PageEntity, Set<IndexEntity>> pageWithIndexes, int index) {
        for (IndexEntity indexEntity : indexes) {
            PageEntity pageEntity = indexEntity.getPageEntity();
            pair.put(pageEntity, indexEntity);
            if (index == 0) {
                pageWithIndexes.put(pageEntity, Stream.of(indexEntity).collect(Collectors.toSet()));
            }
        }
    }

    private Map<PageEntity, Float> calculateRelevancePages(Map<PageEntity, Set<IndexEntity>> pages) {
        Map<PageEntity, Float> relevance = new HashMap<>();
        float maxAbsoluteRelevance = 0f;
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
        List<LemmaEntity> sortedLemmaEntities = new ArrayList<>();
        for (String lemma : lemmas) {
            Optional<LemmaEntity> lemmaEntity = lemmaService.findLemmaEntityByLemmaAndSiteEntity(lemma, siteEntity);
            if (lemmaEntity.isEmpty()) {
                return new ArrayList<>();
            }
            sortedLemmaEntities.add(lemmaEntity.get());
        }
        sortedLemmaEntities.sort(Comparator.comparing(LemmaEntity::getFrequency));
        return sortedLemmaEntities;
    }

    private Map<PageEntity, Set<IndexEntity>> getMatchesPages(Map<PageEntity, IndexEntity> from,
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
        if (query.isBlank()) {
            return new ErrorSearch(false, "Задан пустой поисковый запрос");
        }
        Map<PageEntity, Set<IndexEntity>> pagesWithIndexes = findPages(query, site);
        Map<PageEntity, Float> pagesWithRelevance = calculateRelevancePages(pagesWithIndexes);
        Map<PageEntity, Float> pagesResult = getSortedPagesByRelevance(pagesWithRelevance);
        SuccessfulSearch successfulSearch = new SuccessfulSearch();
        successfulSearch.setResult(true);
        if (limit > 0) {
            successfulSearch.setCount(pagesResult.keySet().size());
        } else {
            successfulSearch.setCount(limit);
        }
        List<SearchData> data = successfulSearch.getData();
        for (Map.Entry<PageEntity, Float> entry : pagesResult.entrySet()) {
            SearchData searchData = createSearchDataObject(entry, query);
            if (data.size() == limit) {
                break;
            }
            data.add(searchData);
        }
        successfulSearch.setData(data.stream().skip(offset).collect(Collectors.toList()));
        return successfulSearch;
    }

    private SearchData createSearchDataObject(Map.Entry<PageEntity, Float> entry, String query) {
        SearchData searchData = new SearchData();
        searchData.setUri(entry.getKey().getPath());
        String sitePath = getSitePath(entry.getKey());
        searchData.setSite(sitePath);
        searchData.setSiteName(entry.getKey().getSiteEntity().getName());
        searchData.setTitle(getTitleFromPage(entry.getKey()));
        searchData.setRelevance(entry.getValue());
        searchData.setSnippet(getSnippet(entry.getKey().getContent(), query));
        return searchData;
    }

    private String getTitleFromPage(PageEntity pageEntity) {
        String content = pageEntity.getContent();
        return Jsoup.parse(content).title();
    }

    private String getSnippet(String content, String query) {
        String text = Jsoup.parse(content).body().text();
        Set<String> lemmas = new HashSet<>(splitTextIntoLemmas(query));
        List<String> foundWords = getFoundWords(text, lemmas);
        String[] wordsArray = text.split(" ");
        Map<Integer, String> wordsWithPosition = getWordsWithPosition(foundWords, wordsArray);
        Map<Integer, String> wordsForSnippet = removingAdjacentPosition(wordsWithPosition);
        return concatenateLines(wordsForSnippet, wordsArray, foundWords);
    }

    private String concatenateLines(Map<Integer, String> wordsForSnippet, String[] wordsArray, List<String> words) {
        int countFoundWords = Math.min(wordsForSnippet.size(), 3);
        Map<String, Map<Integer, Set<String>>> linesWithMatchRate = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : wordsForSnippet.entrySet()) {
            String[] wordArrayLine = getWordsArrayForOneLine(entry, wordsArray, countFoundWords);
            String line = createWordsLine(wordArrayLine, words);
            Map<Integer, Set<String>> matchRate = calculateMatchRateInLine(line);
            linesWithMatchRate.put(line, matchRate);
        }
        return getBestSnippetLines(linesWithMatchRate, countFoundWords);
    }

    private String checkWordInText(String testedWord, List<String> foundWords) {
        String cleaningWord = morphologyService.cleaningText(testedWord);
        String[] words = cleaningWord.split(" ");
        for (String word : foundWords) {
            boolean isMatchWord = false;
            if (words.length > 1) {
                List<String> wordsFrom = Arrays.asList(words);
                isMatchWord = foundWords.stream().anyMatch(e1 -> wordsFrom.stream().anyMatch(e1::equals));
            }
            if (cleaningWord.toLowerCase(Locale.ROOT).equals(word) && words.length == 1 || isMatchWord) {
                return testedWord;
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
        int endPosition = findEndPositionForLine(startPosition, countFoundWords, wordsArray);
        String[] line = new String[endPosition];
        System.arraycopy(wordsArray, startPosition, line, 0, endPosition);
        return line;
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
            } else if (countFoundWords == 2 && i <= position - 14) {
                return position;
            } else if (i <= position - 8) {
                return position;
            }
        }
        return position;
    }

    private int findEndPositionForLine(int startPosition, int countFoundWords, String[] wordsArray) {
        if (countFoundWords == 1) {
            return startPosition + 32 < wordsArray.length ? 32 : wordsArray.length - startPosition;
        } else if (countFoundWords == 2) {
            return startPosition + 18 < wordsArray.length ? 18 : wordsArray.length - startPosition;
        } else {
            return startPosition + 12 < wordsArray.length ? 12 : wordsArray.length - startPosition;
        }
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

    private List<String> getFoundWords(String text, Set<String> lemmas) {
        List<String> foundWords = new ArrayList<>();
        String[] words = morphologyService.cleaningText(text).split(" ");
        for (String word : words) {
            List<String> lemmaList = morphologyService.getLemmas(word);
            if (!lemmaList.isEmpty() && lemmas.stream().anyMatch(e1 -> lemmaList.stream().anyMatch(e1::equals))) {
                foundWords.add(word);
            }
        }
        return foundWords;
    }

    private Map<Integer, Set<String>> calculateMatchRateInLine(String line) {
        Set<String> rate = new HashSet<>();
        int start = -1;
        while (start < line.length()) {
            int next = line.indexOf("<b>", start + 1);
            if (next >= 0) {
                String word = line.substring(next + 3, line.indexOf("</b>", next + 1));
                rate.addAll(splitTextIntoLemmas(word));
                start = next + 1;
            } else {
                break;
            }
        }
        return Map.of(rate.size(), rate);
    }

    private String getBestSnippetLines(Map<String, Map<Integer, Set<String>>> linesWithMatchRate, int countFoundWords) {
        Set<String> lemmasPool = new HashSet<>();
        StringBuilder stringBuilder = new StringBuilder();
        List<Integer> matchRate = linesWithMatchRate
                .values()
                .stream()
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .sorted(Comparator.reverseOrder())
                .limit(countFoundWords)
                .toList();
        if (linesWithMatchRate.size() <= 3) {
            for (Map.Entry<String, Map<Integer, Set<String>>> entry : linesWithMatchRate.entrySet()) {
                stringBuilder.append(entry.getKey()).append("...");
            }
            return stringBuilder.toString();
        }
        for (Integer rate : matchRate) {
            Iterator<Map.Entry<String, Map<Integer, Set<String>>>> iterator = linesWithMatchRate.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Map<Integer, Set<String>>> entry = iterator.next();
                Set<String> lemmasFromLine = entry.getValue().values()
                        .stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                if (Objects.equals(entry.getValue().keySet().stream().toList().get(0), rate) &&
                        !lemmasPool.containsAll(lemmasFromLine)) {
                    stringBuilder.append(entry.getKey()).append("...");
                    lemmasPool.addAll(lemmasFromLine);
                    iterator.remove();
                    break;
                }
            }
        }
        return stringBuilder.toString();
    }

    private String getSitePath(PageEntity pageEntity) {
        String path = pageEntity.getSiteEntity().getUrl();
        return path.substring(0, path.length() - 1);
    }
}
