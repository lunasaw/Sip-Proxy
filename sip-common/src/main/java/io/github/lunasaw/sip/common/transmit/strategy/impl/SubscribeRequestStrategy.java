package io.github.lunasaw.sip.common.transmit.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
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
 * SUBSCRIBE 请求策略实现。
 *
 * <p>1.7.0：与 INVITE 同结构，{@code sendRequestWithSubscribe} 走 stateful 通道并把 dialog 注册到
 * {@link DialogRegistry}（kind = SUBSCRIBE，expiresAtMs = now + (expires + grace) * 1000），
 * 后续续订 / 退订通过 {@code SipSender.doSubscribeRefresh(callId, content, expires)} 复用同一 dialog。
 *
 * <p>无 {@link SubscribeInfo} 的 SUBSCRIBE（极少出现）退化为 stateless，保留旧行为兼容性。
 *
 * @author lin
 */
@Slf4j
public class SubscribeRequestStrategy extends AbstractSipRequestStrategy {

    /** SUBSCRIBE 自然过期后给定的清理宽限期（秒），覆盖 NOTIFY: terminated 延迟到达 */
    private static final int CLEANUP_GRACE_SECONDS = 60;

    @Override
    protected Request buildRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return SipRequestBuilderFactory.createSubscribeRequest(fromDevice, toDevice, content, null, callId);
    }

    @Override
    protected Request buildRequestWithSubscribe(FromDevice fromDevice, ToDevice toDevice, String content,
                                                 SubscribeInfo subscribeInfo, String callId) {
        return SipRequestBuilderFactory.createSubscribeRequest(fromDevice, toDevice, content, subscribeInfo, callId);
    }

    @Override
    public String sendRequestWithSubscribe(FromDevice fromDevice, ToDevice toDevice, String content,
                                           SubscribeInfo subscribeInfo, String callId,
                                           Event errorEvent, Event okEvent) {
        if (StringUtils.isBlank(callId)) {
            callId = SipRequestUtils.getNewCallId();
        }
        Request request = buildRequestWithSubscribe(fromDevice, toDevice, content, subscribeInfo, callId);
        SipMessageTransmitter.setupEventSubscriptions(request, errorEvent, okEvent);

        // expires=0 是退订，仍走 stateful 注册一个短期 entry，等 NOTIFY: Subscription-State: terminated
        // 到来后由 processDialogTerminated 清理；expires>0 按 expires + grace 设过期时刻
        int expires = subscribeInfo != null ? subscribeInfo.getExpires() : 0;
        long expiresAtMs = expires > 0
                ? System.currentTimeMillis() + (expires + CLEANUP_GRACE_SECONDS) * 1000L
                : System.currentTimeMillis() + CLEANUP_GRACE_SECONDS * 1000L;

        final String finalCallId = callId;
        Dialog dialog = SipMessageTransmitter.transmitStatefulPreRegister(
                fromDevice.getIp(), request, callId,
                (cid, dlg) -> DialogRegistry.register(cid, dlg, expiresAtMs, DialogRegistry.KIND_SUBSCRIBE));
        if (dialog != null) {
            log.debug("SUBSCRIBE 已注册 dialog: callId={}, expires={}s, dialogId={}",
                    finalCallId, expires, dialog.getDialogId());
        } else {
            log.warn("SUBSCRIBE 发送后未获得 dialog: callId={}", finalCallId);
        }
        return callId;
    }
}
