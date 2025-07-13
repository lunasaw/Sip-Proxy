package io.github.lunasaw.sip.common.transmit.request;

import javax.sip.message.Request;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;

/**
 * SIP请求构建器工厂类
 * 提供统一的构建器获取接口和便捷的构建方法
 *
 * @author luna
 */
public class SipRequestBuilderFactory {

    // 单例构建器实例
    private static final MessageRequestBuilder   MESSAGE_BUILDER   = new MessageRequestBuilder();
    private static final InviteRequestBuilder    INVITE_BUILDER    = new InviteRequestBuilder();
    private static final ByeRequestBuilder       BYE_BUILDER       = new ByeRequestBuilder();
    private static final RegisterRequestBuilder  REGISTER_BUILDER  = new RegisterRequestBuilder();
    private static final SubscribeRequestBuilder SUBSCRIBE_BUILDER = new SubscribeRequestBuilder();
    private static final InfoRequestBuilder      INFO_BUILDER      = new InfoRequestBuilder();
    private static final AckRequestBuilder       ACK_BUILDER       = new AckRequestBuilder();
    private static final NotifyRequestBuilder    NOTIFY_BUILDER    = new NotifyRequestBuilder();

    /**
     * 获取MESSAGE请求构建器
     */
    public static MessageRequestBuilder getMessageBuilder() {
        return MESSAGE_BUILDER;
    }

    /**
     * 获取INVITE请求构建器
     */
    public static InviteRequestBuilder getInviteBuilder() {
        return INVITE_BUILDER;
    }

    /**
     * 获取BYE请求构建器
     */
    public static ByeRequestBuilder getByeBuilder() {
        return BYE_BUILDER;
    }

    /**
     * 获取REGISTER请求构建器
     */
    public static RegisterRequestBuilder getRegisterBuilder() {
        return REGISTER_BUILDER;
    }

    /**
     * 获取SUBSCRIBE请求构建器
     */
    public static SubscribeRequestBuilder getSubscribeBuilder() {
        return SUBSCRIBE_BUILDER;
    }

    /**
     * 获取INFO请求构建器
     */
    public static InfoRequestBuilder getInfoBuilder() {
        return INFO_BUILDER;
    }

    /**
     * 获取ACK请求构建器
     */
    public static AckRequestBuilder getAckBuilder() {
        return ACK_BUILDER;
    }

    /**
     * 获取NOTIFY请求构建器
     */
    public static NotifyRequestBuilder getNotifyBuilder() {
        return NOTIFY_BUILDER;
    }

    // ========== 便捷构建方法 ==========

    /**
     * 创建MESSAGE请求
     */
    public static Request createMessageRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return MESSAGE_BUILDER.buildMessageRequest(fromDevice, toDevice, content, callId);
    }

    /**
     * 创建INVITE请求
     */
    public static Request createInviteRequest(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId) {
        return INVITE_BUILDER.buildInviteRequest(fromDevice, toDevice, content, subject, callId);
    }

    /**
     * 创建回放INVITE请求
     */
    public static Request createPlaybackInviteRequest(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId) {
        return INVITE_BUILDER.buildPlaybackInviteRequest(fromDevice, toDevice, content, subject, callId);
    }

    /**
     * 创建BYE请求
     */
    public static Request createByeRequest(FromDevice fromDevice, ToDevice toDevice, String callId) {
        return BYE_BUILDER.buildByeRequest(fromDevice, toDevice, callId);
    }

    /**
     * 创建REGISTER请求
     */
    public static Request createRegisterRequest(FromDevice fromDevice, ToDevice toDevice, Integer expires, String callId) {
        return REGISTER_BUILDER.buildRegisterRequest(fromDevice, toDevice, expires, callId);
    }

    /**
     * 创建带认证的REGISTER请求
     */
    public static Request createRegisterRequestWithAuth(FromDevice fromDevice, ToDevice toDevice, String callId, Integer expires,
        javax.sip.header.WWWAuthenticateHeader www) {
        return REGISTER_BUILDER.buildRegisterRequestWithAuth(fromDevice, toDevice, callId, expires, www);
    }

    /**
     * 创建SUBSCRIBE请求
     */
    public static Request createSubscribeRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return SUBSCRIBE_BUILDER.buildSubscribeRequest(fromDevice, toDevice, content, null, callId);
    }

    /**
     * 创建INFO请求
     */
    public static Request createInfoRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return INFO_BUILDER.buildInfoRequest(fromDevice, toDevice, content, callId);
    }

    /**
     * 创建ACK请求
     */
    public static Request createAckRequest(FromDevice fromDevice, ToDevice toDevice, String callId) {
        return ACK_BUILDER.buildAckRequest(fromDevice, toDevice, callId);
    }

    /**
     * 创建带内容的ACK请求
     */
    public static Request createAckRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return ACK_BUILDER.buildAckRequest(fromDevice, toDevice, content, callId);
    }

    /**
     * 创建NOTIFY请求
     */
    public static Request createNotifyRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return NOTIFY_BUILDER.buildNotifyRequest(fromDevice, toDevice, content, null, callId);
    }

    /**
     * 通用SIP请求构建方法
     */
    public static Request createSipRequest(FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        return createSipRequest(fromDevice, toDevice, sipMessage, null);
    }

    /**
     * 通用SIP请求构建方法（带订阅信息）
     */
    public static Request createSipRequest(FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage, SubscribeInfo subscribeInfo) {
        // 根据方法类型选择合适的构建器
        switch (sipMessage.getMethod()) {
            case Request.MESSAGE:
                return MESSAGE_BUILDER.build(fromDevice, toDevice, sipMessage, subscribeInfo);
            case Request.INVITE:
                return INVITE_BUILDER.build(fromDevice, toDevice, sipMessage, subscribeInfo);
            case Request.BYE:
                return BYE_BUILDER.build(fromDevice, toDevice, sipMessage, subscribeInfo);
            case Request.REGISTER:
                return REGISTER_BUILDER.build(fromDevice, toDevice, sipMessage, subscribeInfo);
            case Request.SUBSCRIBE:
                return SUBSCRIBE_BUILDER.build(fromDevice, toDevice, sipMessage, subscribeInfo);
            case Request.INFO:
                return INFO_BUILDER.build(fromDevice, toDevice, sipMessage, subscribeInfo);
            case Request.ACK:
                return ACK_BUILDER.build(fromDevice, toDevice, sipMessage, subscribeInfo);
            case Request.NOTIFY:
                return NOTIFY_BUILDER.build(fromDevice, toDevice, sipMessage, subscribeInfo);
            default:
                throw new IllegalArgumentException("Unsupported SIP method: " + sipMessage.getMethod());
        }
    }
}