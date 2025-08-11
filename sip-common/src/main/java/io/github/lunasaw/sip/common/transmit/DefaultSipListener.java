package io.github.lunasaw.sip.common.transmit;

import io.github.lunasaw.sip.common.metrics.SipMetrics;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 默认SIP监听器
 * 继承AsyncSipListener，使用Spring传入的线程池
 * 作为Spring管理的Bean，支持依赖注入和配置管理
 *
 * @author luna
 */
@Slf4j
@Component
@NoArgsConstructor
public class DefaultSipListener extends AsyncSipListener {

    /**
     * 构造函数
     * 使用Spring传入的线程池和SIP指标收集器
     *
     * @param messageExecutor Spring管理的线程池执行器
     * @param sipMetrics      SIP指标收集器
     */
    public DefaultSipListener(@Qualifier("sipMessageProcessor") ThreadPoolTaskExecutor messageExecutor, SipMetrics sipMetrics) {
        if (messageExecutor != null) {
            setMessageExecutor(messageExecutor);
        }
        if (sipMetrics != null) {
            setSipMetrics(sipMetrics);
        }
    }

    /**
     * 设置消息执行器
     * 覆盖父类的本地线程池，使用Spring管理的线程池
     *
     * @param messageExecutor Spring管理的线程池执行器
     */
    @Override
    public void setMessageExecutor(ThreadPoolTaskExecutor messageExecutor) {
        try {
            log.info("成功设置Spring线程池: coreSize={}, maxSize={}, queueCapacity={}",
                    messageExecutor.getCorePoolSize(),
                    messageExecutor.getMaxPoolSize(),
                    messageExecutor.getQueueCapacity());
            super.setMessageExecutor(messageExecutor);
        } catch (Exception e) {
            log.error("设置Spring线程池失败", e);
            throw new RuntimeException("设置Spring线程池失败", e);
        }
    }

    /**
     * 获取默认监听器统计信息
     *
     * @return 统计信息
     */
    @Override
    public String getProcessorStats() {
        return String.format("DefaultSipListener[%s]", super.getProcessorStats());
    }
}