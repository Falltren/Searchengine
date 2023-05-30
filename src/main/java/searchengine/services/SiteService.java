package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.SiteRepository;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;

    @Transactional
    public void deleteSiteByUrl(String url) {
        siteRepository.deleteByUrl(url);
    }

    public Optional<SiteEntity> findSiteByUrl(String url) {
        return siteRepository.findByUrl(url);
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
}
