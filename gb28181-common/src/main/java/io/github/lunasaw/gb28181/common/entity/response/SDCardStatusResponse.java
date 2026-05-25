package io.github.lunasaw.gb28181.common.entity.response;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GB28181-2022 A.2.6.16 存储卡状态查询应答 (cmdType=SDCardStatus)
 *
 * <pre>
 * &lt;Response&gt;
 *   &lt;CmdType&gt;SDCardStatus&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;SumNum&gt;2&lt;/SumNum&gt;
 *   &lt;SDCardStatusInfo Num="2"&gt;
 *     &lt;Item&gt;
 *       &lt;ID&gt;1&lt;/ID&gt;
 *       &lt;HddName&gt;SD1&lt;/HddName&gt;
 *       &lt;Status&gt;ok&lt;/Status&gt;
 *       &lt;Capacity&gt;131072&lt;/Capacity&gt;
 *       &lt;FreeSpace&gt;65536&lt;/FreeSpace&gt;
 *     &lt;/Item&gt;
 *   &lt;/SDCardStatusInfo&gt;
 * &lt;/Response&gt;
 * </pre>
 *
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class SDCardStatusResponse extends XmlBean {

    @XmlElement(name = "CmdType")
    private String cmdType = CmdTypeEnum.SD_CARD_STATUS.getType();

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "SumNum")
    private Integer sumNum;

    @XmlElement(name = "SDCardStatusInfo")
    private SDCardStatusInfo sdCardStatusInfo;

    public SDCardStatusResponse(String sn, String deviceId) {
        this.sn = sn;
        this.deviceId = deviceId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "SDCardStatusInfo")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SDCardStatusInfo {

        @XmlAttribute(name = "Num")
        private Integer num;

        @XmlElement(name = "Item")
        private List<SDCardItem> items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "Item")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SDCardItem {

        @XmlElement(name = "ID")
        private Integer id;

        @XmlElement(name = "HddName")
        private String hddName;

        /**
         * ok-正常, formatting-格式化, unformatted-未格式化, idle-空闲, error-错误
         */
        @XmlElement(name = "Status")
        private String status;

        /**
         * 格式化进度 0-100，百分比
         */
        @XmlElement(name = "FormatProgress")
        private Integer formatProgress;

        /**
         * 存储容量，单位 MB
         */
        @XmlElement(name = "Capacity")
        private Integer capacity;

        /**
         * 剩余存储容量，单位 MB
         */
        @XmlElement(name = "FreeSpace")
        private Integer freeSpace;
    }
}
