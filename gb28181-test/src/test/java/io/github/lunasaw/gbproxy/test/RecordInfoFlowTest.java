package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
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
 * GB28181-2022 §A.2.4.5 / §A.2.6.7 录像信息查询 + 应答端到端集成测试。
 *
 * <p>流程：server (平台) → MESSAGE(Query, cmdType=RecordInfo) → client (设备) →
 * QueryListener.onRecordInfoQuery 返回 mock DeviceRecord → server 收到 MESSAGE(Response, cmdType=RecordInfo) →
 * onRecordInfoResponse listener。覆盖矩阵 §A.2.4.5 + §A.2.6.7 两行。
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class RecordInfoFlowTest {

    @Autowired private ServerCommandSender commandSender;
    @Autowired private TestClientRegisterHandler registerHandler;
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

    /** §A.2.4.5 + §A.2.6.7：发送 RecordInfo 查询 → 设备应答 → 平台 onRecordInfoResponse 接收。 */
    @Test
    void recordInfoQuery_shouldReceiveResponse() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        commandSender.deviceRecordInfoQuery(clientId, "2024-01-01T00:00:00", "2024-01-01T23:59:59");

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("RecordInfo Query → Response 链路应 5 秒内完成")
            .isTrue();
        DeviceRecord received = eventHandler.getLastRecord();
        assertThat(received).isNotNull();
        assertThat(received.getDeviceId()).isEqualTo(clientId);
        assertThat(received.getCmdType()).isEqualTo(CmdTypeEnum.RECORD_INFO.getType());
        assertThat(received.getRecordList()).isNotEmpty();
        assertThat(received.getRecordList().get(0).getStartTime()).isNotBlank();
    }

    /** Unix 时间戳重载也应正常工作。 */
    @Test
    void recordInfoQuery_byUnixTimestamp_shouldReceiveResponse() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.reset(latch);

        long start = 1704067200L;       // 2024-01-01T00:00:00Z
        long end = start + 86400L;
        commandSender.deviceRecordInfoQuery(clientId, start, end);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        DeviceRecord received = eventHandler.getLastRecord();
        assertThat(received).isNotNull();
        assertThat(received.getDeviceId()).isEqualTo(clientId);
    }
}
