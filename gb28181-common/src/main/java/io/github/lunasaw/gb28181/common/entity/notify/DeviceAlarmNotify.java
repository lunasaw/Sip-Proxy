package io.github.lunasaw.gb28181.common.entity.notify;


import com.luna.common.date.DateUtils;
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Notify>
 * <CmdType>Alarm</CmdType>
 * <SN>744523</SN>
 * <DeviceID>2133</DeviceID>
 * <AlarmPriority>AlarmPriority</AlarmPriority>
 * <AlarmMethod>deviceAlarm.getAlarmMethod()</AlarmMethod>
 * <AlarmTime>DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(deviceAlarm.getAlarmTime())</AlarmTime>
 * <AlarmDescription>deviceAlarm.getAlarmDescription() </AlarmDescription>
 * <Longitude>deviceAlarm.getLongitude()</Longitude>
 * <Latitude>deviceAlarm.getLatitude() </Latitude>
 * <info>
 * <AlarmType>deviceAlarm.getAlarmType() </AlarmType>
 * </info>
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
public class DeviceAlarmNotify extends XmlBean {
    @XmlElement(name = "CmdType")
    public String     cmdType;

    @XmlElement(name = "SN")
    public String     sn;

    @XmlElement(name = "DeviceID")
    public String     deviceId;

    @XmlElement(name = "AlarmPriority")
    public String     alarmPriority;

    @XmlElement(name = "AlarmMethod")
    public String     alarmMethod;

    /**
     * ISO8601
     */
    @XmlElement(name = "AlarmTime")
    public String     alarmTime;

    /**
     * 经度
     */
    @XmlElement(name = "Longitude")
    public String     longitude;

    /**
     * 纬度
     */
    @XmlElement(name = "Latitude")
    public String     latitude;

    @XmlElement(name = "Info")
    private AlarmInfo info;

    public DeviceAlarmNotify(String cmdType, String sn, String deviceId) {
        this.cmdType = cmdType;
        this.sn = sn;
        this.deviceId = deviceId;
    }

    public static void main(String[] args) {
        DeviceAlarmNotify deviceCatalog = new DeviceAlarmNotify();
        deviceCatalog.setDeviceId("123");
        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setAddress("!23");
        System.out.println(deviceCatalog);
    }

    public DeviceAlarmNotify setAlarm(DeviceAlarm deviceAlarm) {
        setAlarmPriority(deviceAlarm.getAlarmPriority());
        setAlarmMethod(deviceAlarm.getAlarmMethod());
        setAlarmTime(DateUtils.formatTime(DateUtils.ISO8601_PATTERN, deviceAlarm.getAlarmTime()));
        setLongitude(String.valueOf(deviceAlarm.getLongitude()));
        setLatitude(String.valueOf(deviceAlarm.getLatitude()));
        setInfo(new AlarmInfo(deviceAlarm.getAlarmType()));
        return this;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "Info")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AlarmInfo {

        @XmlElement(name = "AlarmType")
        public String alarmType;
    }
}
