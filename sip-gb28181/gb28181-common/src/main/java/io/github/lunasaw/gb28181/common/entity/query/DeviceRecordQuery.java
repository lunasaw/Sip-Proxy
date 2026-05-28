package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.luna.common.date.DateUtils;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.Getter;
import lombok.Setter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Query>
 * <CmdType>RecordInfo</CmdType>
 * <SN>sn</SN>
 * <DeviceID>channelId</DeviceID>
 * <StartTime>DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(startTime)</StartTime>
 * <EndTime> DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(endTime)</EndTime>
 * <Secrecy> secrecy </Secrecy>
 * <Type>type</Type>
 * </Query>
 * 
 * @author luna
 */
@Getter
@Setter
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceRecordQuery extends XmlBean {
    @XmlElement(name = "CmdType")
    public String cmdType;

    @XmlElement(name = "SN")
    public String sn;

    @XmlElement(name = "DeviceID")
    public String deviceId;

    @XmlElement(name = "StartTime")
    public String startTime;

    @XmlElement(name = "EndTime")
    public String endTime;

    @XmlElement(name = "Secrecy")
    public String secrecy;

    /**
     * 大华NVR要求必须增加一个值为all的文本元素节点Type
     *
     * all（time 或 alarm 或 manual 或 all）
     */
    @XmlElement(name = "Type")
    public String type;

    public DeviceRecordQuery() {}

    public DeviceRecordQuery(String cmdType, String sn, String deviceId) {
        this.cmdType = cmdType;
        this.sn = sn;
        this.deviceId = deviceId;
    }

    public static void main(String[] args) {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).withZone(ZoneId.of("Asia/Shanghai"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).withZone(ZoneId.of("Asia/Shanghai"));

        String format = dateTimeFormatter.format(formatter.parse("2023-11-11 10:10:10"));
        System.out.println(format);

        String s = DateUtils.formatTime(DateUtils.ISO8601_PATTERN, new Date());
        System.out.println(s);
    }
}
