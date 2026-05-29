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
 * <CmdType>DeviceControl</CmdType>
 * <SN>840481</SN>
 * <DeviceID>channelId</DeviceID>
 * <HomePosition>
 * <Enabled>1</Enabled>
 * <ResetTime>resetTime</ResetTime>
 * <PresetIndex>presetIndex</PresetIndex>
 * </HomePosition>
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
public class DeviceControlPosition extends DeviceControlBase {


    @XmlElement(name = "HomePosition")
    public HomePosition homePosition;

    public DeviceControlPosition(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("HomePosition");
    }

    public static void main(String[] args) {
        DeviceControlPosition alarm = new DeviceControlPosition();
        alarm.setCmdType("DeviceControl");
        alarm.setSn("179173");
        alarm.setDeviceId("123");

        HomePosition homePosition = new HomePosition();
        homePosition.setEnabled("1");
        homePosition.setResetTime("222");
        homePosition.setPresetIndex("2");
        alarm.setHomePosition(homePosition);

        System.out.println(alarm);

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "HomePosition")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HomePosition {

        @XmlElement(name = "Enabled")
        public String enabled;

        @XmlElement(name = "ResetTime")
        public String resetTime;
        @XmlElement(name = "PresetIndex")
        private String presetIndex;

    }
}
