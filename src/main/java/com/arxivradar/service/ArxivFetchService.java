package com.arxivradar.service;

import com.arxivradar.domain.AiScore;
import com.arxivradar.domain.Paper;
import com.arxivradar.mapper.PaperMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 arXiv Atom API 拉取最新 CS/ML 论文,写入 DB。
 */
@Service
public class ArxivFetchService {

    private static final Logger log = LoggerFactory.getLogger(ArxivFetchService.class);

    private static final String ARXIV_API = "https://export.arxiv.org/api/query";
    private static final List<String> CS_CATEGORIES =
            Arrays.asList("cs.AI", "cs.LG", "cs.CL", "cs.CV", "cs.RO", "stat.ML");
    private static final Set<String> KNOWN_CATEGORIES = new LinkedHashSet<>(CS_CATEGORIES);

    private static final Pattern ENTRY_PATTERN =
            Pattern.compile("<entry>([\\s\\S]*?)</entry>");
    private static final Pattern ID_PATTERN =
            Pattern.compile("<id>([\\s\\S]*?)</id>");
    private static final Pattern TITLE_PATTERN =
            Pattern.compile("<title>([\\s\\S]*?)</title>");
    private static final Pattern SUMMARY_PATTERN =
            Pattern.compile("<summary>([\\s\\S]*?)</summary>");
    private static final Pattern PUBLISHED_PATTERN =
            Pattern.compile("<published>([\\s\\S]*?)</published>");
    private static final Pattern AUTHOR_NAME_PATTERN =
            Pattern.compile("<author>\\s*<name>([\\s\\S]*?)</name>");
    private static final Pattern CATEGORY_PATTERN =
            Pattern.compile("<category[^>]*term=\"([^\"]+)\"");
    private static final Pattern PDF_LINK_PATTERN =
            Pattern.compile("<link[^>]*title=\"pdf\"[^>]*href=\"([^\"]+)\"");
    private static final Pattern ABS_ID_PATTERN =
            Pattern.compile("abs/([^v]+?)(?:v\\d+)?$");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final OkHttpClient httpClient;
    private final PaperMapper paperMapper;
    private final int maxResults;

    public ArxivFetchService(PaperMapper paperMapper,
                             @Value("${arxiv.max-results:50}") int maxResults) {
        this.paperMapper = paperMapper;
        this.maxResults = maxResults;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 执行一次抓取,返回本次 upsert 的条数。
     */
    public int fetchOnce() throws IOException {
        String url = buildUrl();
        log.info("Fetching arXiv: {}", url);

        Request request = new Request.Builder().url(url).build();
        String xml;
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("arXiv API returned " + response.code());
            }
            ResponseBody body = response.body();
            xml = body != null ? body.string() : "";
        }

        List<Paper> papers = parseAtom(xml);
        log.info("Parsed {} papers from arXiv", papers.size());

        int upserted = 0;
        for (Paper p : papers) {
            upserted += upsert(p);
        }
        log.info("Upserted {} papers to DB", upserted);
        return upserted;
    }

    private String buildUrl() {
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < CS_CATEGORIES.size(); i++) {
            if (i > 0) query.append("+OR+");
            query.append("cat:").append(CS_CATEGORIES.get(i));
        }
        return ARXIV_API
                + "?search_query=" + query
                + "&start=0"
                + "&max_results=" + maxResults
                + "&sortBy=submittedDate"
                + "&sortOrder=descending";
    }

    private List<Paper> parseAtom(String xml) {
        List<Paper> papers = new ArrayList<>();
        Matcher entryMatcher = ENTRY_PATTERN.matcher(xml);
        while (entryMatcher.find()) {
            Paper p = parseEntry(entryMatcher.group(1));
            if (p != null && p.getArxivId() != null && p.getTitle() != null) {
                papers.add(p);
            }
        }
        return papers;
    }

    private Paper parseEntry(String entry) {
        String idUrl = normalize(firstMatch(entry, ID_PATTERN));
        if (idUrl.isEmpty()) return null;
        Matcher idMatcher = ABS_ID_PATTERN.matcher(idUrl);
        String arxivId = idMatcher.find() ? idMatcher.group(1) : idUrl;

        String title = normalize(firstMatch(entry, TITLE_PATTERN));
        String abs = normalize(firstMatch(entry, SUMMARY_PATTERN));
        String published = normalize(firstMatch(entry, PUBLISHED_PATTERN));
        LocalDate publishedDate = published.length() >= 10
                ? LocalDate.parse(published.substring(0, 10))
                : LocalDate.now();

        List<String> authors = allMatches(entry, AUTHOR_NAME_PATTERN);
        authors.replaceAll(this::normalize);
        authors.removeIf(String::isEmpty);

        List<String> rawCategories = allMatches(entry, CATEGORY_PATTERN);
        LinkedHashSet<String> filteredCategories = new LinkedHashSet<>();
        for (String c : rawCategories) {
            if (KNOWN_CATEGORIES.contains(c)) filteredCategories.add(c);
        }
        List<String> categories = filteredCategories.isEmpty()
                ? new ArrayList<>(List.of("cs.AI"))
                : new ArrayList<>(filteredCategories);

        String pdfUrl = firstMatch(entry, PDF_LINK_PATTERN);
        if (pdfUrl.isEmpty()) {
            pdfUrl = "https://arxiv.org/pdf/" + arxivId;
        }

        Paper paper = new Paper();
        paper.setArxivId(arxivId);
        paper.setTitle(title);
        paper.setAbstractText(abs);
        paper.setAuthors(authors);
        paper.setCategories(categories);
        paper.setPublishedDate(publishedDate);
        paper.setPdfUrl(pdfUrl);
        paper.setAiScore(pseudoScore(arxivId));
        return paper;
    }

    private int upsert(Paper paper) {
        LambdaQueryWrapper<Paper> qw = new LambdaQueryWrapper<>();
        qw.eq(Paper::getArxivId, paper.getArxivId());
        Paper existing = paperMapper.selectOne(qw);
        if (existing == null) {
            return paperMapper.insert(paper);
        }
        paper.setId(existing.getId());
        paper.setCreatedAt(existing.getCreatedAt());
        return paperMapper.updateById(paper);
    }

    // ---------- utilities ----------

    private String firstMatch(String text, Pattern p) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private List<String> allMatches(String text, Pattern p) {
        List<String> out = new ArrayList<>();
        Matcher m = p.matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return decodeEntities(WHITESPACE_PATTERN.matcher(s.trim()).replaceAll(" "));
    }

    private String decodeEntities(String s) {
        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
    }

    // 与前端脚本相同的伪评分算法(FNV-1a hash),保证同一 arxivId 分数稳定。
    private AiScore pseudoScore(String arxivId) {
        long h = 2166136261L;
        for (int i = 0; i < arxivId.length(); i++) {
            h ^= arxivId.charAt(i);
            h = (h * 16777619L) & 0xFFFFFFFFL;
        }
        return new AiScore(
                pick(h, 0, 72, 98),
                pick(h, 4, 70, 98),
                pick(h, 8, 70, 98),
                pick(h, 12, 70, 95),
                pick(h, 16, 72, 96)
        );
    }

    private int pick(long h, int offset, int min, int max) {
        long seg = (h >> offset) & 0xffL;
        return min + (int) ((seg / 255.0) * (max - min));
    }
}
