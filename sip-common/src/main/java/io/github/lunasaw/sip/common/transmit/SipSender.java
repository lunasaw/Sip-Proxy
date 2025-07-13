package io.github.lunasaw.sip.common.transmit;

import javax.sip.ServerTransaction;
import javax.sip.address.SipURI;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.transmit.request.SipRequestBuilderFactory;
import io.github.lunasaw.sip.common.transmit.strategy.SipRequestStrategy;
import io.github.lunasaw.sip.common.transmit.strategy.SipRequestStrategyFactory;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * SIP消息发送器（重构版）
 * 使用策略模式和建造者模式，提供简洁的API接口
 *
 * @author lin
 */
@Slf4j
public class SipSender {

    /**
     * SIP请求建造者
     * 提供流式API来构建和发送SIP请求
     */
    public static class SipRequestBuilder {
        private final FromDevice fromDevice;
        private final ToDevice   toDevice;
        private final String     method;
        private String           content;
        private String           subject;
        private Integer          expires;
        private Event            errorEvent;
        private Event            okEvent;
        private String           callId;

        public SipRequestBuilder(FromDevice fromDevice, ToDevice toDevice, String method) {
            this.fromDevice = fromDevice;
            this.toDevice = toDevice;
            this.method = method;
            if (StringUtils.isBlank(toDevice.getCallId())) {
                this.callId = SipRequestUtils.getNewCallId();
            } else {
                this.callId = toDevice.getCallId();
            }
        }

        public SipRequestBuilder content(String content) {
            this.content = content;
            return this;
        }

        public SipRequestBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public SipRequestBuilder expires(Integer expires) {
            this.expires = expires;
            return this;
        }

        public SipRequestBuilder errorEvent(Event errorEvent) {
            this.errorEvent = errorEvent;
            return this;
        }

        public SipRequestBuilder okEvent(Event okEvent) {
            this.okEvent = okEvent;
            return this;
        }

        public SipRequestBuilder callId(String callId) {
            if (StringUtils.isNoneBlank(callId)) {
                this.callId = callId;
            }
            return this;
        }

        public String send() {
            SipRequestStrategy strategy = getStrategy();
            if (strategy == null) {
                throw new IllegalArgumentException("不支持的SIP方法: " + method);
            }

            if ("REGISTER".equalsIgnoreCase(method)) {
                return strategy.sendRequest(fromDevice, toDevice, content, callId, errorEvent, okEvent);
            } else if ("INVITE".equalsIgnoreCase(method) && subject != null) {
                return strategy.sendRequestWithSubject(fromDevice, toDevice, content, subject, callId, errorEvent, okEvent);
            } else {
                return strategy.sendRequest(fromDevice, toDevice, content, callId, errorEvent, okEvent);
            }
        }

        private SipRequestStrategy getStrategy() {
            if ("REGISTER".equalsIgnoreCase(method)) {
                return SipRequestStrategyFactory.getRegisterStrategy(expires);
            }
            return SipRequestStrategyFactory.getStrategy(method);
        }
    }

    /**
     * 创建请求建造者
     *
     * @param fromDevice 发送方设备
     * @param toDevice 接收方设备
     * @param method SIP方法
     * @return 请求建造者
     */
    public static SipRequestBuilder request(FromDevice fromDevice, ToDevice toDevice, String method) {
        return new SipRequestBuilder(fromDevice, toDevice, method);
    }

    // ==================== 兼容性方法 ====================

    /**
     * 发送SUBSCRIBE请求
     */
    public static String doSubscribeRequest(FromDevice fromDevice, ToDevice toDevice, String content) {
        return request(fromDevice, toDevice, "SUBSCRIBE")
            .content(content)
            .send();
    }

    public static String doSubscribeRequest(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent,
        Event okEvent) {
        return request(fromDevice, toDevice, "SUBSCRIBE")
            .content(content)
            .errorEvent(errorEvent)
            .okEvent(okEvent)
            .send();
    }

    /**
     * 发送MESSAGE请求
     */
    public static String doMessageRequest(FromDevice fromDevice, ToDevice toDevice, String content) {
        return request(fromDevice, toDevice, "MESSAGE")
            .content(content)
            .send();
    }

    public static String doMessageRequest(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        return request(fromDevice, toDevice, "MESSAGE")
            .content(content)
            .errorEvent(errorEvent)
            .okEvent(okEvent)
            .send();
    }

    /**
     * 发送NOTIFY请求
     */
    public static String doNotifyRequest(FromDevice fromDevice, ToDevice toDevice, String content, SubscribeInfo subscribeInfo) {
        return request(fromDevice, toDevice, "NOTIFY")
            .content(content)
            .send();
    }

    public static String doNotifyRequest(FromDevice fromDevice, ToDevice toDevice, String content, SubscribeInfo subscribeInfo, Event errorEvent,
        Event okEvent) {
        return request(fromDevice, toDevice, "NOTIFY")
            .content(content)
            .errorEvent(errorEvent)
            .okEvent(okEvent)
            .send();
    }

    /**
     * 发送INVITE请求
     */
    public static String doInviteRequest(FromDevice fromDevice, ToDevice toDevice, String content, String subject) {
        return request(fromDevice, toDevice, "INVITE")
            .content(content)
            .subject(subject)
            .send();
    }

    public static String doInviteRequest(FromDevice fromDevice, ToDevice toDevice, String content, String subject, Event errorEvent, Event okEvent) {
        return request(fromDevice, toDevice, "INVITE")
            .content(content)
            .subject(subject)
            .errorEvent(errorEvent)
            .okEvent(okEvent)
            .send();
    }

    /**
     * 发送INFO请求
     */
    public static String doInfoRequest(FromDevice fromDevice, ToDevice toDevice, String content) {
        return request(fromDevice, toDevice, "INFO")
            .content(content)
            .send();
    }

    public static String doInfoRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return request(fromDevice, toDevice, "INFO")
            .content(content)
            .callId(callId)
            .send();
    }

    public static String doInfoRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId, Event errorEvent, Event okEvent) {
        return request(fromDevice, toDevice, "INFO")
            .content(content)
            .callId(callId)
            .errorEvent(errorEvent)
            .okEvent(okEvent)
            .send();
    }

    /**
     * 发送BYE请求
     */
    public static String doByeRequest(FromDevice fromDevice, ToDevice toDevice) {
        return request(fromDevice, toDevice, "BYE")
            .send();
    }

    /**
     * 发送ACK请求
     */
    public static String doAckRequest(FromDevice fromDevice, ToDevice toDevice) {
        return request(fromDevice, toDevice, "ACK")
            .send();
    }

    public static String doAckRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return request(fromDevice, toDevice, "ACK")
            .content(content)
            .callId(callId)
            .send();
    }

    public static String doAckRequest(FromDevice fromDevice, ToDevice toDevice, String callId) {
        return request(fromDevice, toDevice, "ACK")
            .callId(callId)
            .send();
    }

    public static String doAckRequest(FromDevice fromDevice, SipURI sipURI, Response sipResponse) {
        // 将Response转换为SIPResponse
        gov.nist.javax.sip.message.SIPResponse sipResponseImpl = (gov.nist.javax.sip.message.SIPResponse)sipResponse;
        Request messageRequest = SipRequestBuilderFactory.getAckBuilder().buildAckRequest(fromDevice, sipURI, sipResponseImpl);
        SipMessageTransmitter.transmitMessage(fromDevice.getIp(), messageRequest);
        return sipResponseImpl.getCallId().getCallId();
    }

    /**
     * 发送REGISTER请求
     */
    public static String doRegisterRequest(FromDevice fromDevice, ToDevice toDevice, Integer expires) {
        return request(fromDevice, toDevice, "REGISTER")
            .expires(expires)
            .send();
    }

    public static String doRegisterRequest(FromDevice fromDevice, ToDevice toDevice, Integer expires, Event event) {
        return request(fromDevice, toDevice, "REGISTER")
            .expires(expires)
            .errorEvent(event)
            .send();
    }

    public static String doRegisterRequest(FromDevice fromDevice, ToDevice toDevice, Integer expires, String callId, Event errorEvent,
        Event okEvent) {
        return request(fromDevice, toDevice, "REGISTER")
            .expires(expires)
            .callId(callId)
            .errorEvent(errorEvent)
            .okEvent(okEvent)
            .send();
    }

    // ==================== 消息传输方法 ====================

    /**
     * 传输消息（兼容性方法）
     */
    public static void transmitRequest(String ip, Message message) {
        SipMessageTransmitter.transmitMessage(ip, message);
    }

    public static void transmitRequest(String ip, Message message, Event errorEvent) {
        SipMessageTransmitter.transmitMessage(ip, message, errorEvent);
    }

    public static void transmitRequestSuccess(String ip, Message message, Event okEvent) {
        SipMessageTransmitter.transmitMessageSuccess(ip, message, okEvent);
    }

    public static void transmitRequest(String ip, Message message, Event errorEvent, Event okEvent) {
        SipMessageTransmitter.transmitMessage(ip, message, errorEvent, okEvent);
    }

    // ==================== 事务管理方法 ====================

    /**
     * 获取服务器事务（兼容性方法）
     */
    public static ServerTransaction getServerTransaction(Request request) {
        return SipTransactionManager.getServerTransaction(request);
    }

    public static ServerTransaction getServerTransaction(Request request, String ip) {
        return SipTransactionManager.getServerTransaction(request, ip);
    }
}
