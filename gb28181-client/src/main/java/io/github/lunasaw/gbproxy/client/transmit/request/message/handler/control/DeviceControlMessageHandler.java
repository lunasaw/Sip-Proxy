package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control;

import javax.sip.RequestEvent;

import io.github.lunasaw.gbproxy.client.transmit.request.message.ClientMessageRequestProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageProcessorClient;
import io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control.emums.DeviceControlType;
import io.github.lunasaw.sip.common.utils.SpringBeanFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备控制消息处理器
 * 负责处理设备控制请求
 *
 * @author luna
 * @date 2023/10/19
 */
@Component
@Slf4j
@Getter
@Setter
public class DeviceControlMessageHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "DeviceControl";

    private String cmdType = CMD_TYPE;

    @Autowired
    private SpringBeanFactory springBeanFactory;

    public DeviceControlMessageHandler(MessageProcessorClient messageProcessorClient) {
        super(messageProcessorClient);
    }

    @Override
    public String getRootType() {
        return ClientMessageRequestProcessor.METHOD + "Control";
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            String xmlStr = getXmlStr();
            DeviceControlType deviceControlType = DeviceControlType.getDeviceControlTypeFilter(xmlStr);
            if (deviceControlType == null) {
                log.warn("未找到设备控制类型: xmlStr = {}", xmlStr);
                return;
            }

            // 解析控制命令
            Object controlCommand = parseXml(deviceControlType.getClazz());

            // 获取控制命令处理器
            DeviceControlCmd bean = SpringBeanFactory.getBean(deviceControlType.getBeanName());
            if (bean == null) {
                log.warn("未找到设备控制命令处理器: beanName = {}", deviceControlType.getBeanName());
                return;
            }

            // 执行控制命令
            bean.doCmd(controlCommand);

        } catch (Exception e) {
            log.error("处理设备控制请求时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
