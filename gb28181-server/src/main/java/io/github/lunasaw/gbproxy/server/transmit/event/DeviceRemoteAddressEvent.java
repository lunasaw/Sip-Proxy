package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import lombok.Getter;

@Getter
public class DeviceRemoteAddressEvent extends DeviceEvent {

    private final RemoteAddressInfo remoteAddressInfo;

    public DeviceRemoteAddressEvent(Object source, String deviceId, RemoteAddressInfo remoteAddressInfo) {
        super(source, deviceId);
        this.remoteAddressInfo = remoteAddressInfo;
    }
}
