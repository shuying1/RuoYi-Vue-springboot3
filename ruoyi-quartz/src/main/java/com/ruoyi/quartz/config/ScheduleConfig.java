//package com.ruoyi.quartz.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.quartz.SchedulerFactoryBean;
//import javax.sql.DataSource;
//import java.util.Properties;
//
///**
// * Quartz Scheduler 数据库持久化配置（已禁用）
// *
// * <p>原始用途：将 Quartz 调度状态持久化到数据库（qrtz_* 表），
// * 实现集群多节点共享调度。但 RuoYi 默认使用"内存模式"（本文件被注释），
// * 单机部署效率最高。</p>
// *
// * <p>关键配置参数说明：</p>
// * <ul>
// *   <li>{@code org.quartz.jobStore.class = LocalDataSourceJobStore} — 使用 Spring 管理的数据源；</li>
// *   <li>{@code org.quartz.jobStore.isClustered = true} — 开启集群模式；</li>
// *   <li>{@code org.quartz.scheduler.instanceId = AUTO} — 自动生成节点 ID；</li>
// *   <li>{@code org.quartz.threadPool.threadCount = 20} — 调度线程池大小；</li>
// *   <li>{@code startUpDelay = 1} — 应用启动后延迟 1 秒再启动 Scheduler，
// *       保证 DataSource 等依赖就绪。</li>
// * </ul>
// *
// * <p>启用前提：数据库中必须提前建好 qrtz_* 系列表（Quartz 官方 schema），
// * 且 RuoYi 的 sys_job 表中不能手动修改 ID 和 jobGroup 字段
// * （Service 层 init() 依赖这两个字段从 DB 重建 Scheduler 状态）。</p>
// *
// * @author ruoyi
// */
//@Configuration
//public class ScheduleConfig
//{
//    @Bean
//    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource)
//    {
//        SchedulerFactoryBean factory = new SchedulerFactoryBean();
//        factory.setDataSource(dataSource);
//
//        // quartz参数
//        Properties prop = new Properties();
//        prop.put("org.quartz.scheduler.instanceName", "RuoyiScheduler");
//        prop.put("org.quartz.scheduler.instanceId", "AUTO");
//        // 线程池配置
//        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
//        prop.put("org.quartz.threadPool.threadCount", "20");
//        prop.put("org.quartz.threadPool.threadPriority", "5");
//        // JobStore配置
//        prop.put("org.quartz.jobStore.class", "org.springframework.scheduling.quartz.LocalDataSourceJobStore");
//        // 集群配置
//        prop.put("org.quartz.jobStore.isClustered", "true");
//        prop.put("org.quartz.jobStore.clusterCheckinInterval", "15000");
//        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "10");
//        prop.put("org.quartz.jobStore.txIsolationLevelSerializable", "true");
//
//        // sqlserver 启用
//        // prop.put("org.quartz.jobStore.selectWithLockSQL", "SELECT * FROM {0}LOCKS UPDLOCK WHERE LOCK_NAME = ?");
//        prop.put("org.quartz.jobStore.misfireThreshold", "12000");
//        prop.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
//        factory.setQuartzProperties(prop);
//
//        factory.setSchedulerName("RuoyiScheduler");
//        // 延时启动
//        factory.setStartupDelay(1);
//        factory.setApplicationContextSchedulerContextKey("applicationContextKey");
//        // 可选，QuartzScheduler
//        // 启动时更新己存在的Job，这样就不用每次修改targetObject后删除qrtz_job_details表对应记录了
//        factory.setOverwriteExistingJobs(true);
//        // 设置自动启动，默认为true
//        factory.setAutoStartup(true);
//
//        return factory;
//    }
//}
