package io.github.lunasaw.gbproxy.test.gateway;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;

/**
 * 业务服务器通知接口。
 *
 * <p>sip-gateway 把入站 SIP 事件（注册、INVITE、告警等）转发给上游业务服务器，
 * 通常通过 HTTP/MQ 实现。框架不约束传输方式——业务方按需自行实现一个 Bean
 * 替换默认的 {@link NoopBusinessNotifier}。
 *
 * <p>注意：转发应该是异步的（推 MQ / 异步 HTTP），否则会阻塞 SIP 事件线程。
 *
 * <p>v1.5.x：参数从 {@code DeviceRegisterEvent} / {@code ServerInviteEvent} 等具体事件类
 * 改为 typed payload，避免业务侧依赖 sip-proxy 的 ApplicationEvent 子类。
 */
public interface BusinessNotifier {

    /** 设备注册成功 / 上线时回调 */
    void deviceOnline(String deviceId, RegisterInfo registerInfo);

    /** 收到设备主动 INVITE（语音对讲、ePT 紧急呼叫等场景） */
    void inviteIncoming(String callId, String fromUserId, String toUserId,
                        GbSessionDescription sessionDescription, String transactionContextKey);

    /** 设备告警 */
    void alarm(String deviceId, DeviceAlarmNotify notify);
}
