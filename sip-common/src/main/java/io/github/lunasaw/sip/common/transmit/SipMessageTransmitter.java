package io.github.lunasaw.sip.common.transmit;

import gov.nist.javax.sip.SipProviderImpl;
import io.github.lunasaw.sip.common.config.SipCommonContextHolder;
import io.github.lunasaw.sip.common.constant.Constant;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.transmit.event.SipSubscribe;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.extern.slf4j.Slf4j;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.function.BiConsumer;

/**
 * SIP消息传输器
 * 负责SIP消息的传输和事件订阅管理
 *
 * @author lin
 */
@Slf4j
public class SipMessageTransmitter {

    /**
     * 传输消息
     *
     * @param ip 目标IP
     * @param message 消息
     */
    public static void transmitMessage(String ip, Message message) {
        transmitMessage(ip, message, null, null);
    }

    /**
     * 传输消息（带错误事件）
     *
     * @param ip 目标IP
     * @param message 消息
     * @param errorEvent 错误事件
     */
    public static void transmitMessage(String ip, Message message, Event errorEvent) {
        transmitMessage(ip, message, errorEvent, null);
    }

    /**
     * 传输消息（带成功事件）
     *
     * @param ip 目标IP
     * @param message 消息
     * @param okEvent 成功事件
     */
    public static void transmitMessageSuccess(String ip, Message message, Event okEvent) {
        transmitMessage(ip, message, null, okEvent);
    }

    /**
     * 传输消息（带事件处理）
     *
     * @param ip 目标IP
     * @param message 消息
     * @param errorEvent 错误事件
     * @param okEvent 成功事件
     */
    public static void transmitMessage(String ip, Message message, Event errorEvent, Event okEvent) {
        // 预处理消息
        preprocessMessage(message);

        // 设置事件订阅
        setupEventSubscriptions(message, errorEvent, okEvent);

        // 发送消息
        sendMessage(ip, message);
    }

    /**
     * 预处理消息
     *
     * @param message 消息
     */
    private static void preprocessMessage(Message message) {
        // 添加User-Agent头
        if (message.getHeader(UserAgentHeader.NAME) == null) {
            message.addHeader(SipRequestUtils.createUserAgentHeader(SipCommonContextHolder.getUserAgent()));
        }
    }

    /**
     * 设置事件订阅
     *
     * <p>1.7.0：可见性从 private 提升至 public，使
     * {@link io.github.lunasaw.sip.common.transmit.strategy.impl.InviteRequestStrategy} /
     * {@link io.github.lunasaw.sip.common.transmit.strategy.impl.SubscribeRequestStrategy}
     * 在走 stateful 通道时能复用同一回调注册逻辑。
     *
     * @param message    消息
     * @param errorEvent 错误事件
     * @param okEvent    成功事件
     */
    public static void setupEventSubscriptions(Message message, Event errorEvent, Event okEvent) {
        CallIdHeader callIdHeader = (CallIdHeader)message.getHeader(CallIdHeader.NAME);
        if (callIdHeader == null) {
            return;
        }

        // 添加错误订阅
        if (errorEvent != null) {
            SipSubscribe.addErrorSubscribe(callIdHeader.getCallId(), (eventResult -> {
                errorEvent.response(eventResult);
                SipSubscribe.removeErrorSubscribe(eventResult.getCallId());
                SipSubscribe.removeOkSubscribe(eventResult.getCallId());
            }));
        }

        // 添加成功订阅
        if (okEvent != null) {
            SipSubscribe.addOkSubscribe(callIdHeader.getCallId(), eventResult -> {
                okEvent.response(eventResult);
                SipSubscribe.removeOkSubscribe(eventResult.getCallId());
                SipSubscribe.removeErrorSubscribe(eventResult.getCallId());
            });
        }
    }

    /**
     * 发送消息
     *
     * @param ip 目标IP
     * @param message 消息
     */
    private static void sendMessage(String ip, Message message) {
        String transport = getTransport(message);

        try {
            if (Constant.TCP.equalsIgnoreCase(transport)) {
                sendTcpMessage(ip, message);
            } else if (Constant.UDP.equalsIgnoreCase(transport)) {
                sendUdpMessage(ip, message);
            }
        } catch (SipException e) {
            log.error("发送SIP消息失败", e);
            throw new RuntimeException("发送SIP消息失败", e);
        }
    }

    /**
     * 发送TCP消息
     *
     * @param ip 目标IP
     * @param message 消息
     * @throws SipException SIP异常
     */
    private static void sendTcpMessage(String ip, Message message) throws SipException {
        SipProviderImpl tcpSipProvider = SipLayer.getTcpSipProvider(ip);
        if (tcpSipProvider == null) {
            log.error("[发送信息失败] 未找到tcp://{}的监听信息", ip);
            return;
        }

        if (message instanceof Request) {
            tcpSipProvider.sendRequest((Request)message);
        } else if (message instanceof Response) {
            tcpSipProvider.sendResponse((Response)message);
        }
    }

    /**
     * 发送UDP消息
     *
     * @param ip 目标IP
     * @param message 消息
     * @throws SipException SIP异常
     */
    private static void sendUdpMessage(String ip, Message message) throws SipException {
        SipProviderImpl sipProvider = SipLayer.getUdpSipProvider(ip);
        if (sipProvider == null) {
            log.error("[发送信息失败] 未找到udp://{}的监听信息", ip);
            return;
        }

        if (message instanceof Request) {
            sipProvider.sendRequest((Request)message);
        } else if (message instanceof Response) {
            sipProvider.sendResponse((Response)message);
        }
    }

    /**
     * 获取传输协议
     *
     * @param message 消息
     * @return 传输协议
     */
    private static String getTransport(Message message) {
        ViaHeader viaHeader = (ViaHeader)message.getHeader(ViaHeader.NAME);
        String transport = "UDP";
        if (viaHeader == null) {
            log.warn("[消息头缺失]： ViaHeader， 使用默认的UDP方式处理数据");
        } else {
            transport = viaHeader.getTransport();
        }
        return transport;
    }

    // ==================== Stateful 发送（1.7.0 新增） ====================

    /**
     * 有状态发送：走 ClientTransaction，JAIN-SIP 在 INVITE / SUBSCRIBE 等会自动创建 Dialog。
     * 调用方负责在发送后处理返回的 Dialog（如注册到 {@link DialogRegistry}）。
     *
     * <p>仅供需要后续 dialog 内消息的方法使用（INVITE / SUBSCRIBE）。REGISTER / MESSAGE /
     * NOTIFY 等无 dialog 语义的请求继续用 {@link #transmitMessage} 无状态发送，避免 Timer F
     * 重传与现有重试逻辑冲突。
     *
     * @param ip      目标 IP
     * @param request 请求
     * @return 创建的 Dialog（INVITE / SUBSCRIBE 时非 null）
     */
    public static Dialog transmitStateful(String ip, Request request) {
        preprocessMessage(request);
        String transport = getTransport(request);
        try {
            SipProviderImpl provider = Constant.TCP.equalsIgnoreCase(transport)
                    ? SipLayer.getTcpSipProvider(ip)
                    : SipLayer.getUdpSipProvider(ip);
            if (provider == null) {
                log.error("[发送信息失败] 未找到 {}://{} 的监听信息", transport, ip);
                return null;
            }
            ClientTransaction ct = provider.getNewClientTransaction(request);
            ct.sendRequest();
            return ct.getDialog();
        } catch (SipException e) {
            log.error("有状态发送 SIP 消息失败", e);
            throw new RuntimeException("有状态发送 SIP 消息失败", e);
        }
    }

    /**
     * 有状态发送（先注册 dialog 再发送，消除响应竞态）。
     *
     * <p>JAIN-SIP {@code getNewClientTransaction(req)} 已经创建 Dialog 引用，不必等到 sendRequest
     * 之后再注册。先注册后发送可避免 200 OK 在本地 register 之前到达 ResponseProcessor 的
     * 罕见但可能的窗口（同机回环 / 极低延迟链路）。
     *
     * @param ip       目标 IP
     * @param request  请求
     * @param callId   提前透传给 register 回调，避免再从 request 头里取
     * @param register 注册回调，由调用方决定注册到哪个 registry（一般是 DialogRegistry::register
     *                 或带 expiresAtMs / kind 的 BiConsumer 包装）
     * @return 已注册的 Dialog（可能为 null，如 provider 缺失）
     */
    public static Dialog transmitStatefulPreRegister(String ip, Request request, String callId,
                                                      BiConsumer<String, Dialog> register) {
        preprocessMessage(request);
        String transport = getTransport(request);
        try {
            SipProviderImpl provider = Constant.TCP.equalsIgnoreCase(transport)
                    ? SipLayer.getTcpSipProvider(ip)
                    : SipLayer.getUdpSipProvider(ip);
            if (provider == null) {
                log.error("[发送信息失败] 未找到 {}://{} 的监听信息", transport, ip);
                return null;
            }
            ClientTransaction ct = provider.getNewClientTransaction(request);
            Dialog dialog = ct.getDialog();
            if (dialog != null && register != null) {
                register.accept(callId, dialog);
            }
            ct.sendRequest();
            return dialog;
        } catch (SipException e) {
            log.error("有状态发送 SIP 消息失败", e);
            throw new RuntimeException("有状态发送 SIP 消息失败", e);
        }
    }
}