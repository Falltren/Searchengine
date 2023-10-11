package searchengine.services;

import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LemmaService {

    List<LemmaEntity> findAllLemmaEntityBySiteEntity(SiteEntity siteEntity);

    Optional<LemmaEntity> findLemmaEntityByLemmaAndSiteEntity(String lemma, SiteEntity siteEntity);

    Map<LemmaEntity, Integer> addLemma(Map<String, Integer> lemmas, SiteEntity siteEntity);

    List<LemmaEntity> findLemmasList(SiteEntity siteEntity, Collection<String> lemmas);


}
