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
 * GB28181-2022 §A.2.3.2.9 画面镜像配置（cmdType=DeviceConfig，子标签 FrameMirror）。
 *
 * <p>用于平台向设备下发"画面镜像 / 翻转"配置（横向 / 纵向 / 旋转）。
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class FrameMirrorConfig extends DeviceControlBase {

    @XmlElement(name = "FrameMirror")
    private FrameMirror frameMirror;

    public FrameMirrorConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("FrameMirror");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FrameMirror {

        /** 水平镜像：0=关闭，1=开启 */
        @XmlElement(name = "Horizontal")
        private Integer horizontal;

        /** 垂直镜像：0=关闭，1=开启 */
        @XmlElement(name = "Vertical")
        private Integer vertical;

        /** 旋转角度：0=不旋转，1=90°，2=180°，3=270° */
        @XmlElement(name = "Rotate")
        private Integer rotate;
    }
}
