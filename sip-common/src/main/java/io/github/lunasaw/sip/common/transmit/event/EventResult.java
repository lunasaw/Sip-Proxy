package io.github.lunasaw.sip.common.transmit.event;

import javax.sip.*;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.transmit.event.result.DeviceNotFoundEvent;
import lombok.Data;

/**
 * SIP事件结果，封装响应、超时、事务终止、会话终止等各类事件的统一结果。
 *
 * @param <T> 原始事件类型
 */
@Data
public class EventResult<T> {
    /** HTTP状态码，超时/终止类事件为 -1024。 */
    public int             statusCode;
    /** 事件结果类型。 */
    public EventResultType type;
    /** 结果描述信息。 */
    public String          msg;
    /** 关联的 Call-ID。 */
    public String          callId;
    /** 关联的 SIP Dialog。 */
    public Dialog dialog;
    /** 原始事件对象。 */
    public T               event;

    public EventResult() {}

    /**
     * 根据原始事件构造结果，自动识别事件类型并提取 callId、状态码等信息。
     *
     * @param event 原始事件对象
     */
    public EventResult(T event) {
        this.event = event;
        if (event instanceof ResponseEvent) {
            ResponseEvent responseEvent = (ResponseEvent)event;
            Response response = responseEvent.getResponse();
            this.type = EventResultType.response;
            if (response != null) {
                this.msg = response.getReasonPhrase();
                this.statusCode = response.getStatusCode();
            }
            assert response != null;
            this.callId = ((CallIdHeader)response.getHeader(CallIdHeader.NAME)).getCallId();
        } else if (event instanceof TimeoutEvent) {
            TimeoutEvent timeoutEvent = (TimeoutEvent)event;
            this.type = EventResultType.timeout;
            this.msg = "消息超时未回复";
            this.statusCode = -1024;
            if (timeoutEvent.isServerTransaction()) {
                this.callId = ((SIPRequest)timeoutEvent.getServerTransaction().getRequest()).getCallIdHeader().getCallId();
                this.dialog = timeoutEvent.getServerTransaction().getDialog();
            } else {
                this.callId = ((SIPRequest)timeoutEvent.getClientTransaction().getRequest()).getCallIdHeader().getCallId();
                this.dialog = timeoutEvent.getClientTransaction().getDialog();
            }
        } else if (event instanceof TransactionTerminatedEvent) {
            TransactionTerminatedEvent transactionTerminatedEvent = (TransactionTerminatedEvent)event;
            this.type = EventResultType.transactionTerminated;
            this.msg = "事务已结束";
            this.statusCode = -1024;
            if (transactionTerminatedEvent.isServerTransaction()) {
                this.callId = ((SIPRequest)transactionTerminatedEvent.getServerTransaction().getRequest()).getCallIdHeader().getCallId();
                this.dialog = transactionTerminatedEvent.getServerTransaction().getDialog();
            } else {
                this.callId = ((SIPRequest) transactionTerminatedEvent.getClientTransaction().getRequest()).getCallIdHeader().getCallId();
                this.dialog = transactionTerminatedEvent.getClientTransaction().getDialog();
                this.dialog = transactionTerminatedEvent.getClientTransaction().getDialog();
            }
        } else if (event instanceof DialogTerminatedEvent) {
            DialogTerminatedEvent dialogTerminatedEvent = (DialogTerminatedEvent)event;
            this.type = EventResultType.dialogTerminated;
            this.msg = "会话已结束";
            this.statusCode = -1024;
            this.callId = dialogTerminatedEvent.getDialog().getCallId().getCallId();
            this.dialog = dialogTerminatedEvent.getDialog();

        } else if (event instanceof RequestEvent) {
            RequestEvent requestEvent = (RequestEvent) event;
            this.type = EventResultType.ack;
            this.msg = "ack event";
            this.callId = requestEvent.getDialog().getCallId().getCallId();
            this.dialog = requestEvent.getDialog();
        } else if (event instanceof DeviceNotFoundEvent) {
            this.type = EventResultType.deviceNotFoundEvent;
            this.msg = "设备未找到";
            this.statusCode = -1024;
            this.callId = ((DeviceNotFoundEvent)event).getCallId();
        }
    }
}