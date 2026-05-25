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
 * GB28181-2022 A.2.3.2.11 / A.2.1.12 OSD 配置命令 (cmdType=DeviceConfig，子标签 OSDConfig)
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class OsdConfig extends DeviceControlBase {

    @XmlElement(name = "OSDConfig")
    private OsdInfo osdConfig;

    public OsdConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("OSDConfig");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "OSDConfig")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OsdInfo {

        @XmlElement(name = "Length")
        private Integer length;

        @XmlElement(name = "Width")
        private Integer width;

        @XmlElement(name = "TimeX")
        private Integer timeX;

        @XmlElement(name = "TimeY")
        private Integer timeY;

        /**
         * 0-关闭，1-打开（默认值）
         */
        @XmlElement(name = "TimeEnable")
        private Integer timeEnable;

        /**
         * 0-YYYY-MM-DD HH:MM:SS, 1-YYYY 年 MM 月 DD 日 HH:MM:SS
         */
        @XmlElement(name = "TimeType")
        private Integer timeType;

        /**
         * 0-关闭，1-打开（默认值）
         */
        @XmlElement(name = "TextEnable")
        private Integer textEnable;
    }
}
