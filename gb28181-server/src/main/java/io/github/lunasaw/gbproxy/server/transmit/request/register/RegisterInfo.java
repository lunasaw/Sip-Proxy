package io.github.lunasaw.gbproxy.server.transmit.request.register;

import lombok.Data;

import java.util.Date;

/**
 * @author luna
 * @date 2023/10/18
 */
@Data
public class RegisterInfo {

    /**
     * 注册时间
     */
    private Date registerTime;

    /**
     * 注册过期时间
     */
    private Integer expire;

    /**
     * 注册协议
     */
    private String transport;

    /**
     * 设备注册地址当前IP
     */
    private String localIp;

    /**
     * nat转换后看到的IP
     */
    private String remoteIp;

    /**
     * 经过rpotocol转换后的端口
     */
    private Integer remotePort;
}
