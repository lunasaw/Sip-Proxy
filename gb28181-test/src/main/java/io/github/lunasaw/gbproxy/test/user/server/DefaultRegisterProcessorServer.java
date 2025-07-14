package io.github.lunasaw.gbproxy.test.user.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.gbproxy.server.transimit.request.register.RegisterInfo;
import io.github.lunasaw.gbproxy.server.transimit.request.register.RegisterProcessorServer;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.DeviceSupplier;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2023/12/25
 */
@Component
@Slf4j
public class DefaultRegisterProcessorServer implements RegisterProcessorServer {

    public static Map<String, SipTransaction> sipTransactionMap = new ConcurrentHashMap<>();

    @Autowired
    private DeviceSupplier                    deviceSupplier;

    @Override
    public void responseUnauthorized(String userId) {
        log.info("responseUnauthorized::userId = {}", userId);
    }

    @Override
    public SipTransaction getTransaction(String userId) {
        return sipTransactionMap.get(userId);
    }

    @Override
    public void updateRegisterInfo(String userId, RegisterInfo registerInfo) {

        ToDevice instance = ToDevice.getInstance(userId, registerInfo.getRemoteIp(), registerInfo.getRemotePort());
        instance.setTransport(registerInfo.getTransport());
        instance.setLocalIp(registerInfo.getLocalIp());

        deviceSupplier.addOrUpdateDevice(instance);

        log.info("设备注册更新::userId = {}, registerInfo = {}", userId, JSON.toJSONString(registerInfo));
    }

    @Override
    public void updateSipTransaction(String userId, SipTransaction sipTransaction) {
        log.info("设备注册::userId = {}, sipTransaction = {}", userId, sipTransaction);
        sipTransactionMap.put(userId, sipTransaction);
    }

    @Override
    public void deviceOffLine(String userId, RegisterInfo registerInfo, SipTransaction sipTransaction) {
        log.info("设备注销::userId = {}, sipTransaction = {}", userId, sipTransaction);
        deviceSupplier.removeDevice(userId);
        sipTransactionMap.remove(userId);
    }
}
