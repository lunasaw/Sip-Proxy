package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 A.2.4 b）设备目录信息查询请求
 * <pre>
 * <Query>
 *   <CmdType>Catalog</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <StartTime>2023-01-01T00:00:00</StartTime>
 *   <EndTime>2023-12-31T23:59:59</EndTime>
 * </Query>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class CatalogQuery {
    @XmlElement(name = "CmdType")
    private final String cmdType = "Catalog";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "StartTime")
    private String startTime;

    @XmlElement(name = "EndTime")
    private String endTime;
}