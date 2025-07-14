package io.github.lunasaw.gbproxy.test.user.client;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.gbproxy.client.transmit.request.invite.InviteProcessorClient;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.GbSessionDescription;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import io.github.lunasaw.gb28181.common.entity.enums.InviteSessionNameEnum;
import io.github.lunasaw.sip.common.transmit.event.SipSubscribe;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import javax.sdp.SessionDescription;
import javax.sdp.SessionName;
import java.io.FileNotFoundException;

/**
 * @author luna
 * @date 2023/11/7
 */
@Slf4j
@Component
public class DefaultInviteProcessorClient implements InviteProcessorClient {

    private static String VIDEO_FILE;
    private static String RECORD_VIDEO_FILE;

    static {
        try {
            VIDEO_FILE = ResourceUtils.getFile("classpath:file/invite.mp4").getAbsolutePath();
            RECORD_VIDEO_FILE = ResourceUtils.getFile("classpath:file/record.mp4").getAbsolutePath();
        } catch (FileNotFoundException e) {

        }
    }

    @Autowired
    @Qualifier("clientFrom")
    private Device fromDevice;
    @Autowired
    private FfmpegCommander ffmpegCommander;

    @Override
    @SneakyThrows
    public void inviteSession(String callId, SdpSessionDescription sessionDescription) {
        GbSessionDescription gbSessionDescription = (GbSessionDescription) sessionDescription;
        SessionDescription descriptionBaseSdb = gbSessionDescription.getBaseSdb();
        SessionName sessionName = descriptionBaseSdb.getSessionName();

        if (InviteSessionNameEnum.PLAY.getType().equals(sessionName.getValue())) {
            log.info("点播请求 inviteSession::sessionDescription = {}", JSON.toJSONString(sessionDescription));

            SipSubscribe.addOkSubscribe(callId, eventResult -> {
                ffmpegCommander.closeAllStream();
                ffmpegCommander.pushStream(eventResult.callId, VIDEO_FILE, gbSessionDescription.getAddress(), gbSessionDescription.getPort());
            });
        } else if (InviteSessionNameEnum.PLAY_BACK.getType().equals(sessionName.getValue())) {

            log.info("回放请求 inviteSession::sessionDescription = {}", JSON.toJSONString(sessionDescription));

            SipSubscribe.addOkSubscribe(callId, eventResult -> {
                ffmpegCommander.closeAllStream();
                ffmpegCommander.pushStream(eventResult.callId, RECORD_VIDEO_FILE, gbSessionDescription.getAddress(), gbSessionDescription.getPort());
            });
        }
    }
}
