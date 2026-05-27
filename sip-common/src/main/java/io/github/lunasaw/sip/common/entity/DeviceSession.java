package io.github.lunasaw.sip.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SIP设备会话，关联发送方与接收方设备信息。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceSession {

    /** 用户ID。 */
    String userId;
    /** SIP设备编号。 */
    String sipId;

    public DeviceSession(String userId, String sipId) {
        this.userId = userId;
        this.sipId = sipId;
    }

    /** 发送方设备信息。 */
    private FromDevice fromDevice;

    /** 接收方设备信息。 */
    private ToDevice toDevice;
}
