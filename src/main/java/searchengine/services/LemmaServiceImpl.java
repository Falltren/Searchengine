package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;

    private final MorphologyService morphologyService;

    @Override
    public List<LemmaEntity> findAllLemmaEntityBySiteEntity(SiteEntity siteEntity) {
        return lemmaRepository.findAllLemmaEntityBySiteEntity(siteEntity);
    }

    public void addLemma(String lemma, SiteEntity siteEntity) {
        Map<String, Integer> map = morphologyService.getLemmas(lemma);
        for (String word : map.keySet()) {
            System.out.println("ok");

        }
    }

    public Optional<LemmaEntity> findLemmaEntityByLemmaAndSiteEntity(String lemma, SiteEntity siteEntity) {
        return lemmaRepository.findLemmaEntityByLemmaAndSiteEntity(lemma, siteEntity);
    }


}
