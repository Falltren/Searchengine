package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;

@Service
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;

    public void saveNewPage(SiteEntity siteEntity, String path, int code, String content) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setSiteEntity(siteEntity);
        pageEntity.setPath(path);
        pageEntity.setCode(code);
        pageEntity.setContent(content);
        pageRepository.save(pageEntity);
    }
}
