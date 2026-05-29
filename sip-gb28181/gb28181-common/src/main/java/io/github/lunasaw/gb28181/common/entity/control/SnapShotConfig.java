package io.github.lunasaw.gb28181.common.entity.control;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 A.2.3.2.12 / A.2.1.24 图像抓拍配置命令 (cmdType=DeviceConfig)
 *
 * <pre>
 * &lt;Control&gt;
 *   &lt;CmdType&gt;DeviceConfig&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;SnapShotConfig&gt;
 *     &lt;SnapNum&gt;1&lt;/SnapNum&gt;
 *     &lt;Interval&gt;1&lt;/Interval&gt;
 *     &lt;UploadURL&gt;http://example.com/upload&lt;/UploadURL&gt;
 *     &lt;SessionID&gt;32-128 chars&lt;/SessionID&gt;
 *   &lt;/SnapShotConfig&gt;
 * &lt;/Control&gt;
 * </pre>
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class SnapShotConfig extends DeviceControlBase {

    @XmlElement(name = "SnapShotConfig")
    private SnapShotInfo snapShotConfig;

    public SnapShotConfig(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("SnapShotConfig");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement(name = "SnapShotConfig")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SnapShotInfo {

        /**
         * 连拍张数（1-10），手动抓拍取值1
         */
        @XmlElement(name = "SnapNum")
        private Integer snapNum;

        /**
         * 单张抓拍间隔时间（秒），最短1秒
         */
        @XmlElement(name = "Interval")
        private Integer interval;

        /**
         * 抓拍图像上传路径
         */
        @XmlElement(name = "UploadURL")
        private String uploadURL;

        /**
         * 会话 ID（32-128 字节，由大小写英文字母、数字、短划线组成）
         */
        @XmlElement(name = "SessionID")
        private String sessionId;
    }
}
