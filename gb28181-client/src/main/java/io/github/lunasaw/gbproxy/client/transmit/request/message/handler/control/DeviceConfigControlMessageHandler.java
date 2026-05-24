package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import io.github.lunasaw.gb28181.common.entity.control.SnapShotConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientConfigEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageClientHandlerAbstract;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * GB28181-2022 §9.5 / A.2.3.2 设备配置控制（cmdType=DeviceConfig，rootType=Control）。
 *
 * <p>当前覆盖：
 * <ul>
 *   <li>SnapShotConfig (A.2.3.2.12)</li>
 *   <li>OSDConfig (A.2.3.2.11)</li>
 *   <li>VideoAlarmRecord (A.2.3.2.7)</li>
 *   <li>AlarmReport (A.2.3.2.10)</li>
 * </ul>
 *
 * <p>v1.5.0 改造：所有子分支都发布统一的 {@link ClientConfigEvent}，由 Adapter 用
 * Class&lt;?&gt; → Consumer 映射表分发到 {@code ConfigListener}。
 *
 * <p>新增子配置类型时：
 * <ol>
 *   <li>在 {@code gb28181-common/entity/control/cfg/} 下增加对应 JAXB ���（@XmlRootElement(name="Control")）</li>
 *   <li>在本 handler 增加 contains 分支，发布 ClientConfigEvent（payload 为新 config 类）</li>
 *   <li>在 {@code ConfigListener} 增加对应方法</li>
 *   <li>在 {@code ClientListenerAdapter.CONFIG_DISPATCH} 映射表注册新 Class → Consumer</li>
 * </ol>
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class DeviceConfigControlMessageHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.DEVICE_CONFIG.getType();

    private String cmdType = CMD_TYPE;

    private final ApplicationEventPublisher publisher;

    @Override
    public String getRootType() {
        return CONTROL;
    }

    @Override
    public void handForEvt(RequestEvent event) {
        try {
            String xmlStr = getXmlStr();
            DeviceSession deviceSession = getDeviceSession(event);

            DeviceControlBase cfg = parseConfig(xmlStr);
            if (cfg == null) {
                log.debug("DeviceConfig 命令未识别子标签，xml = {}", xmlStr);
                return;
            }
            publisher.publishEvent(new ClientConfigEvent(this, deviceSession.getUserId(), cfg));
        } catch (Exception e) {
            log.error("处理 DeviceConfig 控制命令时发生异常: event = {}", event, e);
        }
    }

    private DeviceControlBase parseConfig(String xmlStr) {
        if (xmlStr.contains("<SnapShotConfig>")) {
            return (SnapShotConfig) XmlUtils.parseObj(xmlStr, SnapShotConfig.class);
        }
        if (xmlStr.contains("<OSDConfig>")) {
            return (OsdConfig) XmlUtils.parseObj(xmlStr, OsdConfig.class);
        }
        if (xmlStr.contains("<VideoAlarmRecord>")) {
            return (VideoAlarmRecordConfig) XmlUtils.parseObj(xmlStr, VideoAlarmRecordConfig.class);
        }
        if (xmlStr.contains("<AlarmReport>")) {
            return (AlarmReportConfig) XmlUtils.parseObj(xmlStr, AlarmReportConfig.class);
        }
        return null;
    }

    @Override
    public String getCmdType() {
        return cmdType;
    }
}
