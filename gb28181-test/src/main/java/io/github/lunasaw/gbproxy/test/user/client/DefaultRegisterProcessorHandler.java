package io.github.lunasaw.gbproxy.test.user.client;

import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.client.transmit.response.register.RegisterProcessorHandler;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
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
    private ClientDeviceSupplier deviceSupplier;

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
                    FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
                    Device device = deviceSupplier.getDevice(toUserId);
                    if (device == null) {
                        log.error("心跳失败 设备不存在 toUserId = {} ", toUserId);
                        return;
                    }
                    ToDevice toDevice = deviceSupplier.getToDevice(device);

                    DeviceKeepLiveNotify keepLiveNotify = new DeviceKeepLiveNotify(
                            CmdTypeEnum.KEEPALIVE.getType(),
                            RandomStrUtil.getValidationCode(),
                            clientFromDevice.getUserId()
                    );
                    keepLiveNotify.setStatus("OK");

                    ClientCommandSender.sendCommand("MESSAGE", clientFromDevice, toDevice, eventResult -> {
                        dynamicTask.stop(KEEPALIVE);
                        // 注册
                        log.error("心跳失败 发起注册 registerSuccess::toUserId = {} ", toUserId);

                        ClientCommandSender.sendRegisterCommand(clientFromDevice, toDevice, 300);
                    }, eventResult -> {
                        // 心跳成功
                        log.info("registerSuccess::toUserId = {}", toUserId);
                    }, keepLiveNotify);
                }, 60, TimeUnit.SECONDS);

        if (!isRegister) {
            // 注销
            dynamicTask.stop(KEEPALIVE);
        }


    }
}
