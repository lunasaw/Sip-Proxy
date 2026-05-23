package io.github.lunasaw.gbproxy.server.transmit.event;

import lombok.Getter;

@Getter
public class DeviceRegisterChallengeEvent extends DeviceEvent {

    public DeviceRegisterChallengeEvent(Object source, String deviceId) {
        super(source, deviceId);
    }
}
