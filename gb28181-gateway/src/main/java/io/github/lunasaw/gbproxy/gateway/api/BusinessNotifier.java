package io.github.lunasaw.gbproxy.gateway.api;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;

/**
 * 业务服务器通知接口。
 *
 * <p>sip-gateway 把入站 SIP 事件（注册、INVITE、告警等）转发给上游业务服务器，
 * 通常通过 HTTP/MQ 实现。框架不约束传输方式——业务方按需自行实现一个 Bean
 * 替换默认的 {@link io.github.lunasaw.gbproxy.gateway.notifier.NoopBusinessNotifier}。
 *
 * <p><strong>注意</strong>：实现必须异步（推 MQ / 异步 HTTP），否则会阻塞 SIP 事件线程
 * 导致设备超时重传。建议用 {@code @Async} 或 MQ 投递。
 *
 * @author luna
 */
public interface BusinessNotifier {

    /** 设备注册成功 / 上线时回调 */
    void deviceOnline(String deviceId, RegisterInfo registerInfo);

    /**
     * 收到设备主动 INVITE（语音对讲、ePT 紧急呼叫等场景）。
     *
     * <p>业务方异步处理后通过 {@code transactionContextKey} 取回 RequestEvent 完成回包。
     *
     * @param rawSdp 原始 SDP 文本（UTF-8 解码自 INVITE body）。
     *               <strong>直接转给 ZLM/SRS 推流时用此参数</strong>，避免 {@link GbSessionDescription}
     *               反向序列化丢字段（自定义 a= 行、y=ssrc、f= 视频参数、厂商方言等）
     * @param parsed 已解析的 SDP 模型。<strong>业务侧抠 ssrc / m-line 端口做流匹配时用此参数</strong>，
     *               省去重复 SDP 解析；可能为 null（INVITE 无 body 或解析失败）
     */
    void inviteIncoming(String callId, String fromUserId, String toUserId,
                        String rawSdp,
                        GbSessionDescription parsed,
                        String transactionContextKey);

    /** 设备告警 */
    void alarm(String deviceId, DeviceAlarmNotify notify);
}
