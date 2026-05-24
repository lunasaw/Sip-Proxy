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
 * GB28181-2022 A.2.6.13 巡航轨迹列表查询应答 (cmdType=CruiseTrackListQuery)
 *
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class CruiseTrackListResponse extends XmlBean {

    @XmlElement(name = "CmdType")
    private String cmdType = CmdTypeEnum.CRUISE_TRACK_LIST_QUERY.getType();

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "SumNum")
    private Integer sumNum;

    @XmlElement(name = "CruiseTrackList")
    private CruiseTrackList cruiseTrackList;

    public CruiseTrackListResponse(String sn, String deviceId) {
        this.sn = sn;
        this.deviceId = deviceId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "CruiseTrackList")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CruiseTrackList {

        @XmlAttribute(name = "Num")
        private Integer num;

        @XmlElement(name = "CruiseTrack")
        private List<CruiseTrack> tracks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "CruiseTrack")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CruiseTrack {

        @XmlElement(name = "Number")
        private Integer number;

        @XmlElement(name = "Name")
        private String name;
    }
}
