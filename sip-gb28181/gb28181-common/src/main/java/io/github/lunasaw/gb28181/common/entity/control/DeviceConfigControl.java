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
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Control>
 * <CmdType>DeviceConfig</CmdType>
 * <SN>150959</SN>
 * <DeviceID>channelId</DeviceID>
 * <BasicParam>
 * <Name>name</Name>
 * <Expiration>30</Expiration>
 * <HeartBeatInterval>300</HeartBeatInterval>
 * <HeartBeatCount>300</HeartBeatCount>
 * </BasicParam>
 * </Control>
 * 
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceConfigControl extends DeviceControlBase {


    @XmlElement(name = "BasicParam")
    private BasicParam basicParam;

    public DeviceConfigControl(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("BasicParam");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "BasicParam")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BasicParam {

        @XmlElement(name = "Name")
        private String name;
        @XmlElement(name = "Expiration")
        private String expiration;
        @XmlElement(name = "HeartBeatInterval")
        private String heartBeatInterval;
        @XmlElement(name = "HeartBeatCount")
        private String heartBeatCount;

    }

    public static void main(String[] args) {
        DeviceConfigControl alarm = new DeviceConfigControl();
        alarm.setCmdType("DeviceControl");
        alarm.setSn("179173");
        alarm.setDeviceId("123");

        BasicParam basicParam = new BasicParam();
        basicParam.setExpiration("30");
        basicParam.setHeartBeatCount("31");
        basicParam.setHeartBeatInterval("300");
        basicParam.setName("QWWQ");
        alarm.setBasicParam(basicParam);

        System.out.println(alarm);

    }
}
