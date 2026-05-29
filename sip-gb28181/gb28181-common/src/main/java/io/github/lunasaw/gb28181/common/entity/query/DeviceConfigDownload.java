package io.github.lunasaw.gb28181.common.entity.query;

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
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Control>
 * <CmdType>DeviceConfig</CmdType>
 * <SN>150959</SN>
 * <DeviceID>channelId</DeviceID>
 * <BasicParam>
 * <Name>name</Name>
 * <Expiration>30</Expiration>
 * <HeartBeatInterval>300</HeartBeatInterval>
 * <HeartBeatCount>300</HeartBeatCount>
 * </BasicParam>
 * </Control>
 * 
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceConfigDownload extends DeviceBase {


    /**
     * 查询配置参数类型(必选),可查询的配 置 类 型 包 括
     * 基 本 参 数 配 置:BasicParam,
     * 视 频 参数范围:VideoParamOpt,SVAC
     * 编 码 配 置:SVACEncodeConfig,SVAC
     * 解 码 配 置:SVACDe-codeConfig。
     * 可同时查询多个配置类型,各类型以“/”分隔,可返回与查询
     * SN 值相同的多个响应,每个响应对应一个配置类型
     */
    @XmlElement(name = "ConfigType")
    public String configType;

    public DeviceConfigDownload(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

}
