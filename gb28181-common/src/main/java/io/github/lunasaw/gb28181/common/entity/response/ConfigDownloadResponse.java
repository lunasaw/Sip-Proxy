package io.github.lunasaw.gb28181.common.entity.response;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 A.2.6 j）设备配置查询应答
 * <pre>
 * <Response>
 *   <CmdType>ConfigDownload</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <Result>OK</Result>
 *   <BasicParam>...</BasicParam>
 *   <VideoParamOpt>...</VideoParamOpt>
 *   <SVACEncodeConfig>...</SVACEncodeConfig>
 *   <SVACDecodeConfig>...</SVACDecodeConfig>
 * </Response>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConfigDownloadResponse {
    @XmlElement(name = "CmdType")
    private final String cmdType = "ConfigDownload";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "Result")
    private String result;

    @XmlElement(name = "BasicParam")
    private String basicParam;

    @XmlElement(name = "VideoParamOpt")
    private String videoParamOpt;

    @XmlElement(name = "SVACEncodeConfig")
    private String svacEncodeConfig;

    @XmlElement(name = "SVACDecodeConfig")
    private String svacDecodeConfig;
}