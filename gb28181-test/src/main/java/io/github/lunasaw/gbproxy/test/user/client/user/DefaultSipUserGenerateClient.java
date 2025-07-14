package io.github.lunasaw.gbproxy.test.user.client.user;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gbproxy.client.user.SipUserGenerateClient;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.utils.SipUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.service.DeviceSupplier;

import javax.sip.RequestEvent;

/**
 * @author luna
 * @date 2023/12/29
 */
@Component
public class DefaultSipUserGenerateClient implements SipUserGenerateClient {

    @Autowired
    @Qualifier("clientFrom")
    private Device fromDevice;

    @Autowired
    private DeviceSupplier deviceSupplier;

    @Override
    public Device getToDevice(String userId) {
        return deviceSupplier.getDevice(userId);
    }

    @Override
    public Device getFromDevice() {
        return fromDevice;
    }

    @Override
    public boolean checkDevice(RequestEvent evt) {
        SIPRequest request = (SIPRequest)evt.getRequest();

        // 在客户端看来 收到请求的时候fromHeader还是服务端的 toHeader才是自己的，这里是要查询自己的信息
        String userId = SipUtils.getUserIdFromToHeader(request);

        // 获取设备
        FromDevice fromDevice = (FromDevice)getFromDevice();

        if (fromDevice == null) {
            return false;
        }
        return userId.equals(fromDevice.getUserId());
    }
}
