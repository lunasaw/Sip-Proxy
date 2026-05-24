package io.github.lunasaw.gb28181.common.entity.control.cfg;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GB28181-2022 §A.2.3.2.6 录像计划配置（cmdType=DeviceConfig，子标签 VideoRecordPlan）。
 *
 * <p>录像计划描述设备在一周内不同时间段的录像策略（连续/事件触发/定时等）。
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class VideoRecordPlanConfig extends DeviceControlBase {

    @XmlElement(name = "VideoRecordPlan")
    private VideoRecordPlan videoRecordPlan;

    public VideoRecordPlanConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("VideoRecordPlan");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VideoRecordPlan {

        /** 录像计划名称（业务定义） */
        @XmlElement(name = "Name")
        private String name;

        /** 计划开始日期 ISO8601，格式 yyyy-MM-dd */
        @XmlElement(name = "StartDate")
        private String startDate;

        /** 计划结束日期 ISO8601，格式 yyyy-MM-dd */
        @XmlElement(name = "EndDate")
        private String endDate;

        /** 一周 7 天的录像策略，逐日描述 */
        @XmlElementWrapper(name = "WeekScheduleList")
        @XmlElement(name = "WeekSchedule")
        private List<DaySchedule> weekScheduleList;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class DaySchedule {

            /** 1=星期日，2=星期一，... 7=星期六 */
            @XmlElement(name = "DayOfWeek")
            private Integer dayOfWeek;

            /** 当日录像时段，HH:MM:SS-HH:MM:SS 多段以 ; 分隔 */
            @XmlElement(name = "TimeRanges")
            private String timeRanges;

            /** 录像类型：1=连续录像，2=移动侦测录像，3=报警录像 */
            @XmlElement(name = "RecordType")
            private Integer recordType;
        }
    }
}
