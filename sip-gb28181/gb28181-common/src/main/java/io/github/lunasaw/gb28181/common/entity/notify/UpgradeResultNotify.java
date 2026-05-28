package io.github.lunasaw.gb28181.common.entity.notify;

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
 * GB28181-2022 A.2.5.9 设备软件升级结果通知
 *
 * <pre>
 * &lt;Notify&gt;
 *   &lt;CmdType&gt;DeviceUpgradeResult&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;SessionID&gt;32-128 chars&lt;/SessionID&gt;
 *   &lt;UpgradeResult&gt;OK&lt;/UpgradeResult&gt;
 *   &lt;Firmware&gt;V1.0.1&lt;/Firmware&gt;
 *   &lt;UpgradeFailedReason&gt;01&lt;/UpgradeFailedReason&gt;
 * &lt;/Notify&gt;
 * </pre>
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Notify")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpgradeResultNotify extends XmlBean {

    @XmlElement(name = "CmdType")
    private String cmdType = CmdTypeEnum.DEVICE_UPGRADE_RESULT.getType();

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "SessionID")
    private String sessionId;

    @XmlElement(name = "UpgradeResult")
    private String upgradeResult;

    @XmlElement(name = "Firmware")
    private String firmware;

    @XmlElement(name = "UpgradeFailedReason")
    private String upgradeFailedReason;

    public UpgradeResultNotify(String sn, String deviceId) {
        this.sn = sn;
        this.deviceId = deviceId;
    }
}
