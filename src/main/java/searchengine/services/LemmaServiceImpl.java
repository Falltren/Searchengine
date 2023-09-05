package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;

//    private final MorphologyService morphologyService;

    @Override
    public List<LemmaEntity> findAllLemmaEntityBySiteEntity(SiteEntity siteEntity) {
        return lemmaRepository.findAllLemmaEntityBySiteEntity(siteEntity);
    }

    public synchronized Map<LemmaEntity, Integer> addLemma(Map<String, Integer> lemmas, SiteEntity siteEntity) {
        LemmaEntity lemmaEntity;
        Map<LemmaEntity, Integer> map = new HashMap<>();
        for (String word : lemmas.keySet()) {
            Optional<LemmaEntity> optionalLemma = findLemmaEntityByLemmaAndSiteEntity(word, siteEntity);
            if (optionalLemma.isPresent()) {
                lemmaEntity = optionalLemma.get();
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            } else {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSiteEntity(siteEntity);
                lemmaEntity.setFrequency(1);
                lemmaEntity.setLemma(word);
            }
            lemmaRepository.save(lemmaEntity);
            map.put(lemmaEntity, lemmas.get(word));
        }
        return map;
    }

    public Optional<LemmaEntity> findLemmaEntityByLemmaAndSiteEntity(String lemma, SiteEntity siteEntity) {
        return lemmaRepository.findLemmaEntityByLemmaAndSiteEntity(lemma, siteEntity);
    }

    @Override
    public List<LemmaEntity> findLemmasList(SiteEntity siteEntity, Collection<String> lemmas) {
        return lemmaRepository.findLemmaEntityBySiteEntityAndLemmaIn(siteEntity, lemmas);
    }
}
