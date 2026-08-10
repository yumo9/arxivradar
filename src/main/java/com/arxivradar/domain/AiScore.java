package com.arxivradar.domain;

import java.io.Serializable;

/**
 * arXiv 论文 AI 评分。
 */
public class AiScore implements Serializable {

    private int overall;
    private int novelty;
    private int impact;
    private int readability;
    private int methodology;

    public AiScore() {
    }

    public AiScore(int overall, int novelty, int impact, int readability, int methodology) {
        this.overall = overall;
        this.novelty = novelty;
        this.impact = impact;
        this.readability = readability;
        this.methodology = methodology;
    }

    public int getOverall() { return overall; }
    public void setOverall(int overall) { this.overall = overall; }

    public int getNovelty() { return novelty; }
    public void setNovelty(int novelty) { this.novelty = novelty; }

    public int getImpact() { return impact; }
    public void setImpact(int impact) { this.impact = impact; }

    public int getReadability() { return readability; }
    public void setReadability(int readability) { this.readability = readability; }

    public int getMethodology() { return methodology; }
    public void setMethodology(int methodology) { this.methodology = methodology; }
}
