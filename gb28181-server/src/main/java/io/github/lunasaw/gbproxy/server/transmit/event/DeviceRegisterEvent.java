package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import lombok.Getter;

@Getter
public class DeviceRegisterEvent extends DeviceEvent {

    private final RegisterInfo registerInfo;

    public DeviceRegisterEvent(Object source, String deviceId, RegisterInfo registerInfo) {
        super(source, deviceId);
        this.registerInfo = registerInfo;
    }
}
