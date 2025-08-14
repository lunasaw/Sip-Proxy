package io.github.lunasaw.sip.common.transmit.event.request;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;

/**
 * 对SIP事件进行处理，包括request， response， timeout， ioException, transactionTerminated,dialogTerminated
 * 
 * @author luna
 */
public interface SipRequestProcessor {

    /**
     * 对SIP事件进行处理
     * 
     * @param event SIP事件
     */
    void process(RequestEvent event);

    /**
     * 对SIP事件进行处理（带预创建的服务器事务）
     *
     * @param event             SIP事件
     * @param serverTransaction 预创建的服务器事务（可为null）
     */
    default void process(RequestEvent event, ServerTransaction serverTransaction) {
        // 默认实现调用原有方法，向后兼容
        process(event);
    }

}
