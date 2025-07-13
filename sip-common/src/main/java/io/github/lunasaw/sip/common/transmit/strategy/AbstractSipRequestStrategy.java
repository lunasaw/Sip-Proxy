package io.github.lunasaw.sip.common.transmit.strategy;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipMessageTransmitter;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.transmit.request.SipRequestBuilderFactory;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.extern.slf4j.Slf4j;

import javax.sip.message.Request;

/**
 * 抽象的基础SIP请求策略类
 * 提供通用的请求发送逻辑，子类只需要实现具体的请求构建逻辑
 *
 * @author lin
 */
@Slf4j
public abstract class AbstractSipRequestStrategy implements SipRequestStrategy {

    @Override
    public String sendRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId, Event errorEvent, Event okEvent) {
        Request request = buildRequest(fromDevice, toDevice, content, callId);
        SipMessageTransmitter.transmitMessage(fromDevice.getIp(), request, errorEvent, okEvent);
        return callId;
    }

    @Override
    public String sendRequestWithSubject(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId, Event errorEvent,
        Event okEvent) {
        Request request = buildRequestWithSubject(fromDevice, toDevice, content, subject, callId);
        SipMessageTransmitter.transmitMessage(fromDevice.getIp(), request, errorEvent, okEvent);
        return callId;
    }

    /**
     * 构建基础请求
     *
     * @param fromDevice 发送方设备
     * @param toDevice 接收方设备
     * @param content 请求内容
     * @param callId 呼叫ID
     * @return 构建的请求
     */
    protected abstract Request buildRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId);

    /**
     * 构建带主题的请求
     *
     * @param fromDevice 发送方设备
     * @param toDevice 接收方设备
     * @param content 请求内容
     * @param subject 主题
     * @param callId 呼叫ID
     * @return 构建的请求
     */
    protected Request buildRequestWithSubject(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId) {
        return buildRequest(fromDevice, toDevice, content, callId);
    }
}