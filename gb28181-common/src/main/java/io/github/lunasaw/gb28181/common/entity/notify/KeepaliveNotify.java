package io.github.lunasaw.gb28181.common.entity.notify;

import jakarta.xml.bind.annotation.*;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GB28181协议 A.2.5 a）状态信息报送
 * <pre>
 * <Notify>
 *   <CmdType>Keepalive</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <Status>OK</Status>
 *   <Info>
 *     <DeviceID>34020000001320000002</DeviceID>
 *     <DeviceID>34020000001320000003</DeviceID>
 *   </Info>
 * </Notify>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Notify")
@XmlAccessorType(XmlAccessType.FIELD)
public class KeepaliveNotify extends XmlBean {
    @XmlElement(name = "CmdType")
    private final String cmdType = "Keepalive";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "Status")
    private String status;

    @XmlElementWrapper(name = "Info")
    @XmlElement(name = "DeviceID")
    private List<String> infoDeviceIds;
}