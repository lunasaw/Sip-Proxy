package io.github.lunasaw.sip.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author luna
 * @date 2023/10/20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceSession {

    String userId;
    String sipId;

    public DeviceSession(String userId, String sipId) {
        this.userId = userId;
        this.sipId = sipId;
    }

    private FromDevice fromDevice;

    private ToDevice toDevice;
}
