package io.github.lunasaw.gb28181.common.entity.control.cfg;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 A.2.3.2.7 / A.2.1.16 报警录像配置命令 (cmdType=DeviceConfig，子标签 VideoAlarmRecord)
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class VideoAlarmRecordConfig extends DeviceControlBase {

    @XmlElement(name = "VideoAlarmRecord")
    private VideoAlarmRecordInfo videoAlarmRecord;

    public VideoAlarmRecordConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("VideoAlarmRecord");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "VideoAlarmRecord")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VideoAlarmRecordInfo {

        /**
         * 0-否，1-是
         */
        @XmlElement(name = "RecordEnable")
        private Integer recordEnable;

        /**
         * 录像延时时间（秒）
         */
        @XmlElement(name = "RecordTime")
        private Integer recordTime;

        /**
         * 预录时间（秒）
         */
        @XmlElement(name = "PreRecordTime")
        private Integer preRecordTime;

        /**
         * 0-主码流，1-子码流1，2-子码流2
         */
        @XmlElement(name = "StreamNumber")
        private Integer streamNumber;
    }
}
