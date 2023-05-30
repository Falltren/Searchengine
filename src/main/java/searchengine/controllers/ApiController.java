package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.response.FailIndexing;
import searchengine.dto.response.IndexingResponse;
import searchengine.dto.response.SuccessfulIndexation;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteService siteService;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> doIndexing() {
        if (siteService.isIndexing()) {
            return ResponseEntity.badRequest().body(new FailIndexing("Индексация уже запущена"));
        }
        indexingService.startIndexing();
        return ResponseEntity.ok().body(new SuccessfulIndexation());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        if (!siteService.isIndexing()){
            return ResponseEntity.badRequest().body(new FailIndexing("Индексация не запущена"));
        }
        indexingService.stopIndexing();
        return ResponseEntity.ok().body(new SuccessfulIndexation());
    }
}
