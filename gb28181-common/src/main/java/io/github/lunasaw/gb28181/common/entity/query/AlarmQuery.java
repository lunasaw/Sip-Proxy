package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 A.2.4 e）报警查询
 * <pre>
 * <Query>
 *   <CmdType>Alarm</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <StartAlarmPriority>0</StartAlarmPriority>
 *   <EndAlarmPriority>4</EndAlarmPriority>
 *   <AlarmMethod>12</AlarmMethod>
 *   <AlarmType>1</AlarmType>
 *   <StartAlarmTime>2023-01-01T00:00:00</StartAlarmTime>
 *   <EndAlarmTime>2023-12-31T23:59:59</EndAlarmTime>
 * </Query>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlarmQuery {
    @XmlElement(name = "CmdType")
    private final String cmdType = "Alarm";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "StartAlarmPriority")
    private String startAlarmPriority;

    @XmlElement(name = "EndAlarmPriority")
    private String endAlarmPriority;

    @XmlElement(name = "AlarmMethod")
    private String alarmMethod;

    @XmlElement(name = "AlarmType")
    private String alarmType;

    @XmlElement(name = "StartAlarmTime")
    private String startAlarmTime;

    @XmlElement(name = "EndAlarmTime")
    private String endAlarmTime;
}