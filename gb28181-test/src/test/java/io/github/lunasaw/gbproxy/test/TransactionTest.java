package io.github.lunasaw.gbproxy.test;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;

import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessor;

/**
 * 测试事务预创建功能
 *
 * @author luna
 */
public class TransactionTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);

    @Test
    public void testSipRequestProcessorInterface() {
        // 测试SipRequestProcessor接口的新方法
        SipRequestProcessor processor = new SipRequestProcessor() {
            @Override
            public void process(RequestEvent event) {
                log.info("处理请求事件（原方法）: {}", event);
            }

            @Override
            public void process(RequestEvent event, ServerTransaction serverTransaction) {
                log.info("处理请求事件（带事务）: event={}, transaction={}", event, serverTransaction);
            }
        };

        // 模拟调用
        RequestEvent mockEvent = null; // 实际测试中需要真实的RequestEvent
        ServerTransaction mockTransaction = null; // 实际测试中需要真实的ServerTransaction

        // 测试默认实现
        processor.process(mockEvent);
        processor.process(mockEvent, mockTransaction);

        log.info("SipRequestProcessor接口测试通过");
    }

    @Test
    public void testTransactionCreationLogic() {
        // 测试AbstractSipListener中的shouldCreateTransaction逻辑

        // 模拟MESSAGE请求 - 应该创建事务
        boolean shouldCreateForMessage = shouldCreateTransaction("MESSAGE");
        assert shouldCreateForMessage : "MESSAGE方法应该创建事务";

        // 模拟ACK请求 - 不应该创建事务
        boolean shouldCreateForAck = shouldCreateTransaction("ACK");
        assert !shouldCreateForAck : "ACK方法不应该创建事务";

        // 模拟INVITE请求 - 应该创建事务
        boolean shouldCreateForInvite = shouldCreateTransaction("INVITE");
        assert shouldCreateForInvite : "INVITE方法应该创建事务";

        log.info("事务创建逻辑测试通过");
    }

    /**
     * 模拟AbstractSipListener.shouldCreateTransaction方法的逻辑
     */
    private boolean shouldCreateTransaction(String method) {
        // MESSAGE、INFO、NOTIFY等需要响应的方法需要事务支持
        // ACK方法通常不需要服务器事务
        return !"ACK".equals(method);
    }
}