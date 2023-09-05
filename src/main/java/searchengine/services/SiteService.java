package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;

    @Transactional
    public void deleteSiteByUrl(String url) {
        siteRepository.deleteByUrl(url);
    }

//    @Modifying
//    @Transactional
//    @Query(value = "DELETE FROM sites s WHERE s.url = :url", nativeQuery = true)
//    public void deleteSiteByUrl(String url) {
//    }

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

    public List<SiteEntity> getIndexedSites(){
        return siteRepository.findByStatus(StatusType.INDEXING);
    }

}
