package io.github.lunasaw.sip.common.transmit.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.DialogRegistry;
import io.github.lunasaw.sip.common.transmit.SipMessageTransmitter;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.transmit.request.SipRequestBuilderFactory;
import io.github.lunasaw.sip.common.transmit.strategy.AbstractSipRequestStrategy;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.sip.Dialog;
import javax.sip.message.Request;

/**
 * INVITE请求策略实现。
 *
 * <p>1.7.0：改走 {@link SipMessageTransmitter#transmitStatefulPreRegister} —— JAIN-SIP 自动创建 Dialog
 * 并由本类登记到 {@link DialogRegistry}，便于后续 BYE / re-INVITE / INFO / UPDATE 等 dialog 内消息复用
 * dialog 状态（to-tag / CSeq / Route Set / Remote Target）。
 *
 * @author lin
 */
@Slf4j
public class InviteRequestStrategy extends AbstractSipRequestStrategy {

    @Override
    protected Request buildRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return SipRequestBuilderFactory.createInviteRequest(fromDevice, toDevice, content, null, callId);
    }

    @Override
    protected Request buildRequestWithSubject(FromDevice fromDevice, ToDevice toDevice, String content, String subject, String callId) {
        return SipRequestBuilderFactory.createInviteRequest(fromDevice, toDevice, content, subject, callId);
    }

    @Override
    public String sendRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId,
                              Event errorEvent, Event okEvent) {
        if (StringUtils.isBlank(callId)) {
            callId = SipRequestUtils.getNewCallId();
        }
        Request request = buildRequest(fromDevice, toDevice, content, callId);
        return sendStateful(fromDevice, request, callId, errorEvent, okEvent);
    }

    @Override
    public String sendRequestWithSubject(FromDevice fromDevice, ToDevice toDevice, String content, String subject,
                                         String callId, Event errorEvent, Event okEvent) {
        if (StringUtils.isBlank(callId)) {
            callId = SipRequestUtils.getNewCallId();
        }
        Request request = buildRequestWithSubject(fromDevice, toDevice, content, subject, callId);
        return sendStateful(fromDevice, request, callId, errorEvent, okEvent);
    }

    private String sendStateful(FromDevice fromDevice, Request request, String callId,
                                Event errorEvent, Event okEvent) {
        SipMessageTransmitter.setupEventSubscriptions(request, errorEvent, okEvent);
        Dialog dialog = SipMessageTransmitter.transmitStatefulPreRegister(
                fromDevice.getIp(), request, callId,
                (cid, dlg) -> DialogRegistry.register(cid, dlg));
        if (dialog != null) {
            log.debug("INVITE 已注册 dialog: callId={}, dialogId={}", callId, dialog.getDialogId());
        } else {
            log.warn("INVITE 发送后未获得 dialog: callId={}", callId);
        }
        return callId;
    }
}
