package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    List<LemmaEntity> findAllLemmaEntityBySiteEntity(SiteEntity siteEntity);

    Optional<LemmaEntity> findLemmaEntityByLemmaAndSiteEntity(String lemma, SiteEntity siteEntity);

    List<LemmaEntity> findBySiteEntityAndLemmaIn(SiteEntity siteEntity, Collection<String>lemmas);

}
