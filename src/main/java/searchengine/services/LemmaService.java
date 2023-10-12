package searchengine.services;

import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Map;
import java.util.Optional;

public interface LemmaService {

    Optional<LemmaEntity> findLemmaEntityByLemmaAndSiteEntity(String lemma, SiteEntity siteEntity);

    Map<LemmaEntity, Integer> addLemma(Map<String, Integer> lemmas, SiteEntity siteEntity);

}
