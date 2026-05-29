package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 A.2.6.15 PTZ 精确状态查询应答 (cmdType=PTZPosition)
 *
 * <pre>
 * &lt;Response&gt;
 *   &lt;CmdType&gt;PTZPosition&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;Pan&gt;180.0&lt;/Pan&gt;
 *   &lt;Tilt&gt;30.0&lt;/Tilt&gt;
 *   &lt;Zoom&gt;2.0&lt;/Zoom&gt;
 *   &lt;HorizontalFieldAngle&gt;60.0&lt;/HorizontalFieldAngle&gt;
 *   &lt;VerticalFieldAngle&gt;40.0&lt;/VerticalFieldAngle&gt;
 *   &lt;MaxViewDistance&gt;500.0&lt;/MaxViewDistance&gt;
 * &lt;/Response&gt;
 * </pre>
 *
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class PTZPositionResponse extends XmlBean {

    @XmlElement(name = "CmdType")
    private String cmdType = CmdTypeEnum.PTZ_POSITION.getType();

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "Pan")
    private Double pan;

    @XmlElement(name = "Tilt")
    private Double tilt;

    @XmlElement(name = "Zoom")
    private Double zoom;

    @XmlElement(name = "HorizontalFieldAngle")
    private Double horizontalFieldAngle;

    @XmlElement(name = "VerticalFieldAngle")
    private Double verticalFieldAngle;

    @XmlElement(name = "MaxViewDistance")
    private Double maxViewDistance;

    public PTZPositionResponse(String sn, String deviceId) {
        this.sn = sn;
        this.deviceId = deviceId;
    }
}
