package io.github.lunasaw.gb28181.common.entity.notify;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 §A.2.5.8 设备实时视音频回传通知。
 *
 * <pre>{@code
 * <?xml version="1.0" encoding="GB2312"?>
 * <Notify>
 *   <CmdType>VideoUploadNotify</CmdType>
 *   <SN>17430</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <Time>2022-09-01T10:00:00</Time>
 *   <Longitude>116.40</Longitude>
 *   <Latitude>39.90</Latitude>
 * </Notify>
 * }</pre>
 *
 * <p>设备开始/结束实时视音频回传时（执法记录仪、移动单警等），向平台主动通知。
 * 平台据此做"是否启用回传媒体流"的会话调度。
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Notify")
@XmlAccessorType(XmlAccessType.FIELD)
public class VideoUploadNotify extends DeviceBase {

    /** 上报通知时间（必选）。 */
    @XmlElement(name = "Time")
    private String time;

    /** 经度（可选）。 */
    @XmlElement(name = "Longitude")
    private Double longitude;

    /** 纬度（可选）。 */
    @XmlElement(name = "Latitude")
    private Double latitude;
}
