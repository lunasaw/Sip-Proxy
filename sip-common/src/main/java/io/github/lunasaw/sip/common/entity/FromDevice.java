package io.github.lunasaw.sip.common.entity;

import io.github.lunasaw.sip.common.config.SipCommonContextHolder;

import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.Data;

/**
 * SIP消息发送方设备，继承Device并携带fromTag和User-Agent标识。
 */
@Data
public class FromDevice extends Device {

    /**
     * fromTag用于标识SIP消息的发送方，每个SIP消息都应该包含一个fromTag字段，这个字段的值是由发送方生成的随机字符串，用于标识该消息的发送方。在SIP消息的传输过程中，每个中间节点都会将fromTag字段的值保留不变，以确保消息的发送方不变。
     */
    private String fromTag;

    /**
     * 发送设备标识 类似浏览器User-Agent
     */
    private String agent;


    /**
     * 创建默认 UDP/TCP_PASSIVE 模式的 FromDevice 实例，自动生成 fromTag 和 User-Agent。
     *
     * @param userId 用户ID
     * @param ip     本地IP
     * @param port   本地端口
     * @return FromDevice实例
     */
    public static FromDevice getInstance(String userId, String ip, int port) {
        FromDevice fromDevice = new FromDevice();
        fromDevice.setUserId(userId);
        fromDevice.setIp(ip);
        fromDevice.setPort(port);
        fromDevice.setTransport("UDP");
        fromDevice.setStreamMode("TCP_PASSIVE");
        fromDevice.setFromTag(SipRequestUtils.getNewFromTag());
        fromDevice.setAgent(SipCommonContextHolder.getUserAgent());
        return fromDevice;
    }

}
