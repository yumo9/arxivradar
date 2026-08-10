package com.arxivradar.controller;

import com.arxivradar.dto.MetaResponse;
import com.arxivradar.dto.PageResponse;
import com.arxivradar.dto.PaperDto;
import com.arxivradar.service.ArxivFetchService;
import com.arxivradar.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Papers", description = "论文数据接口")
public class PaperController {

    private final PaperService paperService;
    private final ArxivFetchService arxivFetchService;

    public PaperController(PaperService paperService, ArxivFetchService arxivFetchService) {
        this.paperService = paperService;
        this.arxivFetchService = arxivFetchService;
    }

    @Operation(summary = "分页查询论文列表")
    @GetMapping("/papers")
    public PageResponse<PaperDto> listPapers(
            @Parameter(description = "页码,从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数,最大 100") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "分类过滤:cs.AI / cs.LG / all") @RequestParam(required = false) String category,
            @Parameter(description = "关键词,匹配标题/作者/摘要") @RequestParam(required = false) String q,
            @Parameter(description = "排序:score / date / novelty") @RequestParam(defaultValue = "score") String sort
    ) {
        return paperService.listPapers(page, size, category, q, sort);
    }

    @Operation(summary = "按 arXiv ID 查询单篇论文")
    @GetMapping("/papers/{arxivId}")
    public ResponseEntity<PaperDto> getPaper(@PathVariable String arxivId) {
        PaperDto paper = paperService.getByArxivId(arxivId);
        return paper == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(paper);
    }

    @Operation(summary = "数据元信息(总数、分类列表)")
    @GetMapping("/meta")
    public MetaResponse getMeta() {
        return paperService.getMeta();
    }

    @Operation(summary = "手动触发一次 arXiv 抓取(开发用)")
    @PostMapping("/admin/refresh")
    public Map<String, Object> refresh() throws Exception {
        long t0 = System.currentTimeMillis();
        int n = arxivFetchService.fetchOnce();
        return Map.of(
                "fetched", n,
                "durationMs", System.currentTimeMillis() - t0
        );
    }
}
