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
 * GB28181-2022 A.2.3.1.13 存储卡格式化控制命令
 *
 * <pre>
 * &lt;Control&gt;
 *   &lt;CmdType&gt;DeviceControl&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;FormatSDCard&gt;1&lt;/FormatSDCard&gt;
 * &lt;/Control&gt;
 * </pre>
 *
 * SD 卡编号从 1 开始，0 表示对所有存储卡进行格式化。
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceControlSDCardFormat extends DeviceControlBase {

    @XmlElement(name = "FormatSDCard")
    private Integer formatSDCard;

    public DeviceControlSDCardFormat(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("FormatSDCard");
    }
}
