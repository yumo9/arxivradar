package com.arxivradar.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * arXiv 论文实体。
 *
 * authors / categories 使用 JacksonTypeHandler 自动序列化为 JSON 字符串。
 * ai_score 直接映射到 PostgreSQL 的 jsonb 列。
 */
@TableName(value = "paper", autoResultMap = true)
public class Paper {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String arxivId;

    private String title;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> authors;

    @TableField("\"abstract\"")
    private String abstractText;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> categories;

    private LocalDate publishedDate;

    private String pdfUrl;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private AiScore aiScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getArxivId() { return arxivId; }
    public void setArxivId(String arxivId) { this.arxivId = arxivId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public LocalDate getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDate publishedDate) { this.publishedDate = publishedDate; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public AiScore getAiScore() { return aiScore; }
    public void setAiScore(AiScore aiScore) { this.aiScore = aiScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
