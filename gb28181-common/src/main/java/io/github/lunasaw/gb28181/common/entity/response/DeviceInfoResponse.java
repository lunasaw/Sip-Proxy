package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GB28181协议 A.2.6 f）设备信息查询应答
 * <pre>
 * <Response>
 *   <CmdType>DeviceInfo</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <DeviceName>摄像头1</DeviceName>
 *   <Result>OK</Result>
 *   <Manufacturer>海康</Manufacturer>
 *   <Model>DS-2CD3T</Model>
 *   <Firmware>V5.5.0</Firmware>
 *   <Channel>4</Channel>
 *   <Info>扩展信息</Info>
 * </Response>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceInfoResponse extends XmlBean {
    @XmlElement(name = "CmdType")
    private final String cmdType = "DeviceInfo";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "DeviceName")
    private String deviceName;

    @XmlElement(name = "Result")
    private String result;

    @XmlElement(name = "Manufacturer")
    private String manufacturer;

    @XmlElement(name = "Model")
    private String model;

    @XmlElement(name = "Firmware")
    private String firmware;

    @XmlElement(name = "Channel")
    private Integer channel;

    @XmlElement(name = "Info")
    private List<String> info;
}