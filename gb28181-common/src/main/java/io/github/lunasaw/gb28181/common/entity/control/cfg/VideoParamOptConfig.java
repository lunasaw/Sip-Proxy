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
 * GB28181-2022 §A.2.3.2.2 视频参数范围配置（cmdType=DeviceConfig，子标签 VideoParamOpt）。
 *
 * <p>用于平台向设备下发"视频参数可选范围"配置，告知设备可以使用哪些分辨率/码率组合。
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class VideoParamOptConfig extends DeviceControlBase {

    @XmlElement(name = "VideoParamOpt")
    private VideoParamOpt videoParamOpt;

    public VideoParamOptConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("VideoParamOpt");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VideoParamOpt {

        /** 视频帧率范围，例如 "10/15/25/30" */
        @XmlElement(name = "DownloadSpeed")
        private String downloadSpeed;

        /** 分辨率，CIF/QCIF/4CIF/D1/720P/1080P 之一 */
        @XmlElement(name = "Resolution")
        private String resolution;

        /** 码率范围（kbps），例如 "256/512/1024" */
        @XmlElement(name = "BitRate")
        private String bitRate;

        /** 视频帧率（每秒帧数） */
        @XmlElement(name = "FrameRate")
        private Integer frameRate;
    }
}
