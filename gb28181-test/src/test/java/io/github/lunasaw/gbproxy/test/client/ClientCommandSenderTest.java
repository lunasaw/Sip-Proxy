package io.github.lunasaw.gbproxy.test.client;

import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.*;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.CustomerSipListener;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.ClientCommandStrategy;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.ClientCommandStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.luna.common.text.RandomStrUtil;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClientCommandSender完整测试类 - 使用真实SIP配置
 * 验证所有消息发送方法的功能和兼容性
 * 严格遵循client设备配置规范
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
@SpringBootTest(classes = io.github.lunasaw.gbproxy.test.Gb28181ApplicationTest.class)
@TestPropertySource(properties = {
        // 客户端配置
        "sip.gb28181.client.keepAliveInterval=1m",
        "sip.gb28181.client.maxRetries=3",
        "sip.gb28181.client.retryDelay=5s",
        "sip.gb28181.client.registerExpires=3600",
        "sip.gb28181.client.clientId=34020000001320000001",
        "sip.gb28181.client.clientName=GB28181-Client",
        "sip.gb28181.client.username=admin",
        "sip.gb28181.client.password=123456",

        // 简化测试配置
        "sip.async.enabled=false",
        "sip.pool.enabled=false",
        "sip.cache.enabled=false",

        // Spring配置
        "spring.main.allow-bean-definition-overriding=true",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "logging.level.io.github.lunasaw.sip=DEBUG"
})
class ClientCommandSenderTest {

    // 客户端设备配置 - 严格按照规范使用clientFromDevice和clientToDevice
    private FromDevice clientFromDevice;
    private ToDevice clientToDevice;

    private SipLayer sipLayer;

    @BeforeEach
    void setUp() {
        log.info("=== 初始化ClientCommandSender测试环境 ===");

        // 严格按照规范创建客户端设备配置
        clientFromDevice = FromDevice.getInstance("34020000001320000001", "127.0.0.1", 5060);
        clientToDevice = ToDevice.getInstance("34020000001320000002", "127.0.0.1", 5060);

        // 创建SipLayer实例
        sipLayer = new SipLayer();

        // 初始化监听点，确保测试环境正确设置
        sipLayer.setSipListener(CustomerSipListener.getInstance());
        sipLayer.addListeningPoint("127.0.0.1", 5060);

        log.info("客户端From设备: {}", clientFromDevice.getUserId());
        log.info("客户端To设备: {}", clientToDevice.getUserId());
    }

    // ==================== 策略模式命令发送测试 ====================

    @Test
    void testSendCommandWithStrategy() {
        log.info("=== 测试策略模式命令发送 ===");

        // 测试MESSAGE策略
        String callId = ClientCommandSender.sendCommand("MESSAGE", clientFromDevice, clientToDevice, "test content");
        assertNotNull(callId);
        log.info("MESSAGE命令发送成功，CallId: {}", callId);

        // 测试不存在的策略（应该使用MESSAGE策略）
        String callId2 = ClientCommandSender.sendCommand("UNKNOWN", clientFromDevice, clientToDevice, "test content");
        assertNotNull(callId2);
        log.info("UNKNOWN命令发送成功（使用MESSAGE策略），CallId: {}", callId2);
    }

    @Test
    void testSendCommandWithEvents() {
        log.info("=== 测试带事件的命令发送 ===");

        // 创建事件处理器
        Event errorEvent = eventResult -> log.error("命令发送失败: {}", eventResult);
        Event okEvent = eventResult -> log.info("命令发送成功: {}", eventResult);

        // 测试带事件的命令发送
        String callId = ClientCommandSender.sendCommand("MESSAGE", clientFromDevice, clientToDevice, errorEvent, okEvent, "test content");
        assertNotNull(callId);
        log.info("带事件的MESSAGE命令发送成功，CallId: {}", callId);
    }

    // ==================== 告警相关命令测试 ====================

    @Test
    void testSendAlarmCommandWithDeviceAlarm() {
        log.info("=== 测试发送设备告警命令 ===");

        // 创建告警对象
        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setAlarmMethod("1");
        deviceAlarm.setAlarmPriority("3");

        String callId = ClientCommandSender.sendAlarmCommand(clientFromDevice, clientToDevice, deviceAlarm);
        assertNotNull(callId);
        log.info("设备告警命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendAlarmCommandWithDeviceAlarmNotify() {
        log.info("=== 测试发送设备告警通知命令 ===");

        // 创建告警通知对象
        DeviceAlarmNotify deviceAlarmNotify = new DeviceAlarmNotify(
                CmdTypeEnum.ALARM.getType(),
                RandomStrUtil.getValidationCode(),
                clientFromDevice.getUserId()
        );

        String callId = ClientCommandSender.sendAlarmCommand(clientFromDevice, clientToDevice, deviceAlarmNotify);
        assertNotNull(callId);
        log.info("设备告警通知命令发送成功，CallId: {}", callId);
    }

    // ==================== 心跳相关命令测试 ====================

    @Test
    void testSendKeepaliveCommandWithStatus() {
        log.info("=== 测试发送心跳状态命令 ===");

        String callId = ClientCommandSender.sendKeepaliveCommand(clientFromDevice, clientToDevice, "ONLINE");
        assertNotNull(callId);
        log.info("心跳状态命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendKeepaliveCommandWithDeviceKeepLiveNotify() {
        log.info("=== 测试发送设备心跳通知命令 ===");

        // 创建心跳通知对象
        DeviceKeepLiveNotify keepLiveNotify = new DeviceKeepLiveNotify(
                CmdTypeEnum.KEEPALIVE.getType(),
                RandomStrUtil.getValidationCode(),
                clientFromDevice.getUserId()
        );
        keepLiveNotify.setStatus("ONLINE");

        String callId = ClientCommandSender.sendKeepaliveCommand(clientFromDevice, clientToDevice, keepLiveNotify);
        assertNotNull(callId);
        log.info("设备心跳通知命令发送成功，CallId: {}", callId);
    }

    // ==================== 设备目录相关命令测试 ====================

    @Test
    void testSendCatalogCommandWithDeviceResponse() {
        log.info("=== 测试发送设备响应目录命令 ===");

        // 创建设备响应对象
        DeviceResponse deviceResponse = new DeviceResponse(
                CmdTypeEnum.CATALOG.getType(),
                RandomStrUtil.getValidationCode(),
                clientFromDevice.getUserId()
        );

        String callId = ClientCommandSender.sendCatalogCommand(clientFromDevice, clientToDevice, deviceResponse);
        assertNotNull(callId);
        log.info("设备响应目录命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendCatalogCommandWithDeviceItems() {
        log.info("=== 测试发送设备项列表目录命令 ===");

        // 创建设备列表
        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setDeviceId("34020000001320000001");
        deviceItem.setName("测试设备");
        List<DeviceItem> deviceItems = Arrays.asList(deviceItem);

        String callId = ClientCommandSender.sendCatalogCommand(clientFromDevice, clientToDevice, deviceItems);
        assertNotNull(callId);
        log.info("设备项列表目录命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendCatalogCommandWithSingleDeviceItem() {
        log.info("=== 测试发送单个设备项目录命令 ===");

        // 创建单个设备项
        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setDeviceId("34020000001320000001");
        deviceItem.setName("测试设备");

        String callId = ClientCommandSender.sendCatalogCommand(clientFromDevice, clientToDevice, deviceItem);
        assertNotNull(callId);
        log.info("单个设备项目录命令发送成功，CallId: {}", callId);
    }

    // ==================== 设备信息相关命令测试 ====================

    @Test
    void testSendDeviceInfoCommand() {
        log.info("=== 测试发送设备信息命令 ===");

        // 创建设备信息
        DeviceInfo deviceInfo = new DeviceInfo(
                CmdTypeEnum.DEVICE_INFO.getType(),
                RandomStrUtil.getValidationCode(),
                clientFromDevice.getUserId()
        );

        String callId = ClientCommandSender.sendDeviceInfoCommand(clientFromDevice, clientToDevice, deviceInfo);
        assertNotNull(callId);
        log.info("设备信息命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendDeviceStatusCommandWithString() {
        log.info("=== 测试发送设备状态字符串命令 ===");

        String callId = ClientCommandSender.sendDeviceStatusCommand(clientFromDevice, clientToDevice, "ONLINE");
        assertNotNull(callId);
        log.info("设备状态字符串命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendDeviceStatusCommandWithDeviceStatus() {
        log.info("=== 测试发送设备状态对象命令 ===");

        // 创建设备状态
        DeviceStatus deviceStatus = new DeviceStatus(
                CmdTypeEnum.DEVICE_STATUS.getType(),
                RandomStrUtil.getValidationCode(),
                clientFromDevice.getUserId()
        );
        deviceStatus.setOnline("ONLINE");

        String callId = ClientCommandSender.sendDeviceStatusCommand(clientFromDevice, clientToDevice, deviceStatus);
        assertNotNull(callId);
        log.info("设备状态对象命令发送成功，CallId: {}", callId);
    }

    // ==================== 位置信息相关命令测试 ====================

    @Test
    void testSendMobilePositionCommand() {
        log.info("=== 测试发送移动位置命令 ===");

        // 创建位置通知对象
        MobilePositionNotify mobilePositionNotify = new MobilePositionNotify(
                CmdTypeEnum.MOBILE_POSITION.getType(),
                RandomStrUtil.getValidationCode(),
                clientFromDevice.getUserId()
        );

        // 创建订阅信息
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setExpires(3600);

        String callId = ClientCommandSender.sendMobilePositionCommand(clientFromDevice, clientToDevice, mobilePositionNotify, subscribeInfo);
        assertNotNull(callId);
        log.info("移动位置命令发送成功，CallId: {}", callId);
    }

    // ==================== 设备更新相关命令测试 ====================

    @Test
    void testSendDeviceChannelUpdateCommand() {
        log.info("=== 测试发送设备通道更新命令 ===");

        // 创建设备更新项
        DeviceUpdateItem deviceUpdateItem = new DeviceUpdateItem();
        deviceUpdateItem.setDeviceId("34020000001320000001");
        List<DeviceUpdateItem> deviceItems = Arrays.asList(deviceUpdateItem);

        String callId = ClientCommandSender.sendDeviceChannelUpdateCommand(clientFromDevice, clientToDevice, deviceItems);
        assertNotNull(callId);
        log.info("设备通道更新命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendDeviceOtherUpdateCommand() {
        log.info("=== 测试发送设备其他更新命令 ===");

        // 创建其他更新项
        DeviceOtherUpdateNotify.OtherItem otherItem = new DeviceOtherUpdateNotify.OtherItem();
        otherItem.setDeviceId("34020000001320000001");
        List<DeviceOtherUpdateNotify.OtherItem> deviceItems = Arrays.asList(otherItem);

        String callId = ClientCommandSender.sendDeviceOtherUpdateCommand(clientFromDevice, clientToDevice, deviceItems);
        assertNotNull(callId);
        log.info("设备其他更新命令发送成功，CallId: {}", callId);
    }

    // ==================== 录像相关命令测试 ====================

    @Test
    void testSendDeviceRecordCommandWithDeviceRecord() {
        log.info("=== 测试发送设备录像对象命令 ===");

        // 创建录像响应对象
        DeviceRecord deviceRecord = new DeviceRecord(
                CmdTypeEnum.RECORD_INFO.getType(),
                RandomStrUtil.getValidationCode(),
                clientFromDevice.getUserId()
        );

        String callId = ClientCommandSender.sendDeviceRecordCommand(clientFromDevice, clientToDevice, deviceRecord);
        assertNotNull(callId);
        log.info("设备录像对象命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendDeviceRecordCommandWithRecordItems() {
        log.info("=== 测试发送录像项列表命令 ===");

        // 创建录像文件列表
        DeviceRecord.RecordItem recordItem = new DeviceRecord.RecordItem();
        recordItem.setDeviceId("34020000001320000001");
        List<DeviceRecord.RecordItem> recordItems = Arrays.asList(recordItem);

        String callId = ClientCommandSender.sendDeviceRecordCommand(clientFromDevice, clientToDevice, recordItems);
        assertNotNull(callId);
        log.info("录像项列表命令发送成功，CallId: {}", callId);
    }

    // ==================== 配置相关命令测试 ====================

    @Test
    void testSendDeviceConfigCommandWithConfigResponse() {
        log.info("=== 测试发送设备配置响应命令 ===");

        // 创建配置响应对象
        DeviceConfigResponse deviceConfigResponse = new DeviceConfigResponse(
                CmdTypeEnum.CONFIG_DOWNLOAD.getType(),
                RandomStrUtil.getValidationCode(),
                clientFromDevice.getUserId()
        );

        String callId = ClientCommandSender.sendDeviceConfigCommand(clientFromDevice, clientToDevice, deviceConfigResponse);
        assertNotNull(callId);
        log.info("设备配置响应命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendDeviceConfigCommandWithBasicParam() {
        log.info("=== 测试发送设备基础参数配置命令 ===");

        // 创建基础参数
        DeviceConfigResponse.BasicParam basicParam = new DeviceConfigResponse.BasicParam();
        basicParam.setName("测试设备");

        String callId = ClientCommandSender.sendDeviceConfigCommand(clientFromDevice, clientToDevice, basicParam);
        assertNotNull(callId);
        log.info("设备基础参数配置命令发送成功，CallId: {}", callId);
    }

    // ==================== 媒体状态相关命令测试 ====================

    @Test
    void testSendMediaStatusCommand() {
        log.info("=== 测试发送媒体状态命令 ===");

        String callId = ClientCommandSender.sendMediaStatusCommand(clientFromDevice, clientToDevice, "121");
        assertNotNull(callId);
        log.info("媒体状态命令发送成功，CallId: {}", callId);
    }

    // ==================== 会话控制相关命令测试 ====================

    @Test
    void testSendByeCommand() {
        log.info("=== 测试发送BYE命令 ===");

        String callId = ClientCommandSender.sendByeCommand(clientFromDevice, clientToDevice);
        assertNotNull(callId);
        log.info("BYE命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendAckCommand() {
        log.info("=== 测试发送ACK命令 ===");

        String callId = ClientCommandSender.sendAckCommand(clientFromDevice, clientToDevice);
        assertNotNull(callId);
        log.info("ACK命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendAckCommandWithCallId() {
        log.info("=== 测试发送带CallId的ACK命令 ===");

        String callId = ClientCommandSender.sendAckCommand(clientFromDevice, clientToDevice, "test-call-id");
        assertNotNull(callId);
        log.info("带CallId的ACK命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendAckCommandWithContentAndCallId() {
        log.info("=== 测试发送带内容和CallId的ACK命令 ===");

        String callId = ClientCommandSender.sendAckCommand(clientFromDevice, clientToDevice, "test content", "test-call-id");
        assertNotNull(callId);
        log.info("带内容和CallId的ACK命令发送成功，CallId: {}", callId);
    }

    // ==================== 注册相关命令测试 ====================

    @Test
    void testSendRegisterCommand() {
        log.info("=== 测试发送注册命令 ===");

        String callId = ClientCommandSender.sendRegisterCommand(clientFromDevice, clientToDevice, 3600);
        assertNotNull(callId);
        log.info("注册命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendRegisterCommandWithEvent() {
        log.info("=== 测试发送带事件的注册命令 ===");

        Event errorEvent = eventResult -> log.error("注册失败: {}", eventResult);

        String callId = ClientCommandSender.sendRegisterCommand(clientFromDevice, clientToDevice, 3600, errorEvent);
        assertNotNull(callId);
        log.info("带事件的注册命令发送成功，CallId: {}", callId);
    }

    @Test
    void testSendUnregisterCommand() {
        log.info("=== 测试发送注销命令 ===");

        String callId = ClientCommandSender.sendUnregisterCommand(clientFromDevice, clientToDevice);
        assertNotNull(callId);
        log.info("注销命令发送成功，CallId: {}", callId);
    }

    @Test
    void testRegisterCommandWithInvalidExpires() {
        log.info("=== 测试无效过期时间的注册命令 ===");

        // 测试无效的过期时间
        assertThrows(IllegalArgumentException.class, () ->
                ClientCommandSender.sendRegisterCommand(clientFromDevice, clientToDevice, -1));
        log.info("无效过期时间的注册命令正确抛出异常");
    }

    // ==================== 建造者模式测试 ====================

    @Test
    void testCommandBuilder() {
        log.info("=== 测试命令建造者模式 ===");

        // 测试建造者模式
        ClientCommandSender.CommandBuilder builder = ClientCommandSender.builder();
        assertNotNull(builder);

        // 测试链式调用
        Event errorEvent = eventResult -> log.error("命令失败: {}", eventResult);
        Event okEvent = eventResult -> log.info("命令成功: {}", eventResult);

        ClientCommandSender.CommandBuilder chainedBuilder = builder
                .commandType("MESSAGE")
                .fromDevice(clientFromDevice)
                .toDevice(clientToDevice)
                .errorEvent(errorEvent)
                .okEvent(okEvent)
                .params("test content");

        assertEquals(builder, chainedBuilder);

        // 测试执行
        String callId = builder.execute();
        assertNotNull(callId);
        log.info("建造者模式命令发送成功，CallId: {}", callId);
    }

    @Test
    void testCommandBuilderWithSubscribeInfo() {
        log.info("=== 测试带订阅信息的命令建造者 ===");

        // 创建订阅信息
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setExpires(3600);

        // 测试带有SubscribeInfo的建造者
        String callId = ClientCommandSender.builder()
                .commandType("SUBSCRIBE")
                .fromDevice(clientFromDevice)
                .toDevice(clientToDevice)
                .subscribeInfo(subscribeInfo)
                .params("test content")
                .execute();

        assertNotNull(callId);
        log.info("带订阅信息的建造者命令发送成功，CallId: {}", callId);
    }

    // ==================== 策略工厂测试 ====================

    @Test
    void testStrategyFactory() {
        log.info("=== 测试策略工厂 ===");

        // 测试获取各种策略
        ClientCommandStrategy messageStrategy = ClientCommandStrategyFactory.getMessageStrategy();
        assertNotNull(messageStrategy);
        assertEquals("MESSAGE", messageStrategy.getCommandType());

        ClientCommandStrategy subscribeStrategy = ClientCommandStrategyFactory.getSubscribeStrategy();
        assertNotNull(subscribeStrategy);
        assertEquals("SUBSCRIBE", subscribeStrategy.getCommandType());

        ClientCommandStrategy notifyStrategy = ClientCommandStrategyFactory.getNotifyStrategy();
        assertNotNull(notifyStrategy);
        assertEquals("NOTIFY", notifyStrategy.getCommandType());

        ClientCommandStrategy inviteStrategy = ClientCommandStrategyFactory.getInviteStrategy();
        assertNotNull(inviteStrategy);
        assertEquals("INVITE", inviteStrategy.getCommandType());

        ClientCommandStrategy byeStrategy = ClientCommandStrategyFactory.getByeStrategy();
        assertNotNull(byeStrategy);
        assertEquals("BYE", byeStrategy.getCommandType());

        ClientCommandStrategy ackStrategy = ClientCommandStrategyFactory.getAckStrategy();
        assertNotNull(ackStrategy);
        assertEquals("ACK", ackStrategy.getCommandType());

        ClientCommandStrategy infoStrategy = ClientCommandStrategyFactory.getInfoStrategy();
        assertNotNull(infoStrategy);
        assertEquals("INFO", infoStrategy.getCommandType());

        log.info("所有策略工厂测试通过");
    }

    @Test
    void testStrategyFactoryWithUnsupportedMethod() {
        log.info("=== 测试不支持方法的策略工厂 ===");

        // 测试不支持的方法
        assertThrows(IllegalArgumentException.class, () ->
                ClientCommandStrategyFactory.getStrategy("UNSUPPORTED"));
        log.info("不支持方法的策略工厂正确抛出异常");
    }

    // ==================== 兼容性测试 ====================

    @Test
    void testCompatibilityMethods() {
        log.info("=== 测试兼容性方法 ===");

        // 测试兼容性方法的存在性
        assertDoesNotThrow(() -> {
            ClientCommandSender.class.getMethod("sendCommand", String.class, FromDevice.class, ToDevice.class, Object[].class);
            ClientCommandSender.class.getMethod("sendAlarmCommand", FromDevice.class, ToDevice.class, DeviceAlarm.class);
            ClientCommandSender.class.getMethod("sendKeepaliveCommand", FromDevice.class, ToDevice.class, String.class);
            ClientCommandSender.class.getMethod("sendCatalogCommand", FromDevice.class, ToDevice.class, DeviceResponse.class);
            ClientCommandSender.class.getMethod("sendDeviceInfoCommand", FromDevice.class, ToDevice.class, DeviceInfo.class);
            ClientCommandSender.class.getMethod("sendByeCommand", FromDevice.class, ToDevice.class);
            ClientCommandSender.class.getMethod("sendAckCommand", FromDevice.class, ToDevice.class);
            ClientCommandSender.class.getMethod("sendRegisterCommand", FromDevice.class, ToDevice.class, Integer.class);
            ClientCommandSender.class.getMethod("builder");
        });
        log.info("兼容性方法测试通过");
    }

    // ==================== 参数验证测试 ====================

    @Test
    void testParameterValidation() {
        log.info("=== 测试参数验证 ===");

        // 测试基本参数处理
        assertDoesNotThrow(() -> {
            // 这些方法应该能处理基本的参数验证
            ClientCommandSender.sendCommand("MESSAGE", clientFromDevice, clientToDevice, "test");
            ClientCommandSender.sendByeCommand(clientFromDevice, clientToDevice);
            ClientCommandSender.sendAckCommand(clientFromDevice, clientToDevice);
        });
        log.info("参数验证测试通过");
    }

    @Test
    void testDeviceConfigurationSeparation() {
        log.info("=== 测试设备配置分离 ===");

        // 验证客户端设备配置的正确性
        assertNotNull(clientFromDevice);
        assertNotNull(clientToDevice);
        assertEquals("34020000001320000001", clientFromDevice.getUserId());
        assertEquals("34020000001320000002", clientToDevice.getUserId());
        assertEquals("127.0.0.1", clientFromDevice.getHostAddress());
        assertEquals("127.0.0.1", clientToDevice.getHostAddress());
        assertEquals(5060, clientFromDevice.getPort());
        assertEquals(5060, clientToDevice.getPort());

        log.info("设备配置分离测试通过");
        log.info("客户端From设备: {}@{}:{}", clientFromDevice.getUserId(), clientFromDevice.getHostAddress(), clientFromDevice.getPort());
        log.info("客户端To设备: {}@{}:{}", clientToDevice.getUserId(), clientToDevice.getHostAddress(), clientToDevice.getPort());
    }

    @Test
    void testSipLayerInitialization() {
        log.info("=== 测试SipLayer初始化 ===");

        // 测试SipLayer的初始化
        assertNotNull(sipLayer);
        // 验证监听点设置
        assertDoesNotThrow(() -> {
            sipLayer.addListeningPoint("127.0.0.1", 8117);
        });

        log.info("SipLayer初始化测试通过");
    }

    @Test
    void testCallIdGeneration() {
        log.info("=== 测试CallId生成 ===");

        // 测试CallId生成功能
        String callId1 = SipRequestUtils.getNewCallId();
        String callId2 = SipRequestUtils.getNewCallId();

        assertNotNull(callId1);
        assertNotNull(callId2);
        assertNotEquals(callId1, callId2);

        log.info("CallId生成测试通过: {} != {}", callId1, callId2);
    }
}