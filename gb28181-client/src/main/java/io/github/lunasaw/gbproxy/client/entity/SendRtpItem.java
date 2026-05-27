package io.github.lunasaw.gbproxy.client.entity;

import lombok.Data;

/**
 * RTP 推流任务信息，记录一次媒体流推送的目标地址、状态及关联标识。
 */
@Data
public class SendRtpItem {

    /**
     * 推流ip
     */
    private String ip;

    /**
     * 推流端口
     */
    private int port;

    /**
     * 推流标识
     */
    private String ssrc;

    /**
     * 平台id
     */
    private String platformId;

    /**
     * 对应设备id
     */
    private String deviceId;

    /**
     * 直播流的应用名
     */
    private String app;

    /**
     * 通道id
     */
    private String channelId;

    /**
     * 推流状态
     * 0 等待设备推流上来
     * 1 等待上级平台回复ack
     * 2 推流中
     */
    private int status = 0;


    /**
     * 设备推流的streamId
     */
    private String streamId;

    /**
     * 是否为tcp
     */
    private boolean tcp;

    /**
     * 是否为tcp主动模式
     */
    private boolean tcpActive;

    /**
     * 自己推流使用的端口
     */
    private int localPort;

    /**
     * 使用的流媒体
     */
    private String mediaServerId;

}
