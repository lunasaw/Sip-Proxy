package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GB28181-2022 A.2.6.14 巡航轨迹查询应答 (cmdType=CruiseTrackQuery)
 *
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class CruiseTrackResponse extends XmlBean {

    @XmlElement(name = "CmdType")
    private String cmdType = CmdTypeEnum.CRUISE_TRACK_QUERY.getType();

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "Number")
    private Integer number;

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "SumNum")
    private Integer sumNum;

    @XmlElement(name = "CruisePointList")
    private CruisePointList cruisePointList;

    public CruiseTrackResponse(String sn, String deviceId) {
        this.sn = sn;
        this.deviceId = deviceId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "CruisePointList")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CruisePointList {

        @XmlAttribute(name = "Num")
        private Integer num;

        @XmlElement(name = "CruisePoint")
        private List<CruisePoint> points;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "CruisePoint")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CruisePoint {

        @XmlElement(name = "PresetIndex")
        private Integer presetIndex;

        @XmlElement(name = "StayTime")
        private Integer stayTime;

        /**
         * 云台速度：1～15
         */
        @XmlElement(name = "Speed")
        private Integer speed;
    }
}
