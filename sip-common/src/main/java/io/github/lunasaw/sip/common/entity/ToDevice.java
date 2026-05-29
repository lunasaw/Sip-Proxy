package io.github.lunasaw.sip.common.entity;

import lombok.Data;

/**
 * SIP消息接收方设备，继承Device并携带对话标识、订阅等字段。
 */
@Data
public class ToDevice extends Device {

    /**
     * 及联的处理的时候，需要将上游的toTag往下带
     * toTag也是SIP协议中的一个字段，用于标识SIP消息的接收方。每个SIP消息都应该包含一个toTag字段，这个字段的值是由接收方生成的随机字符串，
     * 用于标识该消息的接收方。在SIP消息的传输过程中，每个中间节点都会将toTag字段的值保留不变，以确保消息的接收方不变。
     */
    private String toTag;

    /**
     * 需要想下游携带的信息
     */
    private String subject;

    private Integer expires;

    /** 订阅事件类型。 */
    private String eventType;

    /** 订阅事件ID。 */
    private String eventId;

    /** 会话唯一标识 Call-ID。 */
    private String callId;

    /**
     * 创建默认 UDP/TCP_PASSIVE 模式的 ToDevice 实例。
     *
     * @param userId 用户ID
     * @param ip     目标IP
     * @param port   目标端口
     * @return ToDevice实例
     */
    public static ToDevice getInstance(String userId, String ip, int port) {
        ToDevice toDevice = new ToDevice();
        toDevice.setUserId(userId);
        toDevice.setIp(ip);
        toDevice.setPort(port);
        toDevice.setTransport("UDP");
        toDevice.setStreamMode("TCP_PASSIVE");
        toDevice.setToTag(null);
        return toDevice;
    }
}
