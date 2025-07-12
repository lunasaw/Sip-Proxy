package io.github.lunasaw.gbproxy.test.user.client;

import io.github.lunasaw.gbproxy.client.config.ClientProperties;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientSendCmd;
import io.github.lunasaw.gbproxy.client.transmit.response.register.RegisterProcessorHandler;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.DefaultDeviceSupplier;
import io.github.lunasaw.sip.common.service.DeviceSupplier;
import io.github.lunasaw.sip.common.utils.DynamicTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author luna
 * @date 2023/10/17
 */
@Slf4j
@Component
public class DefaultRegisterProcessorHandler implements RegisterProcessorHandler {

    private static final String KEEPALIVE = "keepalive";
    public static Boolean isRegister = true;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private DefaultDeviceSupplier deviceSupplier;

    @Autowired
    private ClientProperties clientProperties;

    @Override
    public Integer getExpire(String userId) {
        return isRegister ? 300 : 0;
    }


    @Override
    public void registerSuccess(String toUserId) {
        // 定时任务 每分钟执行一次
        dynamicTask.startCron(KEEPALIVE,
                () -> {
                    if (!isRegister) {
                        return;
                    }
                    ClientSendCmd.deviceKeepLiveNotify((FromDevice) fromDevice, (ToDevice) deviceSupplier.getDevice(toUserId), "OK",
                            eventResult -> {
                                dynamicTask.stop(KEEPALIVE);
                                // 注册
                                log.error("心跳失败 发起注册 registerSuccess::toUserId = {} ", toUserId);
                                ClientSendCmd.deviceRegister((FromDevice) fromDevice, (ToDevice) deviceSupplier.getDevice(toUserId), 300);
                            });
                }, 60, TimeUnit.SECONDS);

        if (!isRegister) {
            // 注销
            dynamicTask.stop(KEEPALIVE);
        }
    }
}
