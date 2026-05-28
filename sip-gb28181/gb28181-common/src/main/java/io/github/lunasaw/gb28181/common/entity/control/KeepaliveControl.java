package io.github.lunasaw.gb28181.common.entity.control;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 Control类型的Keepalive命令
 * <pre>
 * <Control>
 *   <CmdType>Keepalive</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <Status>OK</Status>
 * </Control>
 * </pre>
 *
 * @author claude
 * @date 2025/01/19
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class KeepaliveControl extends ControlBase {

    @XmlElement(name = "Status")
    private String status;

    public KeepaliveControl(String sn, String deviceId, String status) {
        super("Keepalive", sn, deviceId);
        this.status = status;
    }
}