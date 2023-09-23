package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repository.IndexRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final IndexRepository indexRepository;

    public void addIndex(PageEntity pageEntity, Map<LemmaEntity, Integer> lemmasWithRank) {
        for (Map.Entry<LemmaEntity, Integer> entry : lemmasWithRank.entrySet()){
            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setPageEntity(pageEntity);
            indexEntity.setLemmaEntity(entry.getKey());
            indexEntity.setRank(Float.valueOf(entry.getValue()));
            indexRepository.save(indexEntity);
        }
    }

    @Override
    public List<IndexEntity> getIndexList(List<LemmaEntity> lemmas) {
        return indexRepository.findListIndexesByLemmaEntityIn(lemmas);
    }

    @Override
    public List<IndexEntity> getIndexesByLemma(LemmaEntity lemmaEntity) {
        return indexRepository.findByLemmaEntity(lemmaEntity);
    }


}
