package io.github.lunasaw.sip.common.transmit.event.message;

import io.github.lunasaw.sip.common.transmit.event.handler.RequestHandler;

import javax.sip.RequestEvent;

/**
 * 对message类型的请求单独抽象，根据cmdType进行处理
 */
public interface MessageHandler extends RequestHandler {


    String QUERY = "Query";
    String CONTROL = "Control";
    String NOTIFY = "Notify";
    String RESPONSE = "Response";

    /**
     * 响应ack
     * 
     * @param event 请求事件
     */
    void responseAck(RequestEvent event);

    /**
     * 响应error
     *
     * @param event 请求事件
     */
    void responseError(RequestEvent event);

    /**
     * 自定义错误回复
     * 
     * @param event
     * @param code
     * @param error
     */
    void responseError(RequestEvent event, Integer code, String error);

    /**
     * 处理消息
     *
     * @param event
     */
    void handForEvt(RequestEvent event);

    /**
     * 处理标签
     *
     * @return
     */
    String getRootType();

    /**
     * 处理消息类型
     *
     * @return
     */
    String getCmdType();

    /**
     * 获取处理方法
     *
     * @return
     */
    String getMethod();


    /**
     * 当前接受到的原始消息
     */
    void setXmlStr(String xmlStr);

    /**
     * 是否需要响应ack
     *
     * @return
     */
    default boolean needResponseAck() {
        return true;
    };
}
