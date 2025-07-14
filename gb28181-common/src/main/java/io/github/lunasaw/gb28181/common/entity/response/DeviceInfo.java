package io.github.lunasaw.gb28181.common.entity.response;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceInfo extends DeviceBase {

    @XmlElement(name = "DeviceName")
    private String deviceName;

    @XmlElement(name = "Result")
    private String result;
    /**
     * 设备生产商
     */
    @XmlElement(name = "Manufacturer")
    private String manufacturer;
    /**
     * 设备型号(可选)
     */
    @XmlElement(name = "Model")
    private String model;
    /**
     * 设备固件版本(可选)
     */
    @XmlElement(name = "Firmware")
    private String firmware;
    /**
     * 视频输入通道数(可选)
     */
    @XmlElement(name = "Channel")
    private int    channel;

    public DeviceInfo(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

    public static void main(String[] args) {
        DeviceInfo deviceInfo = new DeviceInfo();
        Object o = DeviceInfo.parseObj("<Response>\n" +
                "    <CmdType>DeviceInfo</CmdType>\n" +
                "    <SN>1</SN>\n" +
                "    <DeviceID>34020000001320000001</DeviceID>\n" +
                "    <Result>OK</Result>\n" +
                "    <DeviceName>34020000001320000001</DeviceName>\n" +
                "    <Manufacturer>HIK</Manufacturer>\n" +
                "    <Model>DS-2CD2T20FD-I5</Model>\n" +
                "    <Firmware>V5.5.82 build 180314</Firmware>\n" +
                "    <Channel>1</Channel>\n" +
                "</Response>", DeviceInfo.class);

        System.out.println(o);
    }
}
