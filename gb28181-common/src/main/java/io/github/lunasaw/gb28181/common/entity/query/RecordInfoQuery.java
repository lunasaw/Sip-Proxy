package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 A.2.4 d）文件目录检索请求
 * <pre>
 * <Query>
 *   <CmdType>RecordInfo</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <StartTime>2023-01-01T00:00:00</StartTime>
 *   <EndTime>2023-12-31T23:59:59</EndTime>
 *   <FilePath>/path/to/file</FilePath>
 *   <Address>上海</Address>
 *   <Secrecy>0</Secrecy>
 *   <Type>all</Type>
 *   <RecorderID>recorder1</RecorderID>
 *   <IndistinctQuery>0</IndistinctQuery>
 * </Query>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecordInfoQuery {
    @XmlElement(name = "CmdType")
    private final String cmdType = "RecordInfo";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "StartTime")
    private String startTime;

    @XmlElement(name = "EndTime")
    private String endTime;

    @XmlElement(name = "FilePath")
    private String filePath;

    @XmlElement(name = "Address")
    private String address;

    @XmlElement(name = "Secrecy")
    private Integer secrecy;

    @XmlElement(name = "Type")
    private String type;

    @XmlElement(name = "RecorderID")
    private String recorderId;

    @XmlElement(name = "IndistinctQuery")
    private String indistinctQuery;
}