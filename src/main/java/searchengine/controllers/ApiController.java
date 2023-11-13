package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexation.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> doIndexing() {
        IndexingResponse indexingResponse = indexingService.startIndexing();
        return ResponseEntity.ok().body(indexingResponse);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse indexingResponse = indexingService.stopIndexing();
        return ResponseEntity.ok().body(indexingResponse);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexingOnePage(@RequestParam String url) {
        IndexingResponse indexingResponse = indexingService.indexingOnePage(url);
        return ResponseEntity.ok(indexingResponse);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam(required = false) String site,
                                                 @RequestParam(required = false, defaultValue = "0") Integer offset,
                                                 @RequestParam(required = false, defaultValue = "20") Integer limit) {
        SearchResponse searchResponse = searchService.searching(query, site, offset, limit);
        return ResponseEntity.ok(searchResponse);
    }
}
