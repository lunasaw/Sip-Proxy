package io.github.lunasaw.gbproxy.gateway.notifier;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.gateway.api.BusinessNotifier;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认 {@link BusinessNotifier}：仅打印日志，便于演示与本地开发。
 *
 * <p>生产环境必须覆盖此 Bean 改为 HTTP/MQ 推送。构造时 {@code log.warn} 提示不要上线。
 *
 * @author luna
 */
@Slf4j
public class NoopBusinessNotifier implements BusinessNotifier {

    public NoopBusinessNotifier() {
        log.warn("NoopBusinessNotifier active — replace with a real HTTP/MQ implementation before production");
    }

    @Override
    public void deviceOnline(String deviceId, RegisterInfo registerInfo) {
        log.info("[business] device online: {}", deviceId);
    }

    @Override
    public void inviteIncoming(String callId, String fromUserId, String toUserId,
                               String rawSdp,
                               GbSessionDescription parsed,
                               String transactionContextKey) {
        log.info("[business] invite incoming: callId={}, from={}, to={}", callId, fromUserId, toUserId);
    }

    @Override
    public void alarm(String deviceId, DeviceAlarmNotify notify) {
        log.warn("[business] alarm: deviceId={}", deviceId);
    }
}
