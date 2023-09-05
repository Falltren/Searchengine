package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;

    public void saveNewPage(PageEntity pageEntity, SiteEntity siteEntity, String path, int code, String content) {
        pageEntity.setSiteEntity(siteEntity);
        pageEntity.setPath(path);
        pageEntity.setCode(code);
        pageEntity.setContent(content);
        pageRepository.save(pageEntity);
    }

    @Transactional
    public void deletePageEntityBySiteEntityAndPath(SiteEntity siteEntity, String path) {
        pageRepository.deletePageEntityBySiteEntityAndPath(siteEntity, path);
    }

    public List<PageEntity> findListPagesByIndexes(SiteEntity siteEntity, List<IndexEntity> indexes){
        return pageRepository.findPageEntityBySiteEntityAndIndexesIn(siteEntity, indexes);
    }
}
