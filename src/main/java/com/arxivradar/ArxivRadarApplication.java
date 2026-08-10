package com.arxivradar;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.arxivradar.mapper")
@EnableScheduling
public class ArxivRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArxivRadarApplication.class, args);
    }
}
