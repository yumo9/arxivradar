package com.arxivradar.service;

import com.arxivradar.domain.Paper;
import com.arxivradar.dto.MetaResponse;
import com.arxivradar.dto.PageResponse;
import com.arxivradar.dto.PaperDto;
import com.arxivradar.mapper.PaperMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class PaperService {

    private static final List<String> KNOWN_CATEGORIES =
            Arrays.asList("cs.AI", "cs.LG", "cs.CL", "cs.CV", "cs.RO", "stat.ML");

    private final PaperMapper paperMapper;

    public PaperService(PaperMapper paperMapper) {
        this.paperMapper = paperMapper;
    }

    /**
     * 分页查询。用原生列名(下划线)构造条件,避免 lambda 引用 JSON 列时的转义问题。
     */
    public PageResponse<PaperDto> listPapers(int page, int size, String category, String q, String sort) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);

        Page<Paper> pageObj = new Page<>(safePage + 1L, safeSize); // MP 分页从 1 开始
        QueryWrapper<Paper> qw = new QueryWrapper<>();

        // 分类精确匹配 (JSON 字符串 like)
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            qw.like("categories", "\"" + category + "\"");
        }

        // 关键词模糊匹配
        if (q != null && !q.isBlank()) {
            String kw = q.trim();
            qw.and(w -> w
                    .like("title", kw)
                    .or().like("authors", kw)
                    .or().like("\"abstract\"", kw));
        }

        applySort(qw, sort);

        Page<Paper> result = paperMapper.selectPage(pageObj, qw);
        List<PaperDto> content = result.getRecords().stream().map(PaperDto::from).toList();

        return new PageResponse<>(
                content,
                result.getTotal(),
                (int) result.getPages(),
                safePage,
                safeSize
        );
    }

    public PaperDto getByArxivId(String arxivId) {
        QueryWrapper<Paper> qw = new QueryWrapper<>();
        qw.eq("arxiv_id", arxivId);
        Paper paper = paperMapper.selectOne(qw);
        return paper == null ? null : PaperDto.from(paper);
    }

    public MetaResponse getMeta() {
        long count = paperMapper.selectCount(null);
        return new MetaResponse(count, null, KNOWN_CATEGORIES);
    }

    private void applySort(QueryWrapper<Paper> qw, String sort) {
        String key = sort == null ? "score" : sort.toLowerCase();
        switch (key) {
            case "date":
                qw.orderByDesc("published_date");
                break;
            case "novelty":
                // ai_score 是 JSONB, 用 postgres 的 -> 操作
                qw.orderByDesc("(ai_score->>'novelty')::int");
                break;
            case "score":
            default:
                qw.orderByDesc("(ai_score->>'overall')::int");
                break;
        }
    }
}
