package com.asiainfo.cuc.serverdetective.config;

import com.asiainfo.cuc.serverdetective.task.WatcherTask;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfiguration {

    @Value("${email.special}")
    private boolean special;

    /**
     * 服务器监控
     * @return
     */
    @Bean
    public JobDetail watcherDetail(){
        JobDetail jobDetail = JobBuilder.newJob(WatcherTask.class).withIdentity("watcherTask").storeDurably().build();
        return jobDetail;
    }

    @Bean
    public Trigger watcherTrigger(){

        //每天6点半点执行
        return TriggerBuilder.newTrigger().forJob(watcherDetail())
                .withIdentity("watcherTask")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 30 6 * * ?"))
                .build();
    }

    /**
     * 只给指定的人频繁的发送邮件
     * @return
     */
    @Bean
    public JobDetail darkWatcherDetail(){
        JobDetail jobDetail = JobBuilder.newJob(WatcherTask.class).withIdentity("darkWatcherTask").storeDurably().build();
        jobDetail.getJobDataMap().put("special",special);
        return jobDetail;
    }

    @Bean
    public Trigger watcherDualTrigger(){
        //4个小时发送一次
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(2)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(darkWatcherDetail())
                .withIdentity("darkWatcherTask")
                .withSchedule(scheduleBuilder)
                .build();
    }

}
