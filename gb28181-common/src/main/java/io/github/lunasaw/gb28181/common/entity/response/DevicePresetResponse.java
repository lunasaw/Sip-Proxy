package io.github.lunasaw.gb28181.common.entity.response;

import java.util.List;

import jakarta.xml.bind.annotation.*;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.assertj.core.util.Lists;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Response>
 * <CmdType>DeviceStatus</CmdType>
 * <SN>sn</SN>
 * <DeviceID>channelId</DeviceID>
 * <Result>OK</Result>
 * <Online>statusStr</Online>
 * <Status>OK</Status>
 * </Response>
 * 
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class DevicePresetResponse extends DeviceBase {

    /**
     * 当前配置的预置位记录,当未配置预置位时不填写
     */
    @XmlElement(name = "Item")
    @XmlElementWrapper(name = "PresetList")
    private List<PresetItem> presetList;

    @Getter
    @Setter
    @XmlRootElement(name = "Item")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PresetItem {

        /**
         * 预置位编码(必选)
         */
        @XmlElement(name = "PresetID")
        private String presetID;

        /**
         * 预置位名称(必选)
         */
        @XmlElement(name = "PresetName")
        private String presetName;

        /**
         * 列表项个数,当未配置预置位时取值为0(必选)
         */
        @XmlElement(name = "Num")
        private String num;

    }

    public DevicePresetResponse(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

    public static void main(String[] args) {
        DevicePresetResponse response = new DevicePresetResponse("DevicePreset", "150959", "channelId");
        PresetItem presetItem = new PresetItem();
        presetItem.setPresetID("1");
        presetItem.setPresetName("name");

        response.setPresetList(Lists.newArrayList(presetItem));
        System.out.println(response);
    }
}
