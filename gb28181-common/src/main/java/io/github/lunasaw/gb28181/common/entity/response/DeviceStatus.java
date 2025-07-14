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
 * <CmdType>DeviceStatus</CmdType>
 * <SN>sn</SN>
 * <DeviceID>channelId</DeviceID>
 * <Result>OK</Result>
 * <Online>ONLINE</Online>
 * <Status>OK</Status>
 * </Response>
 * 
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceStatus extends DeviceBase {

    /**
     * OK
     */
    @XmlElement(name = "Result")
    private String Result = "OK";

    /**
     * "ONLINE":"OFFLINE"
     */
    @XmlElement(name = "Online")
    private String Online = "ONLINE";

    /**
     * OK
     */
    @XmlElement(name = "Status")
    private String status = "OK";

    public DeviceStatus(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

}
