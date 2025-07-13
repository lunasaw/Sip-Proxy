package io.github.lunasaw.sip.common.transmit.strategy;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.event.Event;

/**
 * SIP请求发送策略接口
 * 定义通用的请求发送模式，支持不同的请求类型
 *
 * @author lin
 */
public interface SipRequestStrategy {

    /**
     * 发送请求
     *
     * @param fromDevice 发送方设备
     * @param toDevice 接收方设备
     * @param content 请求内容
     * @param callId 呼叫ID
     * @param errorEvent 错误事件处理器
     * @param okEvent 成功事件处理器
     * @return 呼叫ID
     */
    String sendRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId, Event errorEvent, Event okEvent);

    /**
     * 发送带主题的请求
     *
     * @param fromDevice 发送方设备
     * @param toDevice 接收方设备
     * @param content 请求内容
     * @param subject 主题
     * @param callId 呼叫ID
     * @param errorEvent 错误事件处理器
     * @param okEvent 成功事件处理器
     * @return 呼叫ID
     */
    default String sendRequestWithSubject(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId, Event errorEvent,
        Event okEvent) {
        return sendRequest(fromDevice, toDevice, content, callId, errorEvent, okEvent);
    }
}