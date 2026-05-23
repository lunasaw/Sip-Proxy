package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.sip.common.entity.SipTransaction;
import lombok.Getter;

@Getter
public class DeviceOnlineEvent extends DeviceEvent {

    private final SipTransaction sipTransaction;

    public DeviceOnlineEvent(Object source, String deviceId, SipTransaction sipTransaction) {
        super(source, deviceId);
        this.sipTransaction = sipTransaction;
    }
}
