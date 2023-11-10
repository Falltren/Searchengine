package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.SiteRepository;
import searchengine.services.SiteService;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;

    @Transactional
    public void deleteSiteByUrl(String url) {
        siteRepository.deleteByUrl(url);
    }

    public Optional<SiteEntity> findSiteByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    public int findPagesCountByUrl(String url) {
        Optional<SiteEntity> optionalSiteEntity = findSiteByUrl(url);
        return optionalSiteEntity.map(siteEntity -> siteEntity.getPages().size()).orElse(0);
    }

    public int findLemmasCountByUrl(String url) {
        Optional<SiteEntity> optionalSiteEntity = findSiteByUrl(url);
        return optionalSiteEntity.map(siteEntity -> siteEntity.getLemmas().size()).orElse(0);
    }

    public void saveNewSite(String url, String name) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setUrl(url);
        siteEntity.setName(name);
        siteEntity.setStatusTime(new Date());
        siteEntity.setStatus(StatusType.INDEXING);
        siteRepository.save(siteEntity);
    }

    public void save(SiteEntity siteEntity) {
        siteRepository.save(siteEntity);
    }

    public boolean isIndexing() {
        return siteRepository.findByStatus(StatusType.INDEXING).size() > 0;
    }

    public List<SiteEntity> getIndexedSites() {
        return siteRepository.findByStatus(StatusType.INDEXED);
    }

    public void stopIndexing(SiteEntity siteEntity) {
        siteEntity.setStatus(StatusType.FAILED);
        siteEntity.setStatusTime(new Date());
        siteEntity.setLastError("Индексация остановлена пользователем.");
        save(siteEntity);
    }

    public StatusType getStatus(Site site) {
        return findSiteByUrl(site.getUrl()).orElseThrow().getStatus();
    }

}
