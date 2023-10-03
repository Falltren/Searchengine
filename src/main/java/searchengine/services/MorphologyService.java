package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MorphologyService {
    private final LuceneMorphology luceneMorphology;

    private boolean isAuxiliaryPartOfSpeech(String word) {
        return word.contains("|l ПРЕДЛ") ||
                word.contains("|o МЕЖД") ||
                word.contains("|n СОЮЗ") ||
                word.contains("|p ЧАСТ");
    }

    private String getWordFromMorphInfo(String word) {
        return word.substring(0, word.indexOf("|"));
    }

    public String cleaningText(String text) {
        return text.toLowerCase()
                .replaceAll("[^а-яё]+", " ")
                .replaceAll("ё", "е")
                .replaceAll("\\s+", " ")
                .strip();
    }

    public Map<String, Integer> collectLemmas(String text) {
        if (text.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Integer> lemmas = new HashMap<>();
        String[] words = getWordsArray(text);
        for (String word : words) {
            List<String> lemmaList = luceneMorphology.getMorphInfo(word)
                    .stream()
                    .filter(l -> !isAuxiliaryPartOfSpeech(l) && l.length() > 1)
                    .map(this::getWordFromMorphInfo)
                    .toList();
            calculateLemmaCount(lemmas, lemmaList);
        }
        return lemmas;
    }

    public Map<String, Set<String>> getWordsWithLemmas(String text) {
        Map<String, Set<String>> result = new HashMap<>();
        String[] words = getWordsArray(text);
        for (String word : words) {
            Set<String> lemmas = luceneMorphology.getMorphInfo(word)
                    .stream()
                    .filter(l -> !isAuxiliaryPartOfSpeech(l) && l.length() > 1)
                    .map(this::getWordFromMorphInfo)
                    .collect(Collectors.toSet());
            result.put(word, lemmas);
        }
        return result;
    }

    private String[] getWordsArray(String text) {
        return cleaningText(text).split(" ");
    }

    private void calculateLemmaCount(Map<String, Integer> lemmaStorage, List<String> lemmas) {
        for (String lemma : lemmas) {
            if (lemmaStorage.containsKey(lemma)) {
                int value = lemmaStorage.get(lemma);
                lemmaStorage.put(lemma, value + 1);
            } else {
                lemmaStorage.put(lemma, 1);
            }
        }
    }
}
