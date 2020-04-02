package cn.marwin;

import cn.marwin.classifier.MyClassiyier;
import cn.marwin.crawler.CrawlTask;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.*;

@SpringBootApplication
public class Application {

    public static void main(String[] args) throws IOException {
        // 运行Web服务
        SpringApplication.run(Application.class, args);

        // 初始化分类器
        MyClassiyier.init();

        // 设置定时爬虫任务
        Timer timer = new Timer();
        long delay = 0; // 延迟启动时间
        long period = 1000 * 60 * 30; // 运行周期30m
        timer.scheduleAtFixedRate(new CrawlTask(), delay, period);
    }
    
}

