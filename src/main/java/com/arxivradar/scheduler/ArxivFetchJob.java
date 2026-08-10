package com.arxivradar.scheduler;

import com.arxivradar.mapper.PaperMapper;
import com.arxivradar.service.ArxivFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * arXiv 抓取的调度器:
 * - 应用启动后如果 paper 表为空,立刻拉一次。
 * - 每天早上 6 点(应用时区,默认 Asia/Shanghai)拉一次。
 */
@Component
public class ArxivFetchJob {

    private static final Logger log = LoggerFactory.getLogger(ArxivFetchJob.class);

    private final ArxivFetchService fetchService;
    private final PaperMapper paperMapper;

    public ArxivFetchJob(ArxivFetchService fetchService, PaperMapper paperMapper) {
        this.fetchService = fetchService;
        this.paperMapper = paperMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        long count = paperMapper.selectCount(null);
        if (count > 0) {
            log.info("paper 表已有 {} 条数据,跳过启动时抓取", count);
            return;
        }
        log.info("paper 表为空,启动时拉取一次");
        safeFetch();
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Shanghai")
    public void dailyFetch() {
        log.info("定时任务触发:每日 arXiv 抓取");
        safeFetch();
    }

    private void safeFetch() {
        try {
            int n = fetchService.fetchOnce();
            log.info("抓取完成:{} 条", n);
        } catch (Exception e) {
            log.error("arXiv 抓取失败", e);
        }
    }
}
