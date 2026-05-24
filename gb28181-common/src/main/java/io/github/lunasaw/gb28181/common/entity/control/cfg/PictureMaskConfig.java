package io.github.lunasaw.gb28181.common.entity.control.cfg;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GB28181-2022 §A.2.3.2.8 视频遮挡区域配置（cmdType=DeviceConfig，子标签 PictureMask）。
 *
 * <p>用于平台向设备下发"视频画面遮挡区域"配置，避免敏感区域被录像。
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class PictureMaskConfig extends DeviceControlBase {

    @XmlElement(name = "PictureMask")
    private PictureMask pictureMask;

    public PictureMaskConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("PictureMask");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PictureMask {

        /** 视频遮挡使能：0=关，1=开 */
        @XmlElement(name = "On")
        private Integer on;

        /** 视频遮挡区域数（最多 4 个） */
        @XmlElement(name = "SumNum")
        private Integer sumNum;

        @XmlElementWrapper(name = "RegionList")
        @XmlElement(name = "Region")
        private List<MaskRegion> regions;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class MaskRegion {

            /** 区域左上角 X 坐标（百分比，0-99） */
            @XmlElement(name = "X")
            private Integer x;

            /** 区域左上角 Y 坐标（百分比，0-99） */
            @XmlElement(name = "Y")
            private Integer y;

            /** 区域宽度（百分比，0-100） */
            @XmlElement(name = "Width")
            private Integer width;

            /** 区域高度（百分比，0-100） */
            @XmlElement(name = "Height")
            private Integer height;
        }
    }
}
