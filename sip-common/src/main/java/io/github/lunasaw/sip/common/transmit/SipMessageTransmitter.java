package io.github.lunasaw.sip.common.transmit;

import gov.nist.javax.sip.SipProviderImpl;
import io.github.lunasaw.sip.common.constant.Constant;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.transmit.event.SipSubscribe;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.extern.slf4j.Slf4j;

import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

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
            message.addHeader(SipRequestUtils.createUserAgentHeader(Constant.AGENT));
        }
    }

    /**
     * 设置事件订阅
     *
     * @param message 消息
     * @param errorEvent 错误事件
     * @param okEvent 成功事件
     */
    private static void setupEventSubscriptions(Message message, Event errorEvent, Event okEvent) {
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
}