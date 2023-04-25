package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;

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


}
