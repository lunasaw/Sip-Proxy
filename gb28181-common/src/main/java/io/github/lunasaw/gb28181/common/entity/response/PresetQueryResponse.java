package io.github.lunasaw.gb28181.common.entity.response;

import jakarta.xml.bind.annotation.*;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GB28181协议 A.2.6 k）设备预置位查询应答
 * <pre>
 * <Response>
 *   <CmdType>PresetQuery</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001320000001</DeviceID>
 *   <PresetList Num="2">
 *     <Item>
 *       <PresetID>1</PresetID>
 *       <PresetName>预置位1</PresetName>
 *     </Item>
 *     <Item>
 *       <PresetID>2</PresetID>
 *       <PresetName>预置位2</PresetName>
 *     </Item>
 *   </PresetList>
 * </Response>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class PresetQueryResponse extends XmlBean {
    @XmlElement(name = "CmdType")
    private final String cmdType = "PresetQuery";

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "PresetList")
    private PresetList presetList;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PresetList {
        @XmlAttribute(name = "Num")
        private Integer num;

        @XmlElement(name = "Item")
        private List<PresetItem> items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PresetItem {
        @XmlElement(name = "PresetID")
        private String presetId;

        @XmlElement(name = "PresetName")
        private String presetName;
    }
}