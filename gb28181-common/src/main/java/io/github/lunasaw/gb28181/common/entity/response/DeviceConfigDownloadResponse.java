package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.FrameMirrorConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.PictureMaskConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.SVACDecodeConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.SVACEncodeConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamAttributeConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamOptConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoRecordPlanConfig;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 §A.2.6.9 设备配置查询应答（cmdType=ConfigDownload）。
 *
 * <p>设备返回当前配置画像，按 ConfigType 携带相应可选子标签。所有 cfg 字段都是可选的。
 *
 * <pre>{@code
 * <Response>
 *   <CmdType>ConfigDownload</CmdType>
 *   <SN>17430</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <Result>OK</Result>
 *   <BasicParam>...</BasicParam>            <!-- 可选 -->
 *   <VideoParamOpt>...</VideoParamOpt>      <!-- 可选 -->
 *   <SVACEncodeConfig>...</SVACEncodeConfig><!-- 可选 -->
 *   <SVACDecodeConfig>...</SVACDecodeConfig><!-- 可选 -->
 *   <VideoParamAttribute>...</VideoParamAttribute><!-- 可选 -->
 *   <VideoRecordPlan>...</VideoRecordPlan>  <!-- 可选 -->
 *   <VideoAlarmRecord>...</VideoAlarmRecord><!-- 可选 -->
 *   <PictureMask>...</PictureMask>          <!-- 可选 -->
 *   <FrameMirror>...</FrameMirror>          <!-- 可选 -->
 *   <AlarmReport>...</AlarmReport>          <!-- 可选 -->
 *   <OSDConfig>...</OSDConfig>              <!-- 可选 -->
 * </Response>
 * }</pre>
 *
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceConfigDownloadResponse extends XmlBean {

    @XmlElement(name = "CmdType")
    private final String cmdType = "ConfigDownload";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    /** 查询结果（OK / Error）。 */
    @XmlElement(name = "Result")
    private String result;

    @XmlElement(name = "BasicParam")
    private BasicParamConfig basicParam;

    @XmlElement(name = "VideoParamOpt")
    private VideoParamOptConfig videoParamOpt;

    @XmlElement(name = "SVACEncodeConfig")
    private SVACEncodeConfig svacEncodeConfig;

    @XmlElement(name = "SVACDecodeConfig")
    private SVACDecodeConfig svacDecodeConfig;

    @XmlElement(name = "VideoParamAttribute")
    private VideoParamAttributeConfig videoParamAttribute;

    @XmlElement(name = "VideoRecordPlan")
    private VideoRecordPlanConfig videoRecordPlan;

    @XmlElement(name = "VideoAlarmRecord")
    private VideoAlarmRecordConfig videoAlarmRecord;

    @XmlElement(name = "PictureMask")
    private PictureMaskConfig pictureMask;

    @XmlElement(name = "FrameMirror")
    private FrameMirrorConfig frameMirror;

    @XmlElement(name = "AlarmReport")
    private AlarmReportConfig alarmReport;

    @XmlElement(name = "OSDConfig")
    private OsdConfig osdConfig;

    /**
     * GB28181-2022 §A.2.1.19 基本参数配置（占位，业务方按需补字段）。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BasicParamConfig {
        /** 设备名称。 */
        @XmlElement(name = "Name")
        private String name;

        /** 注册过期时间（秒）。 */
        @XmlElement(name = "Expiration")
        private Integer expiration;

        /** 心跳间隔（秒）。 */
        @XmlElement(name = "HeartBeatInterval")
        private Integer heartBeatInterval;

        /** 心跳超时次数。 */
        @XmlElement(name = "HeartBeatCount")
        private Integer heartBeatCount;
    }
}
