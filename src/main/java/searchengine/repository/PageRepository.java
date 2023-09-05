package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    void deletePageEntityBySiteEntityAndPath(SiteEntity siteEntity, String path);

    List<PageEntity> findPageEntityBySiteEntityAndIndexesIn(SiteEntity siteEntity, List<IndexEntity>indexes);
}
