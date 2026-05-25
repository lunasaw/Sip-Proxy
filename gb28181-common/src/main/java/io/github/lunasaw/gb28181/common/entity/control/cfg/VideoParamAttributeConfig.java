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
 * GB28181-2022 §A.2.3.2.5 视频参数属性配置（cmdType=DeviceConfig，子标签 VideoParamAttribute）。
 *
 * <p>用于平台向设备下发"视频参数属性"配置，包括分辨率/码率/帧率/视频流类型等。
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class VideoParamAttributeConfig extends DeviceControlBase {

    @XmlElement(name = "VideoParamAttribute")
    private VideoParamAttribute videoParamAttribute;

    public VideoParamAttributeConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("VideoParamAttribute");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VideoParamAttribute {

        /** 视频流类型：0=主码流，1=子码流 1，2=子码流 2，3=子码流 3 */
        @XmlElement(name = "StreamNumber")
        private Integer streamNumber;

        /** 码流编码格式，参见 §A.2.1.9 */
        @XmlElement(name = "VideoFormat")
        private String videoFormat;

        /** 分辨率 */
        @XmlElement(name = "Resolution")
        private String resolution;

        /** 帧率（fps） */
        @XmlElement(name = "FrameRate")
        private Integer frameRate;

        /** 码率类型：1=变码率（VBR），2=固定码率（CBR） */
        @XmlElement(name = "BitRateType")
        private Integer bitRateType;

        /** 视频码率（kbps） */
        @XmlElement(name = "VideoBitRate")
        private Integer videoBitRate;
    }
}
