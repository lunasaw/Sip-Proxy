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


    /**
     * 产生通知时间（必选）
     */
    @XmlElement(name = "Time")
    private String time;
    
    /**
     * 经度（必选）- GB28181标准要求double类型
     */
    @XmlElement(name = "Longitude")
    private Double longitude;
    
    /**
     * 纬度（必选）- GB28181标准要求double类型
     */
    @XmlElement(name = "Latitude")
    private Double latitude;
    
    /**
     * 速度，单位：km/h（可选）- GB28181标准要求double类型
     */
    @XmlElement(name = "Speed")
    private Double speed;
    
    /**
     * 方向，取值为当前摄像头方向与正北方的顺时针夹角，取值范围0°～360°，单位：(°)（可选）
     * GB28181标准要求double类型
     */
    @XmlElement(name = "Direction")
    private Double direction;
    
    /**
     * 海拔高度，单位：m（可选）- GB28181标准要求double类型
     */
    @XmlElement(name = "Altitude")
    private Double altitude;

    public MobilePositionNotify(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

}
