package com.tyty.tfs.crawler;

import com.tyty.tfs.dao.DaoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

@Import(DaoConfiguration.class)
@SpringBootApplication
public class Main {

    public static void main(String[] args) throws Exception {

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "64");

//        Crawler crawler = context.getBean(LianJiaLouPanCrawler.class);
//        Crawler crawler = context.getBean(EastMoneyStockCrawler.class);
        Crawler crawler = context.getBean(LianJiaEsfCrawler.class);
        crawler.execute();
    }
}
