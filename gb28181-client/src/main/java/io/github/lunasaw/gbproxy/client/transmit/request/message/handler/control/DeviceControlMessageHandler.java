package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control;

import javax.sip.RequestEvent;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientControlEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gb28181.common.entity.control.*;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 设备控制消息处理器（cmdType=DeviceControl，rootType=Control）。
 *
 * <p>v1.5.0 改造：保留 XML 子标签 → Java Class 的映射在 handler 内（懂 XML 结构），
 * typed payload 通过 {@link ClientControlEvent} 交给 {@code ClientListenerAdapter} 用 instanceof
 * 路由到 {@code ControlListener} 方法（懂 Java 类型）。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class DeviceControlMessageHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "DeviceControl";
    private String cmdType = CMD_TYPE;

    private final ApplicationEventPublisher publisher;

    @Override
    public String getRootType() {
        return "Control";
    }

    private static class HandlerEntry<T extends DeviceControlBase> {
        final String xmlTag;
        final Class<T> clazz;

        HandlerEntry(String xmlTag, Class<T> clazz) {
            this.xmlTag = xmlTag;
            this.clazz = clazz;
        }
    }

    private static final List<HandlerEntry<? extends DeviceControlBase>> HANDLERS = List.of(
            new HandlerEntry<>("PTZCmd", DeviceControlPtz.class),
            new HandlerEntry<>("TeleBoot", DeviceControlTeleBoot.class),
            new HandlerEntry<>("RecordCmd", DeviceControlRecordCmd.class),
            new HandlerEntry<>("GuardCmd", DeviceControlGuard.class),
            new HandlerEntry<>("AlarmCmd", DeviceControlAlarm.class),
            new HandlerEntry<>("IFameCmd", DeviceControlIFame.class),
            new HandlerEntry<>("DragZoomIn", DeviceControlDragIn.class),
            new HandlerEntry<>("DragZoomOut", DeviceControlDragOut.class),
            new HandlerEntry<>("HomePosition", DeviceControlPosition.class),
            new HandlerEntry<>("DeviceUpgrade", DeviceUpgradeControl.class),
            new HandlerEntry<>("PTZPreciseCtrl", DeviceControlPTZPrecise.class),
            new HandlerEntry<>("FormatSDCard", DeviceControlSDCardFormat.class),
            new HandlerEntry<>("TargetTrack", DeviceControlTargetTrack.class)
    );

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            String xmlStr = getXmlStr();
            DeviceSession session = getDeviceSession(event);
            for (HandlerEntry<? extends DeviceControlBase> entry : HANDLERS) {
                if (xmlStr.contains("<" + entry.xmlTag + ">")) {
                    DeviceControlBase cmd = (DeviceControlBase) XmlUtils.parseObj(xmlStr, entry.clazz);
                    publisher.publishEvent(new ClientControlEvent(this, session.getUserId(), cmd));
                    return;
                }
            }
            log.warn("未识别的DeviceControl命令: {}", xmlStr);
        } catch (Exception e) {
            log.error("处理设备控制请求时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
