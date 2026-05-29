package io.github.lunasaw.sip.common.utils;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * 动态定时任务
 *
 * @author lin
 */
@Component
public class DynamicTask {

    private final Logger                          logger      = LoggerFactory.getLogger(DynamicTask.class);
    private final Map<String, ScheduledFuture<?>> futureMap   = new ConcurrentHashMap<>();
    private final Map<String, Runnable>           runnableMap = new ConcurrentHashMap<>();
    private ThreadPoolTaskScheduler               threadPoolTaskScheduler;

    @PostConstruct
    public void DynamicTask() {
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(200);
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskScheduler.setAwaitTerminationSeconds(10);
        threadPoolTaskScheduler.initialize();
    }

    public void startCron(String key, Runnable task, Integer duration, TimeUnit timeUnit) {
        long convert = TimeUnit.MILLISECONDS.convert(duration, timeUnit);
        startCron(key, task, convert);
    }

    /**
     * 循环执行的任务
     *
     * @param key 任务ID
     * @param task 任务
     * @param time 间隔 毫秒
     * @return
     */
    public void startCron(String key, Runnable task, long time) {
        if (ObjectUtils.isEmpty(key)) {
            return;
        }
        ScheduledFuture<?> future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                logger.debug("任务【{}】已存在但是关闭状态！！！", key);
            } else {
                logger.debug("任务【{}】已存在且已启动！！！", key);
                return;
            }
        }
        // scheduleWithFixedDelay 必须等待上一个任务结束才开始计时period， time表示执行的间隔
        future = threadPoolTaskScheduler.scheduleAtFixedRate(task, time);
        if (future != null) {
            futureMap.put(key, future);
            runnableMap.put(key, task);
            logger.debug("任务【{}】启动成功！！！", key);
        } else {
            logger.debug("任务【{}】启动失败！！！", key);
        }
    }

    /**
     * 延时任务
     *
     * @param key 任务ID
     * @param task 任务
     * @param delay 延时 /毫秒
     * @return
     */
    public void startDelay(String key, Runnable task, int delay) {
        if (ObjectUtils.isEmpty(key)) {
            return;
        }
        stop(key);

        // 获取执行的时刻
        Instant startInstant = Instant.now().plusMillis(TimeUnit.MILLISECONDS.toMillis(delay));

        ScheduledFuture future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                logger.debug("任务【{}】已存在但是关闭状态！！！", key);
            } else {
                logger.debug("任务【{}】已存在且已启动！！！", key);
                return;
            }
        }
        // scheduleWithFixedDelay 必须等待上一个任务结束才开始计时 period
        future = threadPoolTaskScheduler.schedule(task, startInstant);
        if (future != null) {
            futureMap.put(key, future);
            runnableMap.put(key, task);
            logger.debug("任务【{}】启动成功！！！", key);
        } else {
            logger.debug("任务【{}】启动失败！！！", key);
        }
    }

    /**
     * 停止指定key的定时任务。
     *
     * @param key 任务ID
     * @return 是否成功停止
     */
    public boolean stop(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return false;
        }
        boolean result = false;
        if (!ObjectUtils.isEmpty(futureMap.get(key)) && !futureMap.get(key).isCancelled() && !futureMap.get(key).isDone()) {
            result = futureMap.get(key).cancel(false);
            futureMap.remove(key);
            runnableMap.remove(key);
        }
        return result;
    }

    /**
     * 判断指定key的任务是否存在。
     *
     * @param key 任务ID
     * @return 是否存在
     */
    public boolean contains(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return false;
        }
        return futureMap.get(key) != null;
    }

    /**
     * 获取所有任务的key集合。
     *
     * @return 任务key集合
     */
    public Set<String> getAllKeys() {
        return futureMap.keySet();
    }

    /**
     * 获取指定key的任务Runnable。
     *
     * @param key 任务ID
     * @return Runnable，不存在时返回null
     */
    public Runnable get(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return null;
        }
        return runnableMap.get(key);
    }

    /**
     * 每五分钟检查失效的任务，并移除
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void execute() {
        if (futureMap.size() > 0) {
            for (String key : futureMap.keySet()) {
                ScheduledFuture<?> future = futureMap.get(key);
                if (future.isDone() || future.isCancelled()) {
                    futureMap.remove(key);
                    runnableMap.remove(key);
                }
            }
        }
    }

    /**
     * 判断指定key的任务是否存活（未完成且未取消）。
     *
     * @param key 任务ID
     * @return 是否存活
     */
    public boolean isAlive(String key) {
        return futureMap.get(key) != null && !futureMap.get(key).isDone() && !futureMap.get(key).isCancelled();
    }
}
