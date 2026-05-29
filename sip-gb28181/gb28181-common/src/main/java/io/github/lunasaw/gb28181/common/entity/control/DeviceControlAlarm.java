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
 * /**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Control>
 * <CmdType>DeviceControl</CmdType>
 * <SN>179173</SN>
 * <DeviceID>213</DeviceID>
 * <AlarmCmd>ResetAlarm</AlarmCmd>
 * <Info>
 * <AlarmMethod>123</AlarmMethod>
 * <AlarmType>alarmType</AlarmType>
 * </Info>
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
public class DeviceControlAlarm extends DeviceControlBase {

    @XmlElement(name = "AlarmCmd")
    public String    alarmCmd;

    @XmlElement(name = "Info")
    public AlarmInfo alarmInfo;

    public DeviceControlAlarm(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("AlarmCmd");
    }

    public static void main(String[] args) {
        DeviceControlAlarm alarm = new DeviceControlAlarm();
        alarm.setCmdType("DeviceControl");
        alarm.setSn("179173");
        alarm.setDeviceId("123");
        alarm.setAlarmCmd("ResetAlarm");

        AlarmInfo alarmInfo = new AlarmInfo();
        alarmInfo.setAlarmMethod("!231");
        alarmInfo.setAlarmType("alarmType");
        alarm.setAlarmInfo(alarmInfo);

        System.out.println(alarm);

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "Info")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AlarmInfo {

        @XmlElement(name = "AlarmMethod")
        public String alarmMethod;

        @XmlElement(name = "AlarmType")
        public String alarmType;

    }
}
