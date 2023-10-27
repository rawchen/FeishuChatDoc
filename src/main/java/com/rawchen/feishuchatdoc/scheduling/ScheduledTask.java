package com.rawchen.feishuchatdoc.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduledTask {

    /**
     * 每周一
     */
    @Scheduled(cron = "0 0 0 ? * MON")
    public void taskOne() {
        try {

        } catch (Exception e) {
            log.error("xx操作失败", e);
        }
    }

    /**
     * 每隔两个小时执行一次
     */
    @Scheduled(initialDelay = 1000 * 60, fixedRate = 1000 * 60 * 60 * 2)
    public void taskTwo() {
        try {

        } catch (Exception e) {
            log.error("xx操作失败", e);
        }
    }
}
