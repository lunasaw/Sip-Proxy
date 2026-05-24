package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control;

import io.github.lunasaw.gb28181.common.entity.control.SnapShotConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientAlarmReportConfigEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientOsdConfigEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientSnapShotConfigEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientVideoAlarmRecordConfigEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * GB28181-2022 §9.5 / A.2.3.2 设备配置控制（cmdType=DeviceConfig，rootType=Control）。
 *
 * 当前覆盖：
 *   - SnapShotConfig (A.2.3.2.12) → {@link ClientSnapShotConfigEvent}
 *   - OSDConfig (A.2.3.2.11) → {@link ClientOsdConfigEvent}
 *   - VideoAlarmRecord (A.2.3.2.7) → {@link ClientVideoAlarmRecordConfigEvent}
 *   - AlarmReport (A.2.3.2.10) → {@link ClientAlarmReportConfigEvent}
 *
 * 其他子命令（VideoRecordPlan / PictureMask / FrameMirror / SVAC* / VideoParamAttribute）按相同模式扩展子分支即可：
 * 1. 在 {@code gb28181-common/entity/control/cfg/} 下增加对应 JAXB 类（@XmlRootElement(name="Control")）；
 * 2. 在本 handler 增加 contains 分支并发布对应 ClientXxxConfigEvent；
 * 3. 在 {@link io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender} 增加 {@code deviceConfigXxx(...)} 门面方法。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class DeviceConfigControlMessageHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.DEVICE_CONFIG.getType();

    private String cmdType = CMD_TYPE;

    private final ApplicationEventPublisher publisher;

    public DeviceConfigControlMessageHandler(MessageRequestHandler messageRequestHandler,
                                             ApplicationEventPublisher publisher) {
        super(messageRequestHandler);
        this.publisher = publisher;
    }

    @Override
    public String getRootType() {
        return CONTROL;
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            String xmlStr = getXmlStr();
            DeviceSession deviceSession = getDeviceSession(event);

            if (xmlStr.contains("<SnapShotConfig>")) {
                SnapShotConfig snapShot = (SnapShotConfig) XmlUtils.parseObj(xmlStr, SnapShotConfig.class);
                publisher.publishEvent(new ClientSnapShotConfigEvent(this, deviceSession.getUserId(), snapShot));
                return;
            }
            if (xmlStr.contains("<OSDConfig>")) {
                OsdConfig osd = (OsdConfig) XmlUtils.parseObj(xmlStr, OsdConfig.class);
                publisher.publishEvent(new ClientOsdConfigEvent(this, deviceSession.getUserId(), osd));
                return;
            }
            if (xmlStr.contains("<VideoAlarmRecord>")) {
                VideoAlarmRecordConfig cfg = (VideoAlarmRecordConfig) XmlUtils.parseObj(xmlStr, VideoAlarmRecordConfig.class);
                publisher.publishEvent(new ClientVideoAlarmRecordConfigEvent(this, deviceSession.getUserId(), cfg));
                return;
            }
            if (xmlStr.contains("<AlarmReport>")) {
                AlarmReportConfig cfg = (AlarmReportConfig) XmlUtils.parseObj(xmlStr, AlarmReportConfig.class);
                publisher.publishEvent(new ClientAlarmReportConfigEvent(this, deviceSession.getUserId(), cfg));
                return;
            }

            log.debug("DeviceConfig 命令未识别子标签，xml = {}", xmlStr);
        } catch (Exception e) {
            log.error("处理 DeviceConfig 控制命令时发生异常: event = {}", event, e);
        }
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}

