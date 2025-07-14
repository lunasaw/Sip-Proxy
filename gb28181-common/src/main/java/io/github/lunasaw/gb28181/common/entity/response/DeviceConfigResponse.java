package io.github.lunasaw.gb28181.common.entity.response;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Response>
 * <CmdType>ConfigDownload</CmdType>
 * <SN>340294132</SN>
 * <DeviceID>34020000001320000264</DeviceID>
 * <Result>OK</Result>
 * <BasicParam>
 * <Name>IP CAMERA</Name>
 * <DeviceID>34020000001320000264</DeviceID>
 * <SIPServerID>34020000002000000001</SIPServerID>
 * <SIPServerIP>192.168.2.135</SIPServerIP>
 * <SIPServerPort>5060</SIPServerPort>
 * <DomainName>3402000000</DomainName>
 * <Expiration>3600</Expiration>
 * <Password>12345678</Password>
 * <HeartBeatInterval>60</HeartBeatInterval>
 * <HeartBeatCount>3</HeartBeatCount>
 * </BasicParam>
 * </Response>
 * 
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceConfigResponse extends DeviceBase {

    /**
     * OK
     */
    @XmlElement(name = "Result")
    private String     Result;

    @XmlElement(name = "BasicParam")
    private BasicParam BasicParam;

    @Getter
    @Setter
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BasicParam {

        @XmlElement(name = "Name")
        private String Name;

        @XmlElement(name = "DeviceID")
        private String DeviceID;

        @XmlElement(name = "SIPServerID")
        private String SIPServerID;

        @XmlElement(name = "SIPServerIP")
        private String SIPServerIP;

        @XmlElement(name = "SIPServerPort")
        private String SIPServerPort;

        @XmlElement(name = "DomainName")
        private String DomainName;

        @XmlElement(name = "Expiration")
        private String Expiration;

        @XmlElement(name = "Password")
        private String Password;

        @XmlElement(name = "HeartBeatInterval")
        private String HeartBeatInterval;

        @XmlElement(name = "HeartBeatCount")
        private String HeartBeatCount;
    }

    public DeviceConfigResponse(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

}
