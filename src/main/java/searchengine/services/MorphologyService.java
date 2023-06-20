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

    public String deletePunctuationMark(String text) {
        return text.toLowerCase().replaceAll("\\p{Punct}", "");
    }

    private String getWordFromMorphInfo(String word) {
        return word.substring(0, word.indexOf("|"));
    }

    public String getContentWithoutHtmlTags(String content) {
        return Jsoup.clean(content, Safelist.none());
    }

    public String cleaningTextFromDigital(String text) {
        return text.replaceAll("\\d+", "");
    }

    public Map<String, Integer> getLemmas(String text) {
        Map<String, Integer> lemmas = new HashMap<>();
        for (String word : cleaningTextFromDigital(deletePunctuationMark(text)).split("\\s+")) {
            for (String w : luceneMorphology.getMorphInfo(word)) {
                if (!isAuxiliaryPartOfSpeech(w)) {
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
