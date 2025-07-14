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
 * @author lin
 */
@Slf4j
@Component
@Getter
@Setter
public class SipSubscribe {

    public static final Map<String, Event> errorSubscribes = new ConcurrentHashMap<>();

    public static final Map<String, Event> okSubscribes = new ConcurrentHashMap<>();

    public static final Map<String, Instant> okTimeSubscribes = new ConcurrentHashMap<>();

    public static final Map<String, Instant> errorTimeSubscribes = new ConcurrentHashMap<>();

    public synchronized static void addErrorSubscribe(String key, Event event) {
        errorSubscribes.put(key, event);
        errorTimeSubscribes.put(key, Instant.now());
    }

    public synchronized static void addOkSubscribe(String key, Event event) {
        okSubscribes.put(key, event);
        okTimeSubscribes.put(key, Instant.now());
    }

    public static Event getErrorSubscribe(String key) {
        return errorSubscribes.get(key);
    }

    public synchronized static void removeErrorSubscribe(String key) {
        if (key == null) {
            return;
        }
        errorSubscribes.remove(key);
        errorTimeSubscribes.remove(key);
    }

    public static Event getOkSubscribe(String key) {
        return okSubscribes.get(key);
    }

    public synchronized static void removeOkSubscribe(String key) {
        if (key == null) {
            return;
        }
        okSubscribes.remove(key);
        okTimeSubscribes.remove(key);
    }

    public static int getErrorSubscribesSize() {
        return errorSubscribes.size();
    }

    public static int getOkSubscribesSize() {
        return okSubscribes.size();
    }

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
