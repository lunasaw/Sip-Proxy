# GB28181 集成测试补全技术方案

> 版本：1.0 | 日期：2026-05-24 | 基于协议：GB/T 28181—2016

---

## 一、现状分析

### 1.1 协议功能覆盖情况

| 章节 | 功能 | 代码实现 | 集成测试 | 优先级 |
|------|------|---------|---------|--------|
| 9.1 | 注册/注销 | ✅ | ✅ `RegistrationFlowTest` | — |
| 9.2 | 实时视音频点播（INVITE/ACK/BYE） | ✅ | ❌ | P0 |
| 9.3 | 设备控制（PTZ/录像/重启/布防） | ✅ | ❌ | P0 |
| 9.4 | 报警事件通知和分发 | ✅ | ❌ | P0 |
| 9.5 | 设备信息查询（目录/设备信息/状态/预置位） | ✅ | ❌ | P0 |
| 9.6 | 状态信息报送（心跳） | ✅ | ❌ | P1 |
| 9.7 | 历史视音频文件检索（RecordInfo） | ✅ | ❌ | P1 |
| 9.8 | 历史视音频回放（INVITE Playback + INFO控制） | ✅ | ❌ | P1 |
| 9.9 | 视音频文件下载 | ✅ | ❌ | P2 |
| 9.10 | 网络校时 | ⚠️ 部分 | ❌ | P2 |
| 9.11 | 订阅和通知（目录订阅/移动位置订阅） | ✅ | ❌ | P1 |
| 9.12 | 语音广播和语音对讲 | ⚠️ 部分 | ❌ | P2 |

**当前测试覆盖率：约 8%（仅注册/注销）**

### 1.2 未实现功能

经对比协议文档与代码，以下功能**代码层面缺失**：

| 功能 | 缺失内容 | 协议章节 |
|------|---------|---------|
| 网络校时 | 服务端主动下发校时 MESSAGE 的发送方法 | 9.10.1 |
| 语音对讲 | 双向音频 INVITE 流程（客户端主动发起） | 9.12.2 |
| 多响应消息传输 | 分页查询的 `SumNum`/`TotalNum` 处理 | 附录 N |
| 域间目录订阅 | 跨域 SUBSCRIBE/NOTIFY 流程 | 9.11.4 |

---

## 二、测试架构设计

### 2.1 测试模式

现有 `RegistrationFlowTest` 采用**真实 SIP 栈**（client 5061 ↔ server 5060 本机通信），这是最高保真度的验证方式。所有新增集成测试沿用此模式：

```
TestApplication（@EnableSipClient + @EnableSipServer）
    ├── SipBootstrap：初始化���听点
    ├── SipBusinessConfig：内存 DeviceSessionCache
    ├── TestClientDeviceSupplier / TestServerDeviceSupplier
    └── 各 Test*Handler：@EventListener 捕获业务事件
```

### 2.2 测试辅助类规划

新增以下共享辅助类（放在 `src/main/java` 供所有测试复用）：

```
handler/
  TestInviteHandler.java        — 监听 DeviceInviteOkEvent / DeviceInviteFailureEvent
  TestQueryHandler.java         — 监听 DeviceCatalogEvent / DeviceInfoEvent / DeviceStatusEvent
  TestAlarmHandler.java         — 监听 DeviceAlarmEvent
  TestKeepaliveHandler.java     — 监听 DeviceKeepaliveEvent
  TestRecordHandler.java        — 监听 DeviceRecordEvent
  TestSubscribeHandler.java     — 监听 DeviceSubscribeResponseEvent / DeviceNotifyUpdateEvent
```

每个 Handler 统一模式：
```java
@Component
public class TestXxxHandler {
    private volatile CountDownLatch latch;
    private volatile XxxEvent lastEvent;

    public void reset(CountDownLatch latch) { this.latch = latch; this.lastEvent = null; }
    public XxxEvent getLastEvent() { return lastEvent; }

    @EventListener
    public void on(XxxEvent e) { lastEvent = e; if (latch != null) latch.countDown(); }
}
```

---

## 三、新增集成测试详细设计

### 3.1 设备信息查询流程（P0）

**文件**：`DeviceQueryFlowTest.java`

**覆盖协议**：§9.5 设备信息查询

```
IT-QUERY-01 目录查询（Catalog）
  流程：server.deviceCatalogQuery(clientId)
        → client 收到 MESSAGE(Catalog)
        → client 回复 MESSAGE(CatalogResponse)
        → server 触发 DeviceCatalogEvent
  断言：event.getCatalog().getDeviceItemList() 不为空
        event.getDeviceId() == clientId

IT-QUERY-02 设备信息查询（DeviceInfo）
  流程：server.deviceInfoQuery(clientId)
        → client 收到 MESSAGE(DeviceInfo)
        → client 回复 MESSAGE(DeviceInfoResponse)
        → server 触发 DeviceInfoEvent
  断言：event.getDeviceInfo().getDeviceID() == clientId

IT-QUERY-03 设备状态查询（DeviceStatus）
  流程：server.deviceStatusQuery(clientId)
        → client 收到 MESSAGE(DeviceStatus)
        → client 回复 MESSAGE(DeviceStatusResponse)
        → server 触发 DeviceStatusEvent
  断言：event.getDeviceStatus().getOnline() == "ONLINE"

IT-QUERY-04 预置位查询（PresetQuery）
  流程：server.devicePresetQuery(clientId)
        → client 收到 MESSAGE(PresetQuery)
        → client 回复 MESSAGE(PresetQueryResponse)
        → server 触发对应事件
```

**实现要点**：
- 客户端侧需实现 `MessageRequestHandler`（`TestMessageRequestHandler`），在收到查询时返回测试数据
- 查询→响应是异步的，用 `CountDownLatch(1)` + 5s 超时等待服务端事件

### 3.2 实时视音频点播流程（P0）

**文件**：`InvitePlayFlowTest.java`

**覆盖协议**：§9.2 实时视音频点播（第三方呼叫控制模式）

```
IT-INVITE-01 实时点播建立（INVITE → 200 OK → ACK）
  流程：server.deviceInvitePlay(clientId, "127.0.0.1", 10000, UDP)
        → client 收到 INVITE（含 SDP）
        → client 回复 200 OK（含 SDP）
        → server 触发 DeviceInviteOkEvent
        → server 发送 ACK
  断言：event.getCallId() 不为空
        SDP 中 s 字段 == "Play"

IT-INVITE-02 点播结束（BYE）
  前置：IT-INVITE-01 完成
  流程：server.deviceBye(clientId, callId)
        → client 收到 BYE
        → client 触发 ClientByeEvent
  断言：ClientByeEvent.getCallId() == callId

IT-INVITE-03 点播失败（4xx 响应）
  流程：server.deviceInvitePlay(clientId, ...)
        → client 回复 488 Not Acceptable Here
        → server 触发 DeviceInviteFailureEvent
  断言：event.getStatusCode() == 488
```

**实现要点**：
- 客户端 `InviteRequestHandler` 需实现 `TestInviteRequestHandler`，收到 INVITE 后自动回复 200 OK + SDP
- SDP 格式参考协议附录 F，s 字段必须为 "Play"

### 3.3 设备控制流程（P0）

**文件**：`DeviceControlFlowTest.java`

**覆盖协议**：§9.3 设备控制

```
IT-CTRL-01 PTZ 云台控制
  流程：server.deviceControlPtzCmd(clientId, PtzCmdEnum.UP, 50)
        → client 收到 MESSAGE(DeviceControl/PTZ)
        → client 触发 DeviceControlRequestHandler
  断言：handler 收到的 ptzCmd 不为空

IT-CTRL-02 设备重启
  流程：server.deviceControlReboot(clientId)
        → client 收到 MESSAGE(DeviceControl/TeleBoot)
  断言：handler 收到 TeleBoot 类型控制命令

IT-CTRL-03 录像控制（开始/停止）
  流程：server.deviceControlRecord(clientId, "Record")
        → client 收到 MESSAGE(DeviceControl/RecordCmd)
  断言：handler 收到 recordCmd == "Record"

IT-CTRL-04 布防/撤防
  流程：server.deviceControlGuardCmd(clientId, "SetGuard")
        → client 收到 MESSAGE(DeviceControl/GuardCmd)
  断言：handler 收到 guardCmd == "SetGuard"
```

**实现要点**：
- 客户端 `DeviceControlRequestHandler` 需实现 `TestDeviceControlHandler`，记录收到的控制命令
- 控制命令是单向的（server→client），无需等待响应，用 `Thread.sleep(200)` 或 `CountDownLatch` 等待处理完成

### 3.4 报警事件通知流程（P0）

**文件**：`AlarmFlowTest.java`

**覆盖协议**：§9.4 报警事件通知和分发

```
IT-ALARM-01 设备主动上报报警
  流程：ClientCommandSender.sendAlarmCommand(clientDevice, serverDevice, alarm)
        → server 收到 MESSAGE(Alarm)
        → server 触发 DeviceAlarmEvent
  断言：event.getDeviceId() == clientId
        event.getAlarm().getAlarmMethod() 不为空

IT-ALARM-02 服务端查询历史报警
  流程：server.deviceAlarmQuery(clientId, startTime, endTime, ...)
        → client 收到 MESSAGE(Alarm 查询)
        → client 回复 MESSAGE(AlarmResponse)
  断言：client 侧 AlarmQueryMessageClientHandler 被调用
```

### 3.5 心跳保活流程（P1）

**文件**：`KeepaliveFlowTest.java`

**覆盖协议**：§9.6 状态信息报送

```
IT-KA-01 设备心跳上报
  流程：ClientCommandSender.sendKeepaliveCommand(clientDevice, serverDevice, "OK")
        → server 收到 MESSAGE(Keepalive)
        → server 触发 DeviceKeepaliveEvent
  断言：event.getDeviceId() == clientId

IT-KA-02 心跳超时设备离线
  流程：注册成功后，不发送心跳，等待 server 超时检测
  断言：DeviceOfflineEvent 被触发（需 server 侧实现超时检测，当前框架可能未实现，标记为 TODO）
```

### 3.6 历史录像检索流程（P1）

**文件**：`RecordQueryFlowTest.java`

**覆盖协议**：§9.7 设备视音频文件检索

```
IT-RECORD-01 录像文件查询
  流程：server.deviceRecordInfoQuery(clientId, startTime, endTime)
        → client 收到 MESSAGE(RecordInfo 查询)
        → client 回复 MESSAGE(RecordInfoResponse)
        → server 触发 DeviceRecordEvent
  断言：event.getDeviceId() == clientId
        event.getRecord().getRecordList() 不为空

IT-RECORD-02 分页查询（多响应）
  流程：同上，但 client 分两次回复（SumNum=2, TotalNum=10）
  断言：server 收到两次 DeviceRecordEvent，SN 相同
```

### 3.7 历史回放流程（P1）

**文件**：`PlaybackFlowTest.java`

**覆盖协议**：§9.8 历史视音频的回放

```
IT-PLAYBACK-01 历史回放建立
  流程：server.deviceInvitePlayBack(clientId, ip, port, UDP, startTime, endTime)
        → client 收到 INVITE（SDP s="Playback"）
        → client 回复 200 OK
        → server 触发 DeviceInviteOkEvent
  断言：SDP s 字段 == "Playback"

IT-PLAYBACK-02 回放暂停/继续（INFO 控制）
  前置：IT-PLAYBACK-01 完成
  流程：server.deviceInvitePlayBackControl(clientId, PlayActionEnums.PAUSE)
        → client 收到 INFO（含 MANSRTSP PAUSE）
        → client 侧 InfoRequestHandler 被调用
  断言：INFO 消息体包含 "PAUSE"

IT-PLAYBACK-03 回放快进
  流程：server.deviceInvitePlayBackControl(clientId, PlayActionEnums.FAST_FORWARD_2X)
  断言：INFO 消息体包含倍速参数
```

### 3.8 订阅与通知流程（P1）

**文件**：`SubscribeFlowTest.java`

**覆盖协议**：§9.11 订阅和通知

```
IT-SUB-01 目录订阅
  流程：server.deviceCatalogSubscribe(clientId, 3600, "Catalog")
        → client 收到 SUBSCRIBE
        → client 回复 200 OK
        → server 触发 DeviceSubscribeResponseEvent
  断言：event.getExpires() == 3600

IT-SUB-02 目录变更通知
  前置：IT-SUB-01 完成
  流程：ClientCommandSender.sendDeviceChannelUpdateCommand(...)
        → server 收到 NOTIFY
        → server 触发 DeviceNotifyUpdateEvent
  断言：event.getDeviceId() == clientId

IT-SUB-03 移动位置订阅
  流程：server.deviceMobilePositionSubscribe(clientId, "5", 3600, "MobilePosition", "1")
        → client 收到 SUBSCRIBE
        → client 回复 200 OK
  断言：SUBSCRIBE 消息体包含 Interval 字段
```

---

## 四、需新增的测试辅助实现

### 4.1 TestMessageRequestHandler

客户端侧业务接口实现，供集成测试注入测试数据：

```java
// 位置：gb28181-test/src/main/java/.../handler/TestMessageRequestHandler.java
@Primary @Component
public class TestMessageRequestHandler implements MessageRequestHandler {
    // 返回固定测试数据，各方法可通过 setter 注入自定义响应
    @Override
    public DeviceInfo getDeviceInfo(String userId) {
        return buildTestDeviceInfo(userId);
    }
    @Override
    public List<DeviceItem> getDeviceItem(String userId) {
        return List.of(buildTestDeviceItem(userId));
    }
    // ... 其他方法返回合理的测试数据
}
```

### 4.2 TestDeviceControlHandler

```java
// 位置：gb28181-test/src/main/java/.../handler/TestDeviceControlHandler.java
@Primary @Component
public class TestDeviceControlHandler implements DeviceControlRequestHandler {
    private volatile Object lastCommand;
    private volatile CountDownLatch latch;

    public void reset(CountDownLatch latch) { this.latch = latch; this.lastCommand = null; }
    public Object getLastCommand() { return lastCommand; }

    @Override
    public void handlePtzControl(String deviceId, DeviceControlPtz ptz) {
        lastCommand = ptz;
        if (latch != null) latch.countDown();
    }
    // ... 其他控制类型
}
```

### 4.3 TestInviteRequestHandler

```java
// 位置：gb28181-test/src/main/java/.../handler/TestInviteRequestHandler.java
@Primary @Component
public class TestInviteRequestHandler implements InviteRequestHandler {
    @Override
    public String getSdpResponse(String deviceId, String remoteSdp) {
        // 返回合法的 SDP 响应，供 INVITE 200 OK 使用
        return buildTestSdpResponse();
    }
}
```

---

## 五、未实现功能补全方案

### 5.1 网络校时（§9.10）

**缺失**：`ServerCommandSender` 无 `deviceTimeSync()` 方法。

**补全方案**：
```java
// 在 ServerCommandSender 中新增
public String deviceTimeSync(String deviceId) {
    DeviceTimeNotify notify = new DeviceTimeNotify(
        CmdTypeEnum.TIME_SYNC.getType(), sn(), deviceId);
    notify.setTime(DateUtils.formatDateTime(new Date()));
    return send("MESSAGE", deviceId, notify);
}
```

需同步在 `gb28181-common` 中添加 `DeviceTimeNotify` 实体（若不存在）。

### 5.2 语音对讲（§9.12.2）

**缺失**：客户端主动发起语音对讲的 INVITE 流程（SDP 中 `s=Talk`）。

**补全方案**：
```java
// ClientCommandSender 新增
public static String sendInviteTalkCommand(FromDevice from, ToDevice to, String sdpContent) {
    return send("INVITE", from, to, sdpContent);
    // sdpContent 中 s 字段需为 "Talk"
}
```

### 5.3 多响应消息传输（附录 N）

**缺失**：`RecordInfoMessageHandler` 和 `ResponseCatalogMessageHandler` 未处理分页（`SumNum` < `TotalNum` 时需等待后续消息）。

**补全方案**：在 handler 中维护按 `SN` 聚合的临时缓存，当 `SumNum == TotalNum` 时才发布事件。

---

## 六、实施计划

### 阶段一：P0 测试（本次实现）

| 任务 | 文件 | 工作量 |
|------|------|--------|
| 新增 TestQueryHandler | `handler/TestQueryHandler.java` | 0.5h |
| 新增 TestMessageRequestHandler | `handler/TestMessageRequestHandler.java` | 1h |
| 新增 TestInviteRequestHandler | `handler/TestInviteRequestHandler.java` | 0.5h |
| 新增 TestDeviceControlHandler | `handler/TestDeviceControlHandler.java` | 0.5h |
| 实现 DeviceQueryFlowTest | `test/DeviceQueryFlowTest.java` | 2h |
| 实现 InvitePlayFlowTest | `test/InvitePlayFlowTest.java` | 2h |
| 实现 DeviceControlFlowTest | `test/DeviceControlFlowTest.java` | 1.5h |
| 实现 AlarmFlowTest | `test/AlarmFlowTest.java` | 1h |

### 阶段二：P1 测试

| 任务 | 文件 | 工作量 |
|------|------|--------|
| 实现 KeepaliveFlowTest | `test/KeepaliveFlowTest.java` | 1h |
| 实现 RecordQueryFlowTest | `test/RecordQueryFlowTest.java` | 1.5h |
| 实现 PlaybackFlowTest | `test/PlaybackFlowTest.java` | 2h |
| 实现 SubscribeFlowTest | `test/SubscribeFlowTest.java` | 2h |

### 阶段三：P2 功能补全 + 测试

| 任务 | 工作量 |
|------|--------|
| 补全网络校时发送方法 + 测试 | 1h |
| 补全语音对讲 INVITE 流程 + 测试 | 2h |
| 补全多响应消息分页聚合 + 测试 | 3h |

---

## 七、执行命令

```bash
# 运行所有集成测试
mvn verify -pl gb28181-test --also-make -Dspring.profiles.active=test

# 运行单个测试类
mvn test -pl gb28181-test -Dtest=DeviceQueryFlowTest -Dspring.profiles.active=test

# 查看覆盖率报告（JaCoCo）
mvn verify -pl gb28181-test --also-make
open gb28181-test/target/site/jacoco/index.html
```

---

## 八、测试数据规范

所有测试使用以下固定设备 ID（符合 GB28181 编码规则，附录 D）：

```
服务端（平台）：34020000002000000001
客户端（设备）：34020000001320000001
通道 ID：      34020000001320000001（与设备 ID 相同，单通道设备）
```

SDP 测试模板：
```
v=0
o=34020000001320000001 0 0 IN IP4 127.0.0.1
s=Play
c=IN IP4 127.0.0.1
t=0 0
m=video 10000 RTP/AVP 96
a=rtpmap:96 PS/90000
y=0100000001
```
