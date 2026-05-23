package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import lombok.Getter;

@Getter
public class DeviceOfflineEvent extends DeviceEvent {

    private final RegisterInfo registerInfo;
    private final SipTransaction sipTransaction;

    public DeviceOfflineEvent(Object source, String deviceId, RegisterInfo registerInfo, SipTransaction sipTransaction) {
        super(source, deviceId);
        this.registerInfo = registerInfo;
        this.sipTransaction = sipTransaction;
    }
}
