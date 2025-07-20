package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control;

import javax.sip.RequestEvent;

import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.gb28181.common.entity.control.*;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiConsumer;

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
    private DeviceControlRequestHandler deviceControlRequestHandler;

    public DeviceControlMessageHandler(MessageRequestHandler messageRequestHandler) {
        super(messageRequestHandler);
    }

    @Override
    public String getRootType() {
        return "Control";
    }

    private static class HandlerEntry<T> {
        final String xmlTag;
        final Class<T> clazz;
        final BiConsumer<DeviceControlRequestHandler, T> handler;

        HandlerEntry(String xmlTag, Class<T> clazz, BiConsumer<DeviceControlRequestHandler, T> handler) {
            this.xmlTag = xmlTag;
            this.clazz = clazz;
            this.handler = handler;
        }
    }

    private static final List<HandlerEntry<?>> HANDLERS = List.of(
            new HandlerEntry<>("PTZCmd", DeviceControlPtz.class, (h, c) -> h.handlePtzCmd((DeviceControlPtz) c)),
            new HandlerEntry<>("TeleBoot", DeviceControlTeleBoot.class, (h, c) -> h.handleTeleBoot((DeviceControlTeleBoot) c)),
            new HandlerEntry<>("RecordCmd", DeviceControlRecordCmd.class, (h, c) -> h.handleRecordCmd((DeviceControlRecordCmd) c)),
            new HandlerEntry<>("GuardCmd", DeviceControlGuard.class, (h, c) -> h.handleGuardCmd((DeviceControlGuard) c)),
            new HandlerEntry<>("AlarmCmd", DeviceControlAlarm.class, (h, c) -> h.handleAlarmCmd((DeviceControlAlarm) c)),
            new HandlerEntry<>("IFameCmd", DeviceControlIFame.class, (h, c) -> h.handleIFameCmd((DeviceControlIFame) c)),
            new HandlerEntry<>("DragZoomIn", DeviceControlDragIn.class, (h, c) -> h.handleDragZoomIn((DeviceControlDragIn) c)),
            new HandlerEntry<>("DragZoomOut", DeviceControlDragOut.class, (h, c) -> h.handleDragZoomOut((DeviceControlDragOut) c)),
            new HandlerEntry<>("HomePosition", DeviceControlPosition.class, (h, c) -> h.handleHomePosition((DeviceControlPosition) c))
    );

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            String xmlStr = getXmlStr();
            boolean matched = false;
            for (HandlerEntry<?> entry : HANDLERS) {
                if (xmlStr.contains("<" + entry.xmlTag + ">")) {
                    Object cmd = XmlUtils.parseObj(xmlStr, entry.clazz);
                    //noinspection unchecked
                    ((BiConsumer<DeviceControlRequestHandler, Object>) entry.handler).accept(deviceControlRequestHandler, cmd);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                log.warn("未识别的DeviceControl命令: {}", xmlStr);
            }
        } catch (Exception e) {
            log.error("处理设备控制请求时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
