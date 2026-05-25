package io.github.lunasaw.sip.common.transmit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 1.7.0 新增：定期清理 {@link DialogRegistry} 中已过期的 SUBSCRIBE entry。
 *
 * <p>JAIN-SIP 在 SUBSCRIBE 订阅 expires 自然超时（无续订）时不会触发 DialogTerminatedEvent
 * （RFC 6665 §4.4.1 case 3：subscription 静默终结，dialog 状态机不变）。
 * 必须依赖本任务定时清理，否则 DialogRegistry 会持续增长。
 *
 * <p>INVITE 类型的 entry 用 {@link DialogRegistry#NO_EXPIRY} 标记，本任务不会动它们 ——
 * INVITE 由 {@code AbstractSipListener.processDialogTerminated} 主路径清理。
 *
 * @author luna
 */
@Slf4j
@Component
public class DialogRegistryCleaner {

    /**
     * 每 60s 跑一次（initialDelay 60s 避免应用启动期空跑）。
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void cleanup() {
        int cleaned = DialogRegistry.cleanupExpired();
        if (cleaned > 0) {
            log.info("DialogRegistryCleaner: cleaned {} expired entries, current size={}, subscribeSize={}",
                    cleaned, DialogRegistry.size(),
                    DialogRegistry.sizeByKind(DialogRegistry.KIND_SUBSCRIBE));
        }
    }
}
