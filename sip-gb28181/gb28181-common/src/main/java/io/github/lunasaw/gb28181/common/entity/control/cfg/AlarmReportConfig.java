package io.github.lunasaw.gb28181.common.entity.control.cfg;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 A.2.3.2.10 / A.2.1.18 报警上报开关配置命令
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlarmReportConfig extends DeviceControlBase {

    @XmlElement(name = "AlarmReport")
    private AlarmReportInfo alarmReport;

    public AlarmReportConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("AlarmReport");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "AlarmReport")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AlarmReportInfo {

        /**
         * 移动侦测事件上报开关：0-关闭，1-打开
         */
        @XmlElement(name = "MotionDetection")
        private Integer motionDetection;

        /**
         * 区域入侵事件上报开关：0-关闭，1-打开
         */
        @XmlElement(name = "FieldDetection")
        private Integer fieldDetection;
    }
}
