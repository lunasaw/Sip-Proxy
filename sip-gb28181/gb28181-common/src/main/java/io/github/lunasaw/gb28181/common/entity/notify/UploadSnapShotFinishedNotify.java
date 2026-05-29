package io.github.lunasaw.gb28181.common.entity.notify;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
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
 * GB28181-2022 A.2.5.7 图像抓拍传输完成通知
 *
 * <pre>
 * &lt;Notify&gt;
 *   &lt;CmdType&gt;UploadSnapShotFinished&lt;/CmdType&gt;
 *   &lt;SN&gt;123&lt;/SN&gt;
 *   &lt;DeviceID&gt;34020000001320000001&lt;/DeviceID&gt;
 *   &lt;SessionID&gt;32-128 chars&lt;/SessionID&gt;
 *   &lt;SnapShotList&gt;
 *     &lt;SnapShotFileID&gt;file-id-1&lt;/SnapShotFileID&gt;
 *     &lt;SnapShotFileID&gt;file-id-2&lt;/SnapShotFileID&gt;
 *   &lt;/SnapShotList&gt;
 * &lt;/Notify&gt;
 * </pre>
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Notify")
@XmlAccessorType(XmlAccessType.FIELD)
public class UploadSnapShotFinishedNotify extends XmlBean {

    @XmlElement(name = "CmdType")
    private String cmdType = CmdTypeEnum.UPLOAD_SNAP_SHOT_FINISHED.getType();

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    @XmlElement(name = "SessionID")
    private String sessionId;

    @XmlElementWrapper(name = "SnapShotList")
    @XmlElement(name = "SnapShotFileID")
    private List<String> snapShotFileIds;

    public UploadSnapShotFinishedNotify(String sn, String deviceId) {
        this.sn = sn;
        this.deviceId = deviceId;
    }
}
