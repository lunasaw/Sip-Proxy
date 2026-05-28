package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.server.api.DeviceLifecycleListener;
import io.github.lunasaw.gbproxy.server.api.DeviceNotifyListener;
import io.github.lunasaw.gbproxy.server.api.DeviceResponseListener;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务事件监听示例：实现 listener 接口选择关心的方法（v1.5.x：取代 v1.4.0 的 @EventListener 散点形式）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SipEventHandler implements DeviceResponseListener, DeviceNotifyListener, DeviceLifecycleListener {

    private final ServerCommandSender commandSender;
    private final SipBusinessConfig sessionCache;

    @Override
    public void onKeepalive(String deviceId, DeviceKeepLiveNotify notify) {
        log.info("keepalive: {}", deviceId);
    }

    @Override
    public void onRemoteAddressChanged(String deviceId, RemoteAddressInfo remoteAddressInfo) {
        sessionCache.register(deviceId, remoteAddressInfo.getIp(), remoteAddressInfo.getPort(), "UDP");
    }

    @Override
    public void onCatalogResponse(String deviceId, String sn, DeviceResponse catalog) {
        log.info("catalog response: deviceId={}, sn={}, channels={}",
            deviceId, sn,
            catalog.getDeviceItemList() == null ? 0 : catalog.getDeviceItemList().size());
    }

    @Override
    public void onRecordInfoResponse(String deviceId, String sn, DeviceRecord record) {
        log.info("record response: deviceId={}, sn={}", deviceId, sn);
    }

    @Override
    public void onAlarmNotify(String deviceId, DeviceAlarmNotify notify) {
        log.warn("alarm: deviceId={}", deviceId);
    }

    /** 示例：收到目录响应后主动查询设备信息 */
    public void queryCatalog(String deviceId) {
        commandSender.deviceCatalogQuery(deviceId);
    }
}
