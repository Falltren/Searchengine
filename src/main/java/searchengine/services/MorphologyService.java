package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MorphologyService {
    private final LuceneMorphology luceneMorphology;

    private static boolean isAuxiliaryPartOfSpeech(String word) {
        return word.contains("|l ПРЕДЛ") ||
                word.contains("|o МЕЖД") ||
                word.contains("|n СОЮЗ") ||
                word.contains("|p ЧАСТ");
    }


    private String getWordFromMorphInfo(String word) {
        return word.substring(0, word.indexOf("|"));
    }

    public String getContentWithoutHtmlTags(String content) {
        return Jsoup.clean(content, Safelist.none());
    }

    public String cleaningText(String text) {
        return text.toLowerCase()
                .replaceAll("[^а-яё]+", " ")
                .replaceAll("ё", "е")
                .replaceAll("\\p{Punct}", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }

    public Map<String, Integer> getLemmas(String text) {
        Map<String, Integer> lemmas = new HashMap<>();
        for (String word : cleaningText(text).split("\\s+")) {
            for (String w : luceneMorphology.getMorphInfo(word)) {
                if (!isAuxiliaryPartOfSpeech(w) && word.length() > 1) {
                    String key = getWordFromMorphInfo(w);
                    if (lemmas.containsKey(key)) {
                        lemmas.put(key, lemmas.get(key) + 1);
                    } else {
                        lemmas.put(key, 1);
                    }
                }
            }
        }
        return lemmas;
    }
}
