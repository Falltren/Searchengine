package searchengine.services;

import java.util.List;
import java.util.Map;

public interface MorphologyService {
    Map<String, Integer> collectLemmas(String text);

    String cleaningText(String text);

    List<String> getLemmas(String word);
}
