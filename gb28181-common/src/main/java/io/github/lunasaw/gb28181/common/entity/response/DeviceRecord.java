package io.github.lunasaw.gb28181.common.entity.response;

import java.util.List;

import jakarta.xml.bind.annotation.*;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Response>
 * <CmdType>RecordInfo</CmdType>
 * <SN>740143</SN>
 * <DeviceID>33010602010002719420</DeviceID>
 * <SumNum>130</SumNum>
 * <RecordList Num="130">
 * <Item>
 * <DeviceID>null</DeviceID>
 * <Name>null</Name>
 * <StartTime>2023-10-16T00:05:00</StartTime>
 * <EndTime>2023-10-16T00:10:03</EndTime>
 * <Secrecy>0</Secrecy>
 * <Type>null</Type>
 * <FileSize>6245911</FileSize>
 * <FilePath>/home/www/ZLMediaKit/release/linux/Debug/www/record/onvif/037a00020053fafd470f__D01_CH01_Main/2023-10-16/000500-001003.mp4</FilePath>
 * </Item>
 * </RecordList>
 * </Response>
 *
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceRecord extends DeviceBase {

    /**
     * 总数
     */
    @XmlElement(name = "SumNum")
    private int sumNum;

    /**
     * "ONLINE":"OFFLINE"
     */
    @XmlElement(name = "Item")
    @XmlElementWrapper(name = "RecordList")
    private List<RecordItem> recordList;

    public DeviceRecord(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

    @Getter
    @Setter
    @XmlRootElement(name = "Item")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RecordItem {

        @XmlElement(name = "DeviceID")
        private String deviceId;
        @XmlElement(name = "Name")
        private String name;
        /**
         * ISO8601格式
         */
        @XmlElement(name = "StartTime")
        private String startTime;
        /**
         * ISO8601格式
         */
        @XmlElement(name = "EndTime")
        private String endTime;
        @XmlElement(name = "Secrecy")
        private String secrecy;
        @XmlElement(name = "Type")
        private String type;
        @XmlElement(name = "FileSize")
        private String fileSize;
        @XmlElement(name = "FilePath")
        private String filePath;
    }

}
