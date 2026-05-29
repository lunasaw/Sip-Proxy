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
 * GB28181-2022 A.2.3.1.11 PTZ 精准控制命令 (cmdType=DeviceControl，PTZPreciseCtrlType)
 *
 * <pre>
 * &lt;Control&gt;
 *   &lt;CmdType&gt;DeviceControl&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;PTZPreciseCtrl&gt;
 *     &lt;Pan&gt;180.0&lt;/Pan&gt;
 *     &lt;Tilt&gt;30.0&lt;/Tilt&gt;
 *     &lt;Zoom&gt;2.0&lt;/Zoom&gt;
 *   &lt;/PTZPreciseCtrl&gt;
 * &lt;/Control&gt;
 * </pre>
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceControlPTZPrecise extends DeviceControlBase {

    @XmlElement(name = "PTZPreciseCtrl")
    private PTZPreciseCtrl ptzPreciseCtrl;

    public DeviceControlPTZPrecise(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("PTZPreciseCtrl");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "PTZPreciseCtrl")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PTZPreciseCtrl {

        /**
         * 云台水平角度，0～360.00
         */
        @XmlElement(name = "Pan")
        private Double pan;

        /**
         * 云台垂直角度，一般取值 -30.00～90.00
         */
        @XmlElement(name = "Tilt")
        private Double tilt;

        /**
         * 变焦倍数，一般大于 1.00
         */
        @XmlElement(name = "Zoom")
        private Double zoom;
    }
}
