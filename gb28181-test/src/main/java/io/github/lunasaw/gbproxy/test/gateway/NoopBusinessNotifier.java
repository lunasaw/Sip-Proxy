package io.github.lunasaw.gbproxy.test.gateway;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认 {@link BusinessNotifier}：仅打印日志，便于演示与本地开发。
 *
 * <p>生产环境覆盖此 Bean 改为 HTTP/MQ 推送即可。
 */
@Slf4j
public class NoopBusinessNotifier implements BusinessNotifier {

    @Override
    public void deviceOnline(String deviceId, RegisterInfo registerInfo) {
        log.info("[business] device online: {}", deviceId);
    }

    @Override
    public void inviteIncoming(String callId, String fromUserId, String toUserId,
                               GbSessionDescription sessionDescription, String transactionContextKey) {
        log.info("[business] invite incoming: callId={}, from={}, to={}", callId, fromUserId, toUserId);
    }

    @Override
    public void alarm(String deviceId, DeviceAlarmNotify notify) {
        log.warn("[business] alarm: deviceId={}", deviceId);
    }
}
