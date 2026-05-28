package io.github.lunasaw.gb28181.common.entity.notify;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 A.2.5 d）语音广播通知
 * <p>
 * 语音广播功能实现用户通过语音输入设备向前端语音输出设备的语音广播。
 * </p>
 * <pre>
 * <Notify>
 *   <CmdType>Broadcast</CmdType>
 *   <SN>992</SN>
 *   <SourceID>31010400001360000001</SourceID>
 *   <TargetID>31010403001370002272</TargetID>
 * </Notify>
 * </pre>
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Notify")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceBroadcastNotify extends DeviceBase {
    
    /**
     * 语音输入设备的设备编码（必选）
     */
    @XmlElement(name = "SourceID")
    private String sourceId;

    /**
     * 语音输出设备的设备编码（必选）
     */
    @XmlElement(name = "TargetID")
    private String targetId;

    public DeviceBroadcastNotify(String cmdType, String sn, String deviceId, String sourceId, String targetId) {
        super(cmdType, sn, deviceId);
        this.sourceId = sourceId;
        this.targetId = targetId;
    }
    
    /**
     * 兼容性构造函数
     * @deprecated 请使用完整的构造函数
     */
    @Deprecated
    public DeviceBroadcastNotify(String type, String fromUserId, String toUserId) {
        super(type, null, null);
        this.sourceId = fromUserId;
        this.targetId = toUserId;
    }
    
    /**
     * 兼容性构造函数
     * @deprecated 请使用完整的构造函数
     */
    @Deprecated
    public DeviceBroadcastNotify(String cmdType, String sourceId, String targetId, String deviceId) {
        super(cmdType, null, deviceId);
        this.sourceId = sourceId;
        this.targetId = targetId;
    }
}
