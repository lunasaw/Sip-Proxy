package io.github.lunasaw.sip.common.service;

import javax.sip.RequestEvent;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.utils.SipUtils;

/**
 * SIP用户生成器接口，提供发送方和接收方设备信息的获取能力。
 */
public interface SipUserGenerate {

    /**
     * 接受设备
     *
     * @param userId sip用户id
     * @return ToDevice
     */
    Device getToDevice(String userId);

    /**
     * 发送设备
     *
     * @return FromDevice
     */
    Device getFromDevice();

    /**
     * 设备检查
     * 
     * @param evt
     * @return
     */
    default boolean checkDevice(RequestEvent evt) {
        SIPRequest request = (SIPRequest)evt.getRequest();

        // 在接收端看来 收到请求的时候fromHeader还是服务端的 toHeader才是自己的，这里是要查询自己的信息
        String userId = SipUtils.getUserIdFromToHeader(request);

        // 获取设备
        FromDevice fromDevice = (FromDevice)getFromDevice();

        if (fromDevice == null) {
            return false;
        }
        return userId.equals(fromDevice.getUserId());
    }
}
