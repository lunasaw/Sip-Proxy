package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.Getter;
import lombok.Setter;

/**
 * GB28181-2022 A.2.4.12 巡航轨迹查询请求 (cmdType=CruiseTrackQuery)
 *
 * @author luna
 */
@Getter
@Setter
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class CruiseTrackQuery extends DeviceBase {

    /**
     * 轨迹编号：0-第一条轨迹，1-第二条轨迹
     */
    @XmlElement(name = "Number")
    private Integer number;

    public CruiseTrackQuery() {
    }

    public CruiseTrackQuery(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }
}
