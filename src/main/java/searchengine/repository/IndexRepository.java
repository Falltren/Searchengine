package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    List<IndexEntity> findListIndexesByLemmaEntityIn(List<LemmaEntity> lemmaEntities);

    List<IndexEntity> findByPageEntityIn(List<PageEntity> pageEntities);

    List<IndexEntity> findByLemmaEntity(LemmaEntity lemmaEntity);
}
