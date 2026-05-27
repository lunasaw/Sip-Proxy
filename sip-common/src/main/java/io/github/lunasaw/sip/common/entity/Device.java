package io.github.lunasaw.sip.common.entity;

import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * SIP设备抽象基类，封装设备的网络地址、传输协议等公共属性。
 */
@Data
public abstract class Device {

    /**
     * 用户Id
     */
    private String userId;

    /**
     * 域
     */
    private String realm;

    /**
     * 传输协议
     * UDP/TCP
     */
    private String transport;

    /**
     * 数据流传输模式
     * UDP:udp传输
     * TCP-ACTIVE：tcp主动模式
     * TCP-PASSIVE：tcp被动模式
     */
    private String streamMode;

    /**
     * wan地址_ip
     */
    private String ip;

    /**
     * wan地址_port
     */
    private int    port;

    /**
     * wan地址
     */
    private String hostAddress;

    /**
     * 密码
     */
    private String password;


    /**
     * 编码
     */
    private String charset;

    /**
     * 获取字符集，未设置时默认返回 UTF-8。
     *
     * @return 字符集名称
     */
    public String getCharset() {
        if (this.charset == null) {
            return "UTF-8";
        }
        return charset;
    }

    /**
     * 设置 WAN 地址字符串。
     *
     * @param hostAddress 地址字符串，格式为 ip:port
     */
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    /**
     * 获取 WAN 地址字符串，未设置时由 ip 和 port 拼接。
     *
     * @return 地址字符串，格式为 ip:port
     */
    public String getHostAddress() {
        if (StringUtils.isBlank(hostAddress)) {
            return ip + ":" + port;
        }
        return hostAddress;
    }
}
