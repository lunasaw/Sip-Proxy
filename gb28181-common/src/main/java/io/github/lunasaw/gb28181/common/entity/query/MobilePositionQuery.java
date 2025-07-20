package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 A.2.4 h）移动设备位置数据查询
 * <pre>
 * <Query>
 *   <CmdType>MobilePosition</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <Interval>5</Interval>
 * </Query>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class MobilePositionQuery {
    @XmlElement(name = "CmdType")
    private final String cmdType = "MobilePosition";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "Interval")
    private Integer interval;
}