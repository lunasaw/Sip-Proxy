package io.github.lunasaw.sip.common.transmit;

import gov.nist.javax.sip.DialogExt;
import gov.nist.javax.sip.SipProviderImpl;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.transmit.request.SipRequestBuilderFactory;
import io.github.lunasaw.sip.common.transmit.strategy.SipRequestStrategy;
import io.github.lunasaw.sip.common.transmit.strategy.SipRequestStrategyFactory;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.context.SipTransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * SIP消息发送器（重构版）
 * 使用策略模式和建造者模式，提供简洁的API接口
 *
 * @author lin
 */
@Slf4j
public class SipSender {


    /**
     * 创建请求建造者
     *
     * @param fromDevice 发送方设备
     * @param toDevice   接收方设备
     * @param method     SIP方法
     * @return 请求建造者
     */
    public static SipRequestBuilder request(FromDevice fromDevice, ToDevice toDevice, String method) {
        return new SipRequestBuilder(fromDevice, toDevice, method);
    }

    /**
     * 发送SUBSCRIBE请求
     */
    public static String doSubscribeRequest(FromDevice fromDevice, ToDevice toDevice, String content, SubscribeInfo subscribeInfo) {
        return request(fromDevice, toDevice, "SUBSCRIBE")
                .content(content)
                .subscribeInfo(subscribeInfo)
                .send();
    }

    // ==================== 兼容性方法 ====================

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
    public static String doNotifyRequest(FromDevice fromDevice, ToDevice toDevice, String content) {
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
                .subscribeInfo(subscribeInfo)
                .send();
    }


    public static String doNotifyRequest(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent,
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

    public static String doInviteRequest(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        return request(fromDevice, toDevice, "INVITE")
                .content(content)
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

    public static String doInfoRequest(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        return request(fromDevice, toDevice, "INFO")
                .content(content)
                .errorEvent(errorEvent)
                .okEvent(okEvent)
                .send();
    }

    /**
     * 发送 BYE 请求（dialog-aware 路径）。
     *
     * <p>1.7.0 起 BYE 必须基于已 confirmed 的 dialog —— JAIN-SIP 自动携带 to-tag / Route Set /
     * 正确 CSeq。旧 {@code doByeRequest(FromDevice, ToDevice)} 签名因不带 to-tag 触发设备 481，
     * 已删除。
     *
     * @param callId INVITE 200 OK 的 Call-ID（由 {@link DialogRegistry} 维护）
     * @return callId
     * @throws DialogNotFoundException 当 callId 找不到对应 dialog 时
     * @throws IllegalStateException   当 dialog 不在 CONFIRMED 状态时
     */
    public static String doByeRequest(String callId) {
        Dialog dialog = DialogRegistry.get(callId);
        if (dialog == null) {
            throw new DialogNotFoundException("no dialog for callId=" + callId
                    + " — INVITE 200 OK 未建立 dialog 或已 terminate");
        }
        if (dialog.getState() != DialogState.CONFIRMED) {
            throw new IllegalStateException("dialog not confirmed: callId=" + callId
                    + ", state=" + dialog.getState() + " — 早 dialog 阶段不应发 BYE，应发 CANCEL");
        }
        try {
            Request bye = dialog.createRequest(Request.BYE);
            SipProviderImpl provider = (SipProviderImpl) ((DialogExt) dialog).getSipProvider();
            ClientTransaction ct = provider.getNewClientTransaction(bye);
            dialog.sendRequest(ct);
            return callId;
        } catch (SipException e) {
            throw new RuntimeException("发送 BYE 失败: callId=" + callId, e);
        }
    }

    /**
     * 发送 SUBSCRIBE 续订 / 退订（dialog-aware 路径）。
     *
     * <p>必须基于已 confirmed 的 SUBSCRIBE dialog（初始 SUBSCRIBE 200 OK 后注册到
     * {@link DialogRegistry}）。续订使用与初始 SUBSCRIBE 同一个 Call-ID，JAIN-SIP 自动携带
     * to-tag / 正确 CSeq / Route Set。
     *
     * @param callId  初始 SUBSCRIBE 的 Call-ID
     * @param content body（XML），通常与初始 SUBSCRIBE 相同；为空则 body 也为空
     * @param expires 续订时长（秒）；0 表示退订
     * @return callId
     * @throws DialogNotFoundException 当 callId 找不到对应 dialog 时
     * @throws IllegalStateException   当 dialog 不在 CONFIRMED 状态时
     */
    public static String doSubscribeRefresh(String callId, String content, int expires) {
        Dialog dialog = DialogRegistry.get(callId);
        if (dialog == null) {
            throw new DialogNotFoundException("no SUBSCRIBE dialog for callId=" + callId
                    + " — 初始 SUBSCRIBE 200 OK 未建立 dialog 或已自然过期");
        }
        if (dialog.getState() != DialogState.CONFIRMED) {
            throw new IllegalStateException("SUBSCRIBE dialog not confirmed: callId=" + callId
                    + ", state=" + dialog.getState());
        }
        try {
            Request req = dialog.createRequest(Request.SUBSCRIBE);
            ExpiresHeader expHeader = SipRequestUtils.createExpiresHeader(expires);
            req.removeHeader(ExpiresHeader.NAME);
            req.addHeader(expHeader);
            if (StringUtils.isNotBlank(content)) {
                req.setContent(content, ContentTypeEnum.APPLICATION_XML.getContentTypeHeader());
            }
            SipProviderImpl provider = (SipProviderImpl) ((DialogExt) dialog).getSipProvider();
            ClientTransaction ct = provider.getNewClientTransaction(req);
            dialog.sendRequest(ct);
            // 退订（expires=0）：等设备发回 NOTIFY: Subscription-State: terminated 后由
            // processDialogTerminated 清理；为兜底，把 entry 的 expiresAt 重置为 60s 内
            if (expires == 0) {
                DialogRegistry.register(callId, dialog,
                        System.currentTimeMillis() + 60_000L, DialogRegistry.KIND_SUBSCRIBE);
            }
            return callId;
        } catch (SipException | java.text.ParseException e) {
            throw new RuntimeException("发送 SUBSCRIBE 失败: callId=" + callId, e);
        }
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


    public static String doAckRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId, Event errorEvent,
                                      Event okEvent) {
        return request(fromDevice, toDevice, "ACK")
                .content(content)
                .okEvent(okEvent)
                .errorEvent(errorEvent)
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
        gov.nist.javax.sip.message.SIPResponse sipResponseImpl = (gov.nist.javax.sip.message.SIPResponse) sipResponse;
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

    /**
     * 传输消息（兼容性方法）
     */
    public static void transmitRequest(String ip, Message message) {
        SipMessageTransmitter.transmitMessage(ip, message);
    }

    // ==================== 消息传输方法 ====================

    public static void transmitRequest(String ip, Message message, Event errorEvent) {
        SipMessageTransmitter.transmitMessage(ip, message, errorEvent);
    }

    public static void transmitRequestSuccess(String ip, Message message, Event okEvent) {
        SipMessageTransmitter.transmitMessageSuccess(ip, message, okEvent);
    }

    public static void transmitRequest(String ip, Message message, Event errorEvent, Event okEvent) {
        SipMessageTransmitter.transmitMessage(ip, message, errorEvent, okEvent);
    }

    /**
     * 获取服务器事务（兼容性方法）
     */
    public static ServerTransaction getServerTransaction(Request request) {
        return SipServerTransactionProvider.getServerTransaction(request);
    }

    // ==================== 事务管理方法 ====================

    public static ServerTransaction getServerTransaction(Request request, String ip) {
        return SipServerTransactionProvider.getServerTransaction(request, ip);
    }

    /**
     * SIP请求建造者
     * 提供流式API来构建和发送SIP请求
     */
    public static class SipRequestBuilder {
        private final FromDevice fromDevice;
        private final ToDevice toDevice;
        private final String method;
        private String content;
        private String subject;
        private SubscribeInfo subscribeInfo;
        private Integer expires;
        private Event errorEvent;
        private Event okEvent;
        private String callId;

        public SipRequestBuilder(FromDevice fromDevice, ToDevice toDevice, String method) {
            this.fromDevice = fromDevice;
            this.toDevice = toDevice;
            this.method = method;

            // Call-ID优先级：ThreadLocal事务上下文 > ToDevice.callId > 新生成
            String contextCallId = SipTransactionContext.getCurrentCallId();
            if (StringUtils.isNotBlank(contextCallId)) {
                this.callId = contextCallId;
                log.debug("使用SIP事务上下文的Call-ID: {}", contextCallId);
            } else if (StringUtils.isNotBlank(toDevice.getCallId())) {
                this.callId = toDevice.getCallId();
                log.debug("使用ToDevice的Call-ID: {}", this.callId);
            } else {
                this.callId = SipRequestUtils.getNewCallId();
                log.debug("生成新的Call-ID: {}", this.callId);
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

        public SipRequestBuilder subscribeInfo(SubscribeInfo subscribeInfo) {
            this.subscribeInfo = subscribeInfo;
            return this;
        }

        public String send() {
            SipRequestStrategy strategy = getStrategy();
            if (strategy == null) {
                throw new IllegalArgumentException("不支持的SIP方法: " + method);
            }

            if ("INVITE".equalsIgnoreCase(method) && subject != null) {
                return strategy.sendRequestWithSubject(fromDevice, toDevice, content, subject, callId, errorEvent, okEvent);
            } else if ("SUBSCRIBE".equalsIgnoreCase(method) && subscribeInfo != null) {
                return strategy.sendRequestWithSubscribe(fromDevice, toDevice, content, subscribeInfo, callId, errorEvent, okEvent);
            }

            if (subscribeInfo != null) {
                return strategy.sendRequestWithSubscribe(fromDevice, toDevice, content, subscribeInfo, callId, errorEvent, okEvent);
            }
            return strategy.sendRequest(fromDevice, toDevice, content, callId, errorEvent, okEvent);
        }

        private SipRequestStrategy getStrategy() {
            if ("REGISTER".equalsIgnoreCase(method)) {
                return SipRequestStrategyFactory.getRegisterStrategy(expires);
            }
            return SipRequestStrategyFactory.getStrategy(method);
        }
    }
}
