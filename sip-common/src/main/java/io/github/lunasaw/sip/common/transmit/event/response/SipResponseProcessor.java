package io.github.lunasaw.sip.common.transmit.event.response;

import javax.sip.ResponseEvent;

/**
 * 处理接收IPCamera发来的SIP协议响应消息
 * 
 * @author swwheihei
 */
public interface SipResponseProcessor {

	/**
	 * 是否需要处理
	 */
	default boolean isNeedProcess(ResponseEvent evt) {
		return true;
	}

	/**
	 * 获取SIP方法类型
	 *
	 * @return SIP方法类型
	 */
	String getMethod();

	/**
	 * 处理接收IPCamera发来的SIP协议响应消息
	 * @param evt 消息对象
	 */
	void process(ResponseEvent evt);


}
