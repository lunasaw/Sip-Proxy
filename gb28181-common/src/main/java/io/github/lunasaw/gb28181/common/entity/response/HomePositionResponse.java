package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 A.2.6.12 看守位信息查询应答 (cmdType=HomePositionQuery)
 *
 * <pre>
 * &lt;Response&gt;
 *   &lt;CmdType&gt;HomePositionQuery&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;HomePosition&gt;
 *     &lt;Enabled&gt;1&lt;/Enabled&gt;
 *     &lt;ResetTime&gt;60&lt;/ResetTime&gt;
 *     &lt;PresetIndex&gt;1&lt;/PresetIndex&gt;
 *   &lt;/HomePosition&gt;
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
public class HomePositionResponse extends XmlBean {

    @XmlElement(name = "CmdType")
    private String cmdType = CmdTypeEnum.HOME_POSITION_QUERY.getType();

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "HomePosition")
    private HomePositionInfo homePosition;

    public HomePositionResponse(String sn, String deviceId) {
        this.sn = sn;
        this.deviceId = deviceId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "HomePosition")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HomePositionInfo {

        /**
         * 看守位开关：0-关闭，1-开启
         */
        @XmlElement(name = "Enabled")
        private Integer enabled;

        /**
         * 自动归位时间间隔（秒），开启看守位时使用
         */
        @XmlElement(name = "ResetTime")
        private Integer resetTime;

        /**
         * 调用预置位编号 (0-255)，开启看守位时使用
         */
        @XmlElement(name = "PresetIndex")
        private Integer presetIndex;
    }
}
