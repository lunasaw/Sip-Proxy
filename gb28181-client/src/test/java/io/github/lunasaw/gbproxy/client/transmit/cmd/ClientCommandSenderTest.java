package io.github.lunasaw.gbproxy.client.transmit.cmd;

import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import com.luna.common.text.RandomStrUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ClientCommandSender测试类
 * 测试非SIP基础协议命令使用MESSAGE策略的功能
 *
 * @author luna
 * @date 2024/01/01
 */
public class ClientCommandSenderTest {

    private FromDevice fromDevice;
    private ToDevice toDevice;

    @BeforeEach
    void setUp() {
        fromDevice = new FromDevice();
        fromDevice.setUserId("testDevice");
        fromDevice.setHostAddress("127.0.0.1");
        fromDevice.setPort(5060);

        toDevice = new ToDevice();
        toDevice.setUserId("testServer");
        toDevice.setHostAddress("127.0.0.1");
        toDevice.setPort(5060);
    }

    @Test
    void testSendMediaStatusCommand() {
        // 测试发送媒体状态命令
        String notifyType = "121";
        String callId = ClientCommandSender.sendMediaStatusCommand(fromDevice, toDevice, notifyType);

        assertNotNull(callId);
        assertFalse(callId.isEmpty());
    }

    @Test
    void testSendKeepaliveCommand() {
        // 测试发送心跳命令
        String status = "ONLINE";
        String callId = ClientCommandSender.sendKeepaliveCommand(fromDevice, toDevice, status);

        assertNotNull(callId);
        assertFalse(callId.isEmpty());
    }

    @Test
    void testSendDeviceStatusCommand() {
        // 测试发送设备状态命令
        String online = "ONLINE";
        String callId = ClientCommandSender.sendDeviceStatusCommand(fromDevice, toDevice, online);

        assertNotNull(callId);
        assertFalse(callId.isEmpty());
    }

    @Test
    void testSendCommandWithNonSipProtocol() {
        // 测试发送非SIP基础协议命令，应该使用MESSAGE策略
        MediaStatusNotify mediaStatusNotify = new MediaStatusNotify(
                CmdTypeEnum.MEDIA_STATUS.getType(),
                RandomStrUtil.getValidationCode(),
                fromDevice.getUserId()
        );
        mediaStatusNotify.setNotifyType("121");

        String callId = ClientCommandSender.sendCommand("MediaStatus", fromDevice, toDevice, mediaStatusNotify);

        assertNotNull(callId);
        assertFalse(callId.isEmpty());
    }

    @Test
    void testSendCommandWithSipProtocol() {
        // 测试发送SIP基础协议命令，应该使用对应的策略
        String content = "test message content";
        String callId = ClientCommandSender.sendCommand("MESSAGE", fromDevice, toDevice, content);

        assertNotNull(callId);
        assertFalse(callId.isEmpty());
    }
}