package com.arxivradar.dto;

import com.arxivradar.domain.AiScore;
import com.arxivradar.domain.Paper;

import java.time.LocalDate;
import java.util.List;

/**
 * 返回给前端的 Paper 视图。字段名保持与前端 types/paper.ts 一致。
 */
public class PaperDto {

    private String id;
    private String arxivId;
    private String title;
    private List<String> authors;
    private String abstract_;
    private List<String> categories;
    private LocalDate publishedDate;
    private String pdfUrl;
    private AiScore aiScore;
    private List<String> relatedPaperIds;

    public static PaperDto from(Paper p) {
        PaperDto dto = new PaperDto();
        dto.id = p.getArxivId();
        dto.arxivId = p.getArxivId();
        dto.title = p.getTitle();
        dto.authors = p.getAuthors();
        dto.abstract_ = p.getAbstractText();
        dto.categories = p.getCategories();
        dto.publishedDate = p.getPublishedDate();
        dto.pdfUrl = p.getPdfUrl();
        dto.aiScore = p.getAiScore();
        dto.relatedPaperIds = List.of();
        return dto;
    }

    public String getId() { return id; }
    public String getArxivId() { return arxivId; }
    public String getTitle() { return title; }
    public List<String> getAuthors() { return authors; }

    // 前端字段名叫 abstract,但 abstract 是 Java 关键字。用 getter 名映射到 JSON 名。
    @com.fasterxml.jackson.annotation.JsonProperty("abstract")
    public String getAbstract() { return abstract_; }

    public List<String> getCategories() { return categories; }
    public LocalDate getPublishedDate() { return publishedDate; }
    public String getPdfUrl() { return pdfUrl; }
    public AiScore getAiScore() { return aiScore; }
    public List<String> getRelatedPaperIds() { return relatedPaperIds; }
}
