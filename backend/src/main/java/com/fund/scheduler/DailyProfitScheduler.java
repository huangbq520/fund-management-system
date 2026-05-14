package com.fund.scheduler;

import com.fund.service.DailyProfitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DailyProfitScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DailyProfitScheduler.class);

    @Resource
    private DailyProfitService dailyProfitService;

    @Scheduled(cron = "0 0 22 * * MON-FRI")
    public void syncDailyProfit() {
        logger.info("=== 每日收益统计任务启动（22:00 盘后） ===");
        long startTime = System.currentTimeMillis();

        try {
            dailyProfitService.calculateDailyProfit();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("=== 每日收益统计任务完成 === 耗时: {}ms", duration);
        } catch (Exception e) {
            logger.error("每日收益统计任务异常: {}", e.getMessage(), e);
        }
    }
}
