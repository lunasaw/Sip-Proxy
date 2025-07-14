package io.github.lunasaw.gbproxy.test.user.server.user;

import io.github.lunasaw.gbproxy.test.config.TestDeviceSupplier;
import io.github.lunasaw.sip.common.entity.FromDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.service.DeviceSupplier;

/**
 * @author luna
 * @date 2023/12/29
 */
@Component
public class DefaultSipUserGenerateServer implements SipUserGenerateServer {

    @Autowired
    private TestDeviceSupplier deviceSupplier;

    @Override
    public Device getToDevice(String userId) {
        Device device = deviceSupplier.getDevice(userId);
        if (device instanceof FromDevice) {
            return device;
        }
        return deviceSupplier.createToDevice(device);
    }

    @Override
    public Device getFromDevice() {
        return deviceSupplier.getServerFromDevice();
    }
}
