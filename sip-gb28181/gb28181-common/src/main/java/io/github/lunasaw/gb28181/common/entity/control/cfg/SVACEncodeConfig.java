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
 * GB28181-2022 §A.2.3.2.3 SVAC 编码配置（cmdType=DeviceConfig，子标签 SVACEncodeConfig）。
 *
 * <p>SVAC（Surveillance Video and Audio Coding，安全视频音频编码）是国标定义的视频编码配置。
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class SVACEncodeConfig extends DeviceControlBase {

    @XmlElement(name = "SVACEncodeConfig")
    private SVACEncodeInfo svacEncodeConfig;

    public SVACEncodeConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("SVACEncodeConfig");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SVACEncodeInfo {

        /** ROI（感兴趣区域）编码使能：0=关，1=开 */
        @XmlElement(name = "ROIEnable")
        private Integer roiEnable;

        /** SVC（可伸缩视频编码）使能：0=关，1=开 */
        @XmlElement(name = "SVCEnable")
        private Integer svcEnable;

        /** 视频编码空间分级：1=两层，2=三层 */
        @XmlElement(name = "SVCSpaceSupportMode")
        private Integer svcSpaceSupportMode;

        /** 视频编码时间分级：1=两层，2=三层，3=四层 */
        @XmlElement(name = "SVCTimeSupportMode")
        private Integer svcTimeSupportMode;

        /** 监控专用信息开关：0=关，1=开 */
        @XmlElement(name = "SurveillanceOnOff")
        private Integer surveillanceOnOff;

        /** 视频加密开关：0=关，1=开 */
        @XmlElement(name = "VideoEncryptOnOff")
        private Integer videoEncryptOnOff;
    }
}
