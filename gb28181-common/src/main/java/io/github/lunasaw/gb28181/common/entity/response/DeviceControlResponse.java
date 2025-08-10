package io.github.lunasaw.gb28181.common.entity.response;

import jakarta.xml.bind.annotation.*;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 A.2.6 a）设备控制应答
 * <pre>
 * <Response>
 *   <CmdType>DeviceControl</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <Result>OK</Result>
 * </Response>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceControlResponse extends XmlBean {
    @XmlElement(name = "CmdType")
    private final String cmdType = "DeviceControl";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "Result")
    private String result;
}