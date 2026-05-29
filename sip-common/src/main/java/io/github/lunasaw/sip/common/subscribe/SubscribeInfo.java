package io.github.lunasaw.sip.common.subscribe;

import javax.sip.header.EventHeader;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SIP订阅信息，封装一次SUBSCRIBE请求的关键参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeInfo {

    /** 订阅唯一标识。 */
    private String id;
    /** 订阅有效期（秒）。 */
    private int expires;
    /** 事件ID。 */
    private String eventId;
    /** 事件类型。 */
    private String eventType;
    /**
     * 上一次的请求
     */
    private SIPRequest request;
    /** 上一次的响应。 */
    private SIPResponse response;
    /**
     * 以下为可选字段
     */
    /** 序列号，可选。 */
    private String sn;
    /** GPS上报间隔，可选。 */
    private int gpsInterval;

    /** 订阅状态，如 active/pending/terminated。 */
    private String subscriptionState;

    /**
     * 从 SIPRequest 构造订阅信息，自动提取 expires、eventId、eventType。
     *
     * @param request SIP SUBSCRIBE 请求
     * @param id      订阅唯一标识
     */
    public SubscribeInfo(SIPRequest request, String id) {
        this.id = id;
        this.request = request;
        this.expires = request.getExpires().getExpires();
        EventHeader eventHeader = (EventHeader) request.getHeader(EventHeader.NAME);
        this.eventId = eventHeader.getEventId();
        this.eventType = eventHeader.getEventType();
    }

}
