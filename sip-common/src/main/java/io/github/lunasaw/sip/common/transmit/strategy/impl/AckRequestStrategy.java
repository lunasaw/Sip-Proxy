package io.github.lunasaw.sip.common.transmit.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.request.SipRequestBuilderFactory;
import io.github.lunasaw.sip.common.transmit.strategy.AbstractSipRequestStrategy;
import lombok.extern.slf4j.Slf4j;

import javax.sip.message.Request;

/**
 * ACK请求策略实现
 *
 * @author lin
 */
@Slf4j
public class AckRequestStrategy extends AbstractSipRequestStrategy {

    @Override
    protected Request buildRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return SipRequestBuilderFactory.createAckRequest(fromDevice, toDevice, content, callId);
    }
}