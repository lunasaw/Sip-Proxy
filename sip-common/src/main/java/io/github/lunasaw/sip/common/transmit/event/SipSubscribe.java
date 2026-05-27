package io.github.lunasaw.sip.common.transmit.event;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * SIP订阅管理器，维护成功和失败两类事件回调，并定时清理过期订阅。
 */
@Slf4j
@Component
@Getter
@Setter
public class SipSubscribe {

    /** 错误事件订阅映射，key 为 Call-ID。 */
    public static final Map<String, Event> errorSubscribes = new ConcurrentHashMap<>();

    /** 成功事件订阅映射，key 为 Call-ID。 */
    public static final Map<String, Event> okSubscribes = new ConcurrentHashMap<>();

    /** 成功订阅的注册时间映射，用于过期清理。 */
    public static final Map<String, Instant> okTimeSubscribes = new ConcurrentHashMap<>();

    /** 错误订阅的注册时间映射，用于过期清理。 */
    public static final Map<String, Instant> errorTimeSubscribes = new ConcurrentHashMap<>();

    /**
     * 添加错误事件订阅。
     *
     * @param key   Call-ID
     * @param event 错误回调
     */
    public synchronized static void addErrorSubscribe(String key, Event event) {
        errorSubscribes.put(key, event);
        errorTimeSubscribes.put(key, Instant.now());
    }

    /**
     * 添加成功事件订阅。
     *
     * @param key   Call-ID
     * @param event 成功回调
     */
    public synchronized static void addOkSubscribe(String key, Event event) {
        okSubscribes.put(key, event);
        okTimeSubscribes.put(key, Instant.now());
    }

    /**
     * 获取错误事件订阅。
     *
     * @param key Call-ID
     * @return 错误回调，不存在时返回null
     */
    public static Event getErrorSubscribe(String key) {
        return errorSubscribes.get(key);
    }

    /**
     * 移除错误事件订阅。
     *
     * @param key Call-ID
     */
    public synchronized static void removeErrorSubscribe(String key) {
        if (key == null) {
            return;
        }
        errorSubscribes.remove(key);
        errorTimeSubscribes.remove(key);
    }

    /**
     * 获取成功事件订阅。
     *
     * @param key Call-ID
     * @return 成功回调，不存在时返回null
     */
    public static Event getOkSubscribe(String key) {
        return okSubscribes.get(key);
    }

    /**
     * 移除成功事件订阅。
     *
     * @param key Call-ID
     */
    public synchronized static void removeOkSubscribe(String key) {
        if (key == null) {
            return;
        }
        okSubscribes.remove(key);
        okTimeSubscribes.remove(key);
    }

    /**
     * 获取错误订阅数量。
     *
     * @return 错误订阅数量
     */
    public static int getErrorSubscribesSize() {
        return errorSubscribes.size();
    }

    /**
     * 获取成功订阅数量。
     *
     * @return 成功订阅数量
     */
    public static int getOkSubscribesSize() {
        return okSubscribes.size();
    }

    /**
     * 发布成功事件，触发对应 Call-ID 的成功回调。
     *
     * @param evt SIP响应事件
     */
    public static void publishOkEvent(ResponseEvent evt) {
        Response response = evt.getResponse();
        CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        String callId = callIdHeader.getCallId();
        Event event = okSubscribes.get(callId);
        if (event != null) {
            removeOkSubscribe(callId);
            event.response(new EventResult(evt));
        }
    }

    /**
     * 发布 ACK 事件，触发对应 Call-ID 的成功回调。
     *
     * @param evt SIP请求事件（ACK）
     */
    public static void publishAckEvent(RequestEvent evt) {
        String callId = evt.getDialog().getCallId().getCallId();
        Event event = okSubscribes.get(callId);
        if (event != null) {
            removeOkSubscribe(callId);
            event.response(new EventResult(evt));
        }
    }

    // @Scheduled(cron="*/5 * * * * ?") //每五秒执行一次
    // @Scheduled(fixedRate= 100 * 60 * 60 )
    @Scheduled(cron = "0 0/5 * * * ?") // 每5分钟执行一次
    public void execute() {
        log.info("[定时任务] 清理过期的SIP订阅信息");

        Instant instant = Instant.now().minusMillis(TimeUnit.MINUTES.toMillis(5));

        for (String key : okTimeSubscribes.keySet()) {
            if (okTimeSubscribes.get(key).isBefore(instant)) {
                okSubscribes.remove(key);
                okTimeSubscribes.remove(key);
            }
        }
        for (String key : errorTimeSubscribes.keySet()) {
            if (errorTimeSubscribes.get(key).isBefore(instant)) {
                errorSubscribes.remove(key);
                errorTimeSubscribes.remove(key);
            }
        }
        log.debug("okTimeSubscribes.size:{}", okTimeSubscribes.size());
        log.debug("okSubscribes.size:{}", okSubscribes.size());
        log.debug("errorTimeSubscribes.size:{}", errorTimeSubscribes.size());
        log.debug("errorSubscribes.size:{}", errorSubscribes.size());
    }
}
