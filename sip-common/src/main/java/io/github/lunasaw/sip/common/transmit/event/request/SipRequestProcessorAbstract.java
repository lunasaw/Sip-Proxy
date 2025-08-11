package io.github.lunasaw.sip.common.transmit.event.request;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Maps;
import com.luna.common.text.StringTools;
import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.constant.Constant;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandler;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import io.github.lunasaw.sip.common.context.SipTransactionContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.message.Response;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author luna
 */
@Getter
@Setter
@Slf4j
public abstract class SipRequestProcessorAbstract implements SipRequestProcessor {


    public static final Map<String, Map<String, MessageHandler>> MESSAGE_HANDLER_CMD_MAP = new ConcurrentHashMap<>();

    public static void addHandler(MessageHandler messageHandler) {
        if (messageHandler == null) {
            return;
        }
        String key = messageHandler.getMethod() + "_" + messageHandler.getCmdType();
        // 打印添加的处理器
        log.info("添加消息处理器: key = {}, rootType = {}, cmdType = {}, method = {}", key, messageHandler.getRootType(), messageHandler.getCmdType(), messageHandler.getMethod());
        if (MESSAGE_HANDLER_CMD_MAP.containsKey(messageHandler.getRootType())) {
            MESSAGE_HANDLER_CMD_MAP.get(messageHandler.getRootType()).put(key, messageHandler);
        } else {
            ConcurrentMap<String, MessageHandler> newedConcurrentMap = Maps.newConcurrentMap();
            newedConcurrentMap.put(key, messageHandler);
            MESSAGE_HANDLER_CMD_MAP.put(messageHandler.getRootType(), newedConcurrentMap);
        }
    }

    public void doMessageHandForEvt(RequestEvent evt, FromDevice fromDevice) {
        doMessageHandForEvt(evt, fromDevice, null);
    }

    public void doMessageHandForEvt(RequestEvent evt, FromDevice fromDevice, ServerTransaction serverTransaction) {
        SIPRequest request = (SIPRequest) evt.getRequest();

        processMessageRequest(evt, fromDevice, serverTransaction, request);
    }

    /**
     * 处理SIP MESSAGE请求的核心逻辑
     */
    private void processMessageRequest(RequestEvent evt, FromDevice fromDevice, ServerTransaction serverTransaction, SIPRequest request) {
        String charset = Optional.of(fromDevice).map(Device::getCharset).orElse(Constant.UTF_8);

        // 解析xml
        byte[] rawContent = request.getRawContent();
        String xmlStr = StringTools.toEncodedString(rawContent, Charset.forName(charset));

        log.info("开始处理消息 doMessageHandForEvt::fromDevice = {}, xmlStr = {}", JSON.toJSONString(fromDevice), xmlStr);
        String cmdType = XmlUtils.getCmdType(xmlStr);
        String rootType = XmlUtils.getRootType(xmlStr);
        String method = request.getMethod();

        if (cmdType == null) {
            log.warn("XML消息中缺少CmdType元素，跳过处理: {}", xmlStr);
            return;
        }
        
        Map<String, MessageHandler> messageHandlerMap = MESSAGE_HANDLER_CMD_MAP.get(rootType);

        if (MapUtils.isEmpty(messageHandlerMap)) {
            // 没有对应的消息处理器
            log.warn("doMessageHandForEvt::未找到对应的消息处理器, method = {}, rootType = {}, cmdType = {}", method, rootType, cmdType);
            return;
        }

        MessageHandler messageHandler = messageHandlerMap.get(method + "_" + cmdType);
        if (messageHandler == null) {
            // 没有对应的消息处理器
            log.warn("doMessageHandForEvt::未找到对应的消息处理器, method = {}, rootType = {}, cmdType = {}", method, rootType, cmdType);
            return;
        }
        try {
            messageHandler.setXmlStr(xmlStr);
            messageHandler.handForEvt(evt);
            if (messageHandler.needResponseAck()) {
                // 如果有预创建的事务，使用它；否则使用原有逻辑
                messageHandler.responseAck(evt, serverTransaction);
            }
        } catch (Exception e) {
            log.error("process::evt = {}, e", evt, e);
            messageHandler.responseError(evt, Response.SERVER_INTERNAL_ERROR, e.getMessage(), serverTransaction);
        }
    }
}
