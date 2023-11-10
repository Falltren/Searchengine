package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.services.PageService;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;

    public void saveNewPage(PageEntity pageEntity, SiteEntity siteEntity, String path, int code, String content) {
        pageEntity.setSiteEntity(siteEntity);
        pageEntity.setPath(path);
        pageEntity.setCode(code);
        pageEntity.setContent(content);
        pageRepository.save(pageEntity);
    }

    @Transactional
    public void deletePageEntity(SiteEntity siteEntity, String path) {
        pageRepository.deleteBySiteEntityAndPath(siteEntity, path);
    }

}
