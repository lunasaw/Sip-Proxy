package io.github.lunasaw.gb28181.common.entity.control.cfg;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 §A.2.3.2.4 SVAC 解码配置（cmdType=DeviceConfig，子标签 SVACDecodeConfig）。
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class SVACDecodeConfig extends DeviceControlBase {

    @XmlElement(name = "SVACDecodeConfig")
    private SVACDecodeInfo svacDecodeConfig;

    public SVACDecodeConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("SVACDecodeConfig");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SVACDecodeInfo {

        /** SVC 解码模式：0=空间分级解码，1=时间分级解码 */
        @XmlElement(name = "SVCSpaceSupportMode")
        private Integer svcSpaceSupportMode;

        @XmlElement(name = "SVCTimeSupportMode")
        private Integer svcTimeSupportMode;

        /** 监控专用信息识别能力：0=不支持，1=支持 */
        @XmlElement(name = "SurveillanceOnOff")
        private Integer surveillanceOnOff;

        /** 视频解密能力：0=不支持，1=支持 */
        @XmlElement(name = "VideoEncryptOnOff")
        private Integer videoEncryptOnOff;
    }
}
