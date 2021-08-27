package com.huobi;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/27/21 10:50 PM
 */
public class JobManagement {
    Logger logger = LoggerFactory.getLogger(JobManagement.class);
    private static Scheduler scheduler;
    private final List<JobDetail> jobDetailList = new ArrayList<>();
    private final List<Trigger> triggerList = new ArrayList<>();


    public JobManagement() {
        try {
            // 获取到一个StdScheduler, StdScheduler其实是QuartzScheduler的一个代理
            scheduler = StdSchedulerFactory.getDefaultScheduler();
        } catch (Exception e) {
            logger.error("====== JobManagement-JobManagement: {} ======", e.getMessage());
        }

    }

    void addJob(String time, Class<? extends Job> jobClass, String jobDetailsKey) {
        // 新建一个Job, 指定执行类是QuartzTest(需实现Job), 指定一个K/V类型的数据, 指定job的name和group
        JobDetail job = newJob(jobClass)
                .usingJobData(jobDetailsKey, jobDetailsKey + "_quant")
                .withIdentity(jobDetailsKey + "_group", jobDetailsKey + "_timer")
                .build();
        // 新建一个Trigger, 表示JobDetail的调度计划, 这里的cron表达式是 每10秒执行一次
        CronTrigger trigger = newTrigger()
                .withIdentity(jobDetailsKey + "_trigger", jobDetailsKey + "_timer")
                .startNow()
                .withSchedule(cronSchedule(time))
                .build();
        jobDetailList.add(job);
        triggerList.add(trigger);
    }

    void startJob() {
        try {
            scheduler.start();
            for (int i = 0; i < jobDetailList.size(); i++) {
                scheduler.scheduleJob(jobDetailList.get(i), triggerList.get(i));
            }
            // 保持进程不被销毁
            Thread.sleep(10000000);
        } catch (Exception e) {
            logger.error("====== JobManagement-startJob: {} ======", e.getMessage());

        }

    }
}
