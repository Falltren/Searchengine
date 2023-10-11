package searchengine.services;

import java.util.Map;
import java.util.Set;

public interface MorphologyService {
    Map<String, Integer> collectLemmas(String text);

    Map<String, Set<String>> getWordsWithLemmas(String text);

    String cleaningText(String text);
}
