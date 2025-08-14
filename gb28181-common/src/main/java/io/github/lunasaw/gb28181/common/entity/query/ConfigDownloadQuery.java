package io.github.lunasaw.gb28181.common.entity.query;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 A.2.4 f）设备配置查询
 * <pre>
 * <Query>
 *   <CmdType>ConfigDownload</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <ConfigType>BasicParam/VideoParamOpt</ConfigType>
 * </Query>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConfigDownloadQuery extends XmlBean {
    @XmlElement(name = "CmdType")
    private final String cmdType = "ConfigDownload";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "ConfigType")
    private String configType;
}