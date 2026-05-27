package io.github.lunasaw.sip.common.transmit.event;

/**
 * SIP事件回调接口，用于异步接收SIP请求的成功或失败结果。
 */
public interface Event {
    /**
     * 回调
     *
     * @param eventResult
     */
    void response(EventResult eventResult);
}