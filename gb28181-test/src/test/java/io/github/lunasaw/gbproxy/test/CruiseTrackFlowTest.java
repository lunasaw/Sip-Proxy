package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackListResponse;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceCruiseTrackEvent;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import io.github.lunasaw.gbproxy.test.handler.TestClientEventHandler;
import io.github.lunasaw.gbproxy.test.handler.TestClientRegisterHandler;
import io.github.lunasaw.gbproxy.test.handler.TestServerEventHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 §9.5 / A.2.4.11 / A.2.4.12 / A.2.6.13 / A.2.6.14 巡航轨迹查询集成测试。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class CruiseTrackFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
    @Autowired private TestClientEventHandler clientEventHandler;
    @Autowired private TestServerEventHandler eventHandler;
    @Autowired private SipBusinessConfig sessionCache;
    @Autowired private ClientDeviceSupplier clientDeviceSupplier;

    @Value("${sip.client.clientId}") private String clientId;
    @Value("${sip.server.serverId}") private String serverId;

    private FromDevice fromDevice;
    private ToDevice toDevice;

    @BeforeEach
    void ensureRegistered() throws InterruptedException {
        fromDevice = clientDeviceSupplier.getClientFromDevice();
        toDevice = (ToDevice) clientDeviceSupplier.getDevice(serverId);
        if (sessionCache.getToDevice(clientId) == null) {
            CountDownLatch latch = new CountDownLatch(1);
            registerHandler.reset(latch);
            ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 3600);
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void cruiseTrackListQuery_shouldRoundTrip() throws InterruptedException {
        CountDownLatch clientLatch = new CountDownLatch(1);
        clientEventHandler.reset(clientLatch);
        CountDownLatch serverLatch = new CountDownLatch(1);
        eventHandler.reset(serverLatch);

        commandSender.deviceCruiseTrackListQuery(clientId);

        assertThat(clientLatch.await(5, TimeUnit.SECONDS)).as("客户端应收到 CruiseTrackListQuery").isTrue();
        assertThat(clientEventHandler.getLastCruiseTrackListQuery()).isNotNull();

        assertThat(serverLatch.await(5, TimeUnit.SECONDS)).as("服务端应收到巡航轨迹列表应答").isTrue();
        assertThat(eventHandler.getLastCruiseTrack().getType()).isEqualTo(DeviceCruiseTrackEvent.Type.LIST);
        CruiseTrackListResponse list = eventHandler.getLastCruiseTrack().getListResponse();
        assertThat(list.getSumNum()).isEqualTo(2);
        assertThat(list.getCruiseTrackList().getTracks()).hasSize(2);
        assertThat(list.getCruiseTrackList().getTracks().get(0).getName()).isEqualTo("Track-A");
    }

    @Test
    void cruiseTrackQuery_shouldRoundTrip() throws InterruptedException {
        CountDownLatch clientLatch = new CountDownLatch(1);
        clientEventHandler.reset(clientLatch);
        CountDownLatch serverLatch = new CountDownLatch(1);
        eventHandler.reset(serverLatch);

        commandSender.deviceCruiseTrackQuery(clientId, 0);

        assertThat(clientLatch.await(5, TimeUnit.SECONDS)).as("客户端应收到 CruiseTrackQuery").isTrue();
        assertThat(clientEventHandler.getLastCruiseTrackQuery().getQuery().getNumber()).isEqualTo(0);

        assertThat(serverLatch.await(5, TimeUnit.SECONDS)).as("服务端应收到巡航轨迹应答").isTrue();
        assertThat(eventHandler.getLastCruiseTrack().getType()).isEqualTo(DeviceCruiseTrackEvent.Type.SINGLE);
        CruiseTrackResponse single = eventHandler.getLastCruiseTrack().getTrackResponse();
        assertThat(single.getNumber()).isEqualTo(0);
        assertThat(single.getCruisePointList().getPoints()).hasSize(2);
    }
}
