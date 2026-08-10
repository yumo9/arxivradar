package com.arxivradar;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Phase 0: 需要 docker compose up 起 Postgres + Redis 后再启用")
class ArxivRadarApplicationTests {

    @Test
    void contextLoads() {
        // Sanity check: Spring context 起得来即通过
    }
}
