package searchengine.services;

import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.Map;

public interface IndexService {

    void addIndex(PageEntity pageEntity, Map<LemmaEntity, Integer> lemmasWithRank);
}
