package io.github.lunasaw.gb28181.common.entity.notify;

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
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Notify>
 * <CmdType>MobilePosition</CmdType>
 * <SN>383451</SN>
 * <DeviceID>123</DeviceID>
 * <Time>gpsMsgInfo.getTime() </Time>
 * <Longitude> gpsMsgInfo.getLng() </Longitude>
 * <Latitude>gpsMsgInfo.getLat() </Latitude>
 * <Speed>gpsMsgInfo.getSpeed()</Speed>
 * <Direction>gpsMsgInfo.getDirection()</Direction>
 * <Altitude>gpsMsgInfo.getAltitude()</Altitude>
 * </Notify>
 * 
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Notify")
@XmlAccessorType(XmlAccessType.FIELD)
public class MobilePositionNotify extends DeviceBase {


    @XmlElement(name = "Time")
    private String time;
    @XmlElement(name = "Longitude")
    private String longitude;
    @XmlElement(name = "Latitude")
    private String latitude;
    @XmlElement(name = "Speed")
    private String speed;
    @XmlElement(name = "Direction")
    private String direction;
    @XmlElement(name = "Altitude")
    private String altitude;

    public MobilePositionNotify(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

}
