package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.sip.common.transmit.event.message.MessageHandler;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 protocol dispatcher 注册 key 完整性测试。
 *
 * <p>本测试是矩阵 §0(A) / §2.1 / §3.1 的代码事实校验：
 * 协议要求的每个 (rootType, method, cmdType) 三元组都必须在 {@link SipRequestProcessorAbstract#MESSAGE_HANDLER_CMD_MAP}
 * 中注册了 handler；缺失则该 cmdType 的入站消息将被静默丢弃，listener 永远不被回调。
 *
 * <p>v1.5.6 起 TDD 化：本测试先于 handler 类编写，定义协议路由契约。
 *
 * @author luna
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class DispatcherRegistrationTest {

    private static MessageHandler resolve(String rootType, String method, String cmdType) {
        Map<String, MessageHandler> map = SipRequestProcessorAbstract.MESSAGE_HANDLER_CMD_MAP.get(rootType);
        if (map == null) {
            return null;
        }
        return map.get(method + "_" + cmdType);
    }

    // ============== Server 端 入站协议路由 ==============

    @Test
    @DisplayName("§A.2.5.6 server 端必须注册 MobilePosition Notify handler（rootType=Notify, method=MESSAGE, cmdType=MobilePosition）")
    void serverShouldRegisterMobilePositionNotifyHandler() {
        MessageHandler handler = resolve("Notify", "MESSAGE", "MobilePosition");
        assertThat(handler)
            .as("MobilePosition Notify dispatcher key 缺失：设备 GPS 位置上报会被静默丢弃")
            .isNotNull();
        assertThat(handler.getClass().getSimpleName())
            .isEqualTo("MobilePositionNotifyMessageHandler");
    }

    @Test
    @DisplayName("§A.2.5.8 server 端必须注册 VideoUpload Notify handler（rootType=Notify, method=MESSAGE, cmdType=VideoUploadNotify）")
    void serverShouldRegisterVideoUploadNotifyHandler() {
        MessageHandler handler = resolve("Notify", "MESSAGE", "VideoUploadNotify");
        assertThat(handler)
            .as("VideoUploadNotify dispatcher key 缺失：设备实时视音频回传通知会被静默丢弃")
            .isNotNull();
        assertThat(handler.getClass().getSimpleName())
            .isEqualTo("VideoUploadNotifyMessageHandler");
    }

    @Test
    @DisplayName("§A.2.6.9 server 端必须注册 ConfigDownload Response handler（rootType=Response, method=MESSAGE, cmdType=ConfigDownload）")
    void serverShouldRegisterConfigDownloadResponseHandler() {
        MessageHandler handler = resolve("Response", "MESSAGE", "ConfigDownload");
        assertThat(handler)
            .as("ConfigDownload Response dispatcher key 缺失：设备配置下载应答会被静默丢弃")
            .isNotNull();
        assertThat(handler.getClass().getSimpleName())
            .isEqualTo("ConfigDownloadResponseMessageHandler");
    }

    @Test
    @DisplayName("§A.2.6.10 server 端必须注册 PresetQuery Response handler（rootType=Response, method=MESSAGE, cmdType=PresetQuery）")
    void serverShouldRegisterPresetQueryResponseHandler() {
        MessageHandler handler = resolve("Response", "MESSAGE", "PresetQuery");
        assertThat(handler)
            .as("PresetQuery Response dispatcher key 缺失：设备预置位查询应答会被静默丢弃")
            .isNotNull();
        assertThat(handler.getClass().getSimpleName())
            .isEqualTo("PresetQueryResponseMessageHandler");
    }

    // ============== Client 端 入站协议路由 ==============

    @Test
    @DisplayName("§A.2.4.9 client 端必须注册 MobilePosition Subscribe handler（rootType=Query, method=SUBSCRIBE, cmdType=MobilePosition）")
    void clientShouldRegisterMobilePositionSubscribeHandler() {
        MessageHandler handler = resolve("Query", "SUBSCRIBE", "MobilePosition");
        assertThat(handler)
            .as("MobilePosition Subscribe dispatcher key 缺失：平台对设备位置订阅会无 200 OK 响应导致事务超时")
            .isNotNull();
        assertThat(handler.getClass().getSimpleName())
            .isEqualTo("SubscribeMobilePositionQueryMessageHandler");
    }

    // ============== 已存在 handler 的回归保护 ==============

    @Test
    @DisplayName("回归保护：v1.5.5 已存在的 server 端 5 个 Notify handler 必须保持注册")
    void existingServerNotifyHandlersShouldStayRegistered() {
        assertThat(resolve("Notify", "MESSAGE", "Alarm")).isNotNull();
        assertThat(resolve("Notify", "MESSAGE", "Keepalive")).isNotNull();
        assertThat(resolve("Notify", "MESSAGE", "MediaStatus")).isNotNull();
        assertThat(resolve("Notify", "MESSAGE", "DeviceUpgradeResult")).isNotNull();
        assertThat(resolve("Notify", "MESSAGE", "UploadSnapShotFinished")).isNotNull();
    }

    @Test
    @DisplayName("回归保护：v1.5.5 已存在的 server 端 10 个 Response handler 必须保持注册")
    void existingServerResponseHandlersShouldStayRegistered() {
        assertThat(resolve("Response", "MESSAGE", "Catalog")).isNotNull();
        assertThat(resolve("Response", "MESSAGE", "DeviceInfo")).isNotNull();
        assertThat(resolve("Response", "MESSAGE", "DeviceStatus")).isNotNull();
        assertThat(resolve("Response", "MESSAGE", "RecordInfo")).isNotNull();
        assertThat(resolve("Response", "MESSAGE", "DeviceConfig")).isNotNull();
        assertThat(resolve("Response", "MESSAGE", "PTZPosition")).isNotNull();
        assertThat(resolve("Response", "MESSAGE", "SDCardStatus")).isNotNull();
        assertThat(resolve("Response", "MESSAGE", "HomePositionQuery")).isNotNull();
        assertThat(resolve("Response", "MESSAGE", "CruiseTrackListQuery")).isNotNull();
        assertThat(resolve("Response", "MESSAGE", "CruiseTrackQuery")).isNotNull();
    }

    @Test
    @DisplayName("【完整性审计】打印当前所有 dispatcher 注册 key（用于人工核对协议覆盖矩阵 §0(A)）")
    void printAllRegisteredDispatcherKeys() {
        System.out.println("==========Total registered handlers==========");
        java.util.List<String> all = new java.util.ArrayList<>();
        SipRequestProcessorAbstract.MESSAGE_HANDLER_CMD_MAP.forEach((rootType, handlerMap) ->
            handlerMap.forEach((key, handler) ->
                all.add(String.format("rootType=%-10s key=%-30s handler=%s", rootType, key, handler.getClass().getSimpleName()))));
        all.sort(java.util.Comparator.naturalOrder());
        all.forEach(System.out::println);
        System.out.println("Total: " + all.size() + " handlers across " +
            SipRequestProcessorAbstract.MESSAGE_HANDLER_CMD_MAP.size() + " rootTypes");
    }

    @Test
    @DisplayName("回归保护：client 端 3 个已存在 Subscribe handler 必须保持注册")
    void existingClientSubscribeHandlersShouldStayRegistered() {
        assertThat(resolve("Query", "SUBSCRIBE", "Catalog"))
            .as("Catalog SUBSCRIBE handler 注册 key 错误（v1.5.5 发现：getMethod() 未 override 导致 key=null_Catalog）")
            .isNotNull();
        assertThat(resolve("Query", "SUBSCRIBE", "Alarm")).isNotNull();
        assertThat(resolve("Query", "SUBSCRIBE", "PTZPosition")).isNotNull();
    }

    @Test
    @DisplayName("§9.11.4 server 端 Catalog NOTIFY handler 必须注册（v1.5.5 发现的死路径修正）")
    void serverShouldRegisterCatalogNotifyHandler() {
        // 项目约定：sendCatalogChangeNotify 通过 SIP MESSAGE 方法承载 <Notify> body（非 SIP NOTIFY 方法）
        MessageHandler handler = resolve("Notify", "MESSAGE", "Catalog");
        assertThat(handler)
            .as("Catalog NOTIFY dispatcher key 缺失（v1.5.5 发现：getRootType=NOTIFYResponse + getMethod=null 双重错误，设备目录变更通知无法路由）")
            .isNotNull();
        assertThat(handler.getClass().getSimpleName())
            .isEqualTo("CatalogNotifyHandler");
    }

    @Test
    @DisplayName("§11.11 BaseMessageServerHandler 死代码必须不存在（已被删除）")
    void deadCodeBaseMessageServerHandlerShouldBeRemoved() {
        // 死代码注册 key=Root_MESSAGE_Catalog，永远无法匹配真实 SIP 报文（rootType 来自 XML 根元素，不可能是 Root）
        Map<String, MessageHandler> rootMap = SipRequestProcessorAbstract.MESSAGE_HANDLER_CMD_MAP.get("Root");
        assertThat(rootMap)
            .as("rootType=Root 不应有任何 handler 注册（BaseMessageServerHandler 已是死代码，应删除）")
            .isNullOrEmpty();
    }
}
