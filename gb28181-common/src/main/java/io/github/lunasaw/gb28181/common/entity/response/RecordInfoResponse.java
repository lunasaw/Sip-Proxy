package io.github.lunasaw.gb28181.common.entity.response;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GB28181协议 A.2.6 h）文件目录检索应答
 * <pre>
 * <Response>
 *   <CmdType>RecordInfo</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <Name>摄像头1</Name>
 *   <SumNum>2</SumNum>
 *   <RecordList Num="2">
 *     <Item>...</Item>
 *     <Item>...</Item>
 *   </RecordList>
 * </Response>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecordInfoResponse {
    @XmlElement(name = "CmdType")
    private final String cmdType = "RecordInfo";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "SumNum")
    private Integer sumNum;

    @XmlElement(name = "RecordList")
    private RecordList recordList;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RecordList {
        @XmlAttribute(name = "Num")
        private Integer num;

        @XmlElement(name = "Item")
        private List<RecordItem> items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RecordItem {
        // 录像项字段，按实际协议补充
        @XmlElement(name = "RecordID")
        private String recordId;
        @XmlElement(name = "Name")
        private String name;
        @XmlElement(name = "FilePath")
        private String filePath;
        @XmlElement(name = "Address")
        private String address;
        @XmlElement(name = "StartTime")
        private String startTime;
        @XmlElement(name = "EndTime")
        private String endTime;
        @XmlElement(name = "Secrecy")
        private Integer secrecy;
        @XmlElement(name = "Type")
        private String type;
        @XmlElement(name = "RecorderID")
        private String recorderId;
        @XmlElement(name = "FileSize")
        private Long fileSize;
    }
}