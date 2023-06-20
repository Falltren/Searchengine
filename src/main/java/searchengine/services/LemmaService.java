package searchengine.services;

import searchengine.config.Site;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface LemmaService {

    List<LemmaEntity>findAllLemmaEntityBySiteEntity(SiteEntity siteEntity);

    Optional<LemmaEntity> findLemmaEntityByLemmaAndSiteEntity(String lemma, SiteEntity siteEntity);

    void addLemma(String lemma, SiteEntity siteEntity);

}
