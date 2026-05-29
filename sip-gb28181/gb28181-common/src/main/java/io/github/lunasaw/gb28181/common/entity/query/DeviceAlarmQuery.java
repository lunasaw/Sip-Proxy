package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.Getter;
import lombok.Setter;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Query>
 * <CmdType>Alarm</CmdType>
 * <SN>217408</SN>
 * <DeviceID>123</DeviceID>
 * <StartAlarmPriority>12312</StartAlarmPriority>
 * <EndAlarmPriority>123</EndAlarmPriority>
 * <AlarmMethod>alarmMethod</AlarmMethod>
 * <AlarmType>alarmType</AlarmType>
 * <StartAlarmTime>startTime</StartAlarmTime>
 * <EndAlarmTime>endTime</EndAlarmTime>
 * </Query>
 * 
 * @author luna
 */
@Getter
@Setter
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceAlarmQuery extends XmlBean {
    @XmlElement(name = "CmdType")
    public String cmdType;

    @XmlElement(name = "SN")
    public String sn;

    @XmlElement(name = "DeviceID")
    public String deviceId;

    @XmlElement(name = "StartTime")
    public String startTime;

    @XmlElement(name = "EndTime")
    public String endTime;

    @XmlElement(name = "StartAlarmPriority")
    public String startAlarmPriority;

    /**
     * 
     */
    @XmlElement(name = "endAlarmPriority")
    public String endAlarmPriority;

    @XmlElement(name = "AlarmType")
    public String alarmType;

    @XmlElement(name = "AlarmMethod")
    public String alarmMethod;



    public DeviceAlarmQuery() {}

    public DeviceAlarmQuery(String cmdType, String sn, String deviceId) {
        this.cmdType = cmdType;
        this.sn = sn;
        this.deviceId = deviceId;
    }

    public static void main(String[] args) {

    }
}
