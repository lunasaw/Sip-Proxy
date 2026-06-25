package io.github.lunasaw.gb28181.common.sdp;

import io.github.lunasaw.gb28181.common.entity.enums.InviteSessionNameEnum;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gb28181.common.entity.sdp.TransportEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sdp.Connection;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sdp.SessionName;
import java.util.Vector;

/**
 * 把 RFC 4566 标准字段在 GB28181 语境下的取值翻译成业务枚举/便利字段。
 * <p>
 * 与 {@link GbSdpExtensionParser} 的区别：本类处理"标准字段 + GB 取值约束"
 * （如 {@code s=Play} → {@link InviteSessionNameEnum#PLAY}），不剥行也不写新行，
 * 只读 {@link SessionDescription} 的标准字段并填回 {@link GbSessionDescription}。
 *
 * @author luna
 * @since 1.6.0
 */
@Slf4j
@Component
public class Gb28181SemanticInterpreter {

    /**
     * 解读 {@code target.baseSdb} 中的标准字段，把 GB 语义结果填回 {@code target}。
     * <p>
     * 单字段解读异常仅记 WARN，不影响其他字段；{@code baseSdb} 为 {@code null} 时直接返回。
     */
    public void interpret(GbSessionDescription target) {
        SessionDescription sd = target.getBaseSdb();
        if (sd == null) {
            return;
        }
        interpretSessionType(sd, target);
        interpretMedia(sd, target);
        interpretConnection(sd, target);
    }

    private void interpretSessionType(SessionDescription sd, GbSessionDescription target) {
        try {
            SessionName sn = sd.getSessionName();
            if (sn == null) {
                return;
            }
            String value = sn.getValue();
            for (InviteSessionNameEnum candidate : InviteSessionNameEnum.values()) {
                if (candidate.getType().equalsIgnoreCase(value)) {
                    target.setSessionType(candidate);
                    return;
                }
            }
        } catch (SdpParseException e) {
            log.warn("interpret s= failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void interpretMedia(SessionDescription sd, GbSessionDescription target) {
        try {
            Vector<MediaDescription> mds = sd.getMediaDescriptions(false);
            if (mds == null || mds.isEmpty()) {
                return;
            }
            MediaDescription md = mds.get(0);
            Media m = md.getMedia();
            target.setTransport(TransportEnum.fromProtoToken(m.getProtocol()));
            target.setPort(m.getMediaPort());
            // a=setup: active|passive (RFC 4145, GB28181 TCP 模式)
            String setupVal = md.getAttribute("setup");
            if (setupVal != null) {
                target.setTcpSetup(setupVal);
            }
        } catch (Exception e) {
            log.warn("interpret m= failed", e);
        }
    }

    private void interpretConnection(SessionDescription sd, GbSessionDescription target) {
        try {
            Connection c = sd.getConnection();
            if (c != null) {
                target.setAddress(c.getAddress());
            }
        } catch (SdpParseException e) {
            log.warn("interpret c= failed", e);
        }
    }
}
