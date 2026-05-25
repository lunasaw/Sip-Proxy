package io.github.lunasaw.gb28181.common.entity.control;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 A.2.3.1.14 目标跟踪控制命令
 *
 * <pre>
 * &lt;Control&gt;
 *   &lt;CmdType&gt;DeviceControl&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;TargetTrack&gt;Manual&lt;/TargetTrack&gt;
 *   &lt;DeviceID2&gt;34020000001320000002&lt;/DeviceID2&gt;
 *   &lt;TargetArea&gt;
 *     &lt;Length&gt;1920&lt;/Length&gt;
 *     &lt;Width&gt;1080&lt;/Width&gt;
 *     &lt;MidPointX&gt;960&lt;/MidPointX&gt;
 *     &lt;MidPointY&gt;540&lt;/MidPointY&gt;
 *     &lt;LengthX&gt;200&lt;/LengthX&gt;
 *     &lt;LengthY&gt;120&lt;/LengthY&gt;
 *   &lt;/TargetArea&gt;
 * &lt;/Control&gt;
 * </pre>
 *
 * TargetTrack 取值：Auto-自动跟踪、Manual-手动跟踪、Stop-停止跟踪
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceControlTargetTrack extends DeviceControlBase {

    @XmlElement(name = "TargetTrack")
    private String targetTrack;

    /**
     * 目标设备编码（全景相机中的全景通道 ID）
     */
    @XmlElement(name = "DeviceID2")
    private String deviceId2;

    @XmlElement(name = "TargetArea")
    private TargetArea targetArea;

    public DeviceControlTargetTrack(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("TargetTrack");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "TargetArea")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TargetArea {

        @XmlElement(name = "Length")
        private Integer length;

        @XmlElement(name = "Width")
        private Integer width;

        @XmlElement(name = "MidPointX")
        private Integer midPointX;

        @XmlElement(name = "MidPointY")
        private Integer midPointY;

        @XmlElement(name = "LengthX")
        private Integer lengthX;

        @XmlElement(name = "LengthY")
        private Integer lengthY;
    }
}
