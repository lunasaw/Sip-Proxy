package io.github.lunasaw.gbproxy.test.utils;

import javax.sip.header.CallIdHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import java.text.ParseException;
import java.util.UUID;

/**
 * 测试专用的SIP请求工具类
 * 提供不依赖SipLayer的工具方法
 *
 * @author luna
 * @date 2025/01/23
 */
public class TestSipRequestUtils {

    private static final MessageFactory MESSAGE_FACTORY;
    private static final HeaderFactory  HEADER_FACTORY;

    static {
        try {
            MESSAGE_FACTORY = javax.sip.SipFactory.getInstance().createMessageFactory();
            HEADER_FACTORY = javax.sip.SipFactory.getInstance().createHeaderFactory();
        } catch (javax.sip.PeerUnavailableException e) {
            throw new RuntimeException("初始化SIP工厂失败", e);
        }
    }

    /**
     * 生成新的CallId
     * 使用UUID生成，不依赖SipLayer
     *
     * @return CallId字符串
     */
    public static String getNewCallId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 创建CallIdHeader
     * 使用UUID生成，不依赖SipLayer
     *
     * @return CallIdHeader
     */
    public static CallIdHeader getNewCallIdHeader() {
        try {
            return HEADER_FACTORY.createCallIdHeader(getNewCallId());
        } catch (ParseException e) {
            throw new RuntimeException("创建CallIdHeader失败", e);
        }
    }

    /**
     * 根据指定CallId创建CallIdHeader
     *
     * @param callId CallId字符串
     * @return CallIdHeader
     */
    public static CallIdHeader createCallIdHeader(String callId) {
        try {
            if (callId == null) {
                return getNewCallIdHeader();
            }
            return HEADER_FACTORY.createCallIdHeader(callId);
        } catch (ParseException e) {
            throw new RuntimeException("创建CallIdHeader失败", e);
        }
    }
}