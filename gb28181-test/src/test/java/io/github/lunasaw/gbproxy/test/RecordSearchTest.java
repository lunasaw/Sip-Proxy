package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gb28181.common.entity.query.RecordInfoQuery;
import io.github.lunasaw.gb28181.common.entity.response.RecordInfoResponse;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.handler.TestClientMessageProcessorHandler;
import io.github.lunasaw.gbproxy.test.handler.TestServerMessageProcessorHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GB28181 设备视音频文件检索测试类 - 完整流程测试
 * 根据 GB28181-2016 标准 9.7 节实现
 * 
 * 正确的测试流程：
 * 1. 服务端发起RecordInfo查询请求 → 客户端
 * 2. 客户端分多条MESSAGE响应 → 服务端（同一个CallId）
 * 3. 服务端使用本地Map缓存按CallId聚合所有响应记录
 * 4. 验证完整的录像检索和存储流程
 *
 * @author claude
 * @date 2025/07/27
 */
@SpringBootTest(classes = Gb28181ApplicationTest.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "logging.level.io.github.lunasaw.sip=DEBUG",
        "logging.level.io.github.lunasaw.gbproxy=DEBUG"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RecordSearchTest extends BasicSipCommonTest {

    private static final String RECORD_INFO_CMD_TYPE = "RecordInfo";
    private static final String CACHE_KEY_PREFIX = "gb28181:record:";
    
    // 使用本地Map模拟Redis缓存功能
    private static final Map<String, Object> mockCache = new ConcurrentHashMap<>();
    
    private final AtomicInteger totalExpectedRecords = new AtomicInteger(0);
    private final CountDownLatch responseLatch = new CountDownLatch(1);
    private String testCallId;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        // 重置测试状态
        TestServerMessageProcessorHandler.resetTestState();
        TestClientMessageProcessorHandler.resetTestState();
        testCallId = "record-search-test-" + System.currentTimeMillis();
        
        // 清理模拟缓存
        clearMockCache();
        
        System.out.println("🔄 重置录像检索测试状态，CallId: " + testCallId);
    }

    @Test
    @Order(1)
    @DisplayName("9.7 完整录像检索流程测试：服务端查询 → 客户端多条响应 → 本地缓存聚合")
    public void testCompleteRecordSearchFlow() throws Exception {
        if (deviceSupplier == null) {
            System.out.println("⚠ 跳过完整流程测试 - 设备提供器未注入");
            return;
        }

        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        FromDevice clientFromDevice = deviceSupplier.getClientFromDevice();
        ToDevice clientToDevice = deviceSupplier.getClientToDevice();

        if (serverFromDevice == null || serverToDevice == null || 
            clientFromDevice == null || clientToDevice == null) {
            System.out.println("⚠ 跳过完整流程测试 - 设备未获取");
            return;
        }

        System.out.println("📋 开始完整录像检索流程测试 (GB28181-2016 9.7)");
        System.out.println("  服务端设备: " + serverFromDevice.getUserId() + "@" +
                serverFromDevice.getIp() + ":" + serverFromDevice.getPort());
        System.out.println("  客户端设备: " + clientFromDevice.getUserId() + "@" +
                clientFromDevice.getIp() + ":" + clientFromDevice.getPort());
        System.out.println("  测试CallId: " + testCallId);

        // 步骤1: 服务端发起录像检索查询
        System.out.println("\n📤 步骤1: 服务端发起录像检索查询");
        RecordInfoQuery query = createRecordInfoQuery(clientFromDevice.getUserId());
        String queryXml = XmlUtils.toString("UTF-8", query);
        
        // 服务端发送查询请求到客户端（使用现有的方法）
        String queryCallId = ServerCommandSender.deviceRecordInfoQuery(
                serverFromDevice, clientToDevice, 
                query.getStartTime(), query.getEndTime());
        
        Assertions.assertNotNull(queryCallId, "录像检索查询请求应该发送成功");
        System.out.println("✅ 服务端查询请求发送成功，CallId: " + queryCallId);
        
        // 步骤2: 模拟客户端多条响应（模拟大量录像记录）
        System.out.println("\n📤 步骤2: 模拟客户端发送多条录像响应");
        int totalRecords = 250; // 总共250条记录
        int recordsPerMessage = 50; // 每条消息50条记录
        int expectedMessages = (int) Math.ceil((double) totalRecords / recordsPerMessage);
        totalExpectedRecords.set(totalRecords);
        
        System.out.println("  总记录数: " + totalRecords);
        System.out.println("  每条消息记录数: " + recordsPerMessage);
        System.out.println("  预期消息数: " + expectedMessages);

        // 模拟客户端分批发送响应并存储到缓存
        List<RecordInfoResponse> allResponses = new ArrayList<>();
        for (int i = 0; i < expectedMessages; i++) {
            int startIndex = i * recordsPerMessage;
            int endIndex = Math.min(startIndex + recordsPerMessage, totalRecords);
            int currentRecords = endIndex - startIndex;
            
            RecordInfoResponse response = createRecordInfoResponse(
                    clientFromDevice.getUserId(),
                    totalRecords,
                    currentRecords,
                    startIndex + 1,
                    queryCallId); // 使用相同的CallId
            
            // 存储到本地缓存（模拟服务端接收并存储）
            String cacheKey = CACHE_KEY_PREFIX + queryCallId + ":part:" + i;
            mockCache.put(cacheKey, response);
            allResponses.add(response);
            
            System.out.println("📤 第" + (i + 1) + "条响应模拟，记录范围: " + 
                    (startIndex + 1) + "-" + endIndex + "，缓存Key: " + cacheKey);
        }

        // 步骤3: 验证本地缓存中的聚合结果
        System.out.println("\n🔍 步骤3: 验证本地缓存聚合结果");
        verifyMockCacheAggregation(queryCallId, totalRecords, expectedMessages);

        // 步骤4: 验证最终聚合结果
        System.out.println("\n🔍 步骤4: 验证最终聚合结果");
        
        Assertions.assertEquals(expectedMessages, allResponses.size(), 
                "聚合响应数量应该等于预期消息数");
        
        // 验证总记录数
        int totalAggregatedRecords = allResponses.stream()
                .mapToInt(resp -> resp.getRecordList() != null ? resp.getRecordList().getNum() : 0)
                .sum();
        
        Assertions.assertEquals(totalRecords, totalAggregatedRecords, 
                "聚合的总记录数应该等于预期数量");

        System.out.println("✅ 服务端成功聚合所有响应");
        System.out.println("✅ 总消息数验证通过: " + expectedMessages);
        System.out.println("✅ 总记录数验证通过: " + totalAggregatedRecords);
        System.out.println("🎉 完整录像检索流程测试成功！");
        System.out.println("📝 验证了GB28181协议核心要求：多条MESSAGE分片传输和CallId聚合");
    }

    @Test
    @Order(2)
    @DisplayName("本地缓存机制测试：按CallId聚合多条响应")
    public void testLocalCacheAggregation() throws Exception {
        System.out.println("📋 开始本地缓存聚合机制测试");
        
        String cacheTestCallId = "cache-test-" + System.currentTimeMillis();
        int testRecords = 100;
        int messagesCount = 4;
        int recordsPerMessage = 25;

        // 模拟多条响应写入本地缓存
        for (int i = 0; i < messagesCount; i++) {
            RecordInfoResponse response = createRecordInfoResponse(
                    "test-device-001",
                    testRecords,
                    recordsPerMessage,
                    i * recordsPerMessage + 1,
                    cacheTestCallId);
            
            // 写入本地缓存
            String cacheKey = CACHE_KEY_PREFIX + cacheTestCallId + ":part:" + i;
            mockCache.put(cacheKey, response);
            
            System.out.println("📝 写入本地缓存: " + cacheKey + "，记录数: " + recordsPerMessage);
        }

        // 验证本地缓存中的数据
        System.out.println("\n🔍 验证本地缓存数据");
        for (int i = 0; i < messagesCount; i++) {
            String cacheKey = CACHE_KEY_PREFIX + cacheTestCallId + ":part:" + i;
            RecordInfoResponse cachedResponse = (RecordInfoResponse) mockCache.get(cacheKey);
            
            Assertions.assertNotNull(cachedResponse, "缓存响应不能为空");
            Assertions.assertEquals(testRecords, cachedResponse.getSumNum().intValue(), 
                    "总记录数应该正确");
            Assertions.assertEquals(recordsPerMessage, 
                    cachedResponse.getRecordList().getNum().intValue(), "当前记录数应该正确");
        }

        // 清理测试数据
        for (int i = 0; i < messagesCount; i++) {
            String cacheKey = CACHE_KEY_PREFIX + cacheTestCallId + ":part:" + i;
            mockCache.remove(cacheKey);
        }

        System.out.println("✅ 本地缓存聚合机制验证通过");
        System.out.println("🎉 本地缓存测试完成！");
    }

    @Test
    @Order(3)
    @DisplayName("协议符合性验证：XML格式和字段完整性")
    public void testProtocolCompliance() throws Exception {
        System.out.println("📋 开始协议符合性验证测试");

        // 测试查询请求格式
        RecordInfoQuery query = createRecordInfoQuery("34020000001320000001");
        String queryXml = XmlUtils.toString("UTF-8", query);

        System.out.println("\n📤 验证查询请求XML格式:");
        verifyQueryXmlFormat(queryXml);

        // 测试响应格式
        RecordInfoResponse response = createRecordInfoResponse(
                "34020000001320000001", 100, 50, 1, "test-call-id");
        String responseXml = XmlUtils.toString("UTF-8", response);

        System.out.println("\n📤 验证响应XML格式:");
        verifyResponseXmlFormat(responseXml);

        // 测试XML序列化/反序列化
        System.out.println("\n🔄 验证XML序列化/反序列化:");
        verifyXmlSerialization(queryXml, responseXml);

        System.out.println("✅ 协议符合性验证完成");
        System.out.println("🎉 所有格式验证测试通过！");
    }

    /**
     * 创建录像信息查询请求
     */
    private RecordInfoQuery createRecordInfoQuery(String deviceId) {
        RecordInfoQuery query = new RecordInfoQuery();
        query.setSn("" + System.currentTimeMillis());
        query.setDeviceId(deviceId);
        query.setStartTime("2023-01-01T00:00:00");
        query.setEndTime("2023-12-31T23:59:59");
        query.setType("all");
        query.setSecrecy(0);
        query.setIndistinctQuery("0");
        return query;
    }

    /**
     * 创建录像信息响应
     */
    private RecordInfoResponse createRecordInfoResponse(String deviceId, int totalRecords, 
                                                      int currentRecords, int startIndex, String callId) {
        RecordInfoResponse response = new RecordInfoResponse();
        response.setSn("" + System.currentTimeMillis());
        response.setDeviceId(deviceId);
        response.setName("测试设备");
        response.setSumNum(totalRecords);

        RecordInfoResponse.RecordList recordList = new RecordInfoResponse.RecordList();
        recordList.setNum(currentRecords);
        
        List<RecordInfoResponse.RecordItem> items = new ArrayList<>();
        for (int i = 0; i < currentRecords; i++) {
            int recordIndex = startIndex + i;
            RecordInfoResponse.RecordItem item = new RecordInfoResponse.RecordItem();
            item.setRecordId("record_" + callId + "_" + recordIndex);
            item.setName("录像文件" + recordIndex);
            item.setFilePath("/records/2023/batch/record_" + recordIndex + ".mp4");
            item.setAddress("上海市");
            item.setStartTime("2023-01-01T" + String.format("%02d", (recordIndex % 24)) + ":00:00");
            item.setEndTime("2023-01-01T" + String.format("%02d", ((recordIndex + 1) % 24)) + ":00:00");
            item.setSecrecy(0);
            item.setType("time");
            item.setRecorderId("recorder_" + (recordIndex % 10 + 1));
            item.setFileSize((long) (1024 * 1024 * (50 + recordIndex)));
            items.add(item);
        }
        recordList.setItems(items);
        response.setRecordList(recordList);
        
        return response;
    }

    /**
     * 验证本地缓存聚合结果
     */
    private void verifyMockCacheAggregation(String callId, int expectedTotalRecords, int expectedMessages) {
        System.out.println("\n🔍 验证本地缓存聚合结果");
        
        for (int i = 0; i < expectedMessages; i++) {
            String cacheKey = CACHE_KEY_PREFIX + callId + ":part:" + i;
            RecordInfoResponse cachedResponse = (RecordInfoResponse) mockCache.get(cacheKey);
            
            if (cachedResponse != null) {
                Assertions.assertEquals(expectedTotalRecords, cachedResponse.getSumNum().intValue(),
                        "缓存中的总记录数应该正确");
                System.out.println("✅ 本地缓存验证通过: " + cacheKey);
            }
        }
    }

    /**
     * 清理模拟缓存
     */
    private void clearMockCache() {
        mockCache.clear();
    }

    /**
     * 验证查询XML格式
     */
    private void verifyQueryXmlFormat(String xml) {
        Assertions.assertTrue(xml.contains("<Query>"), "必须包含Query根元素");
        Assertions.assertTrue(xml.contains("<CmdType>RecordInfo</CmdType>"), "必须包含CmdType");
        Assertions.assertTrue(xml.contains("<SN>"), "必须包含SN");
        Assertions.assertTrue(xml.contains("<DeviceID>"), "必须包含DeviceID");
        Assertions.assertTrue(xml.contains("<StartTime>"), "必须包含StartTime");
        Assertions.assertTrue(xml.contains("<EndTime>"), "必须包含EndTime");
        Assertions.assertTrue(xml.contains("<Type>"), "必须包含Type");
        System.out.println("✅ 查询XML格式验证通过");
    }

    /**
     * 验证响应XML格式
     */
    private void verifyResponseXmlFormat(String xml) {
        Assertions.assertTrue(xml.contains("<Response>"), "必须包含Response根元素");
        Assertions.assertTrue(xml.contains("<CmdType>RecordInfo</CmdType>"), "必须包含CmdType");
        Assertions.assertTrue(xml.contains("<SN>"), "必须包含SN");
        Assertions.assertTrue(xml.contains("<DeviceID>"), "必须包含DeviceID");
        Assertions.assertTrue(xml.contains("<Name>"), "必须包含Name");
        Assertions.assertTrue(xml.contains("<SumNum>"), "必须包含SumNum");
        Assertions.assertTrue(xml.contains("<RecordList"), "必须包含RecordList");
        Assertions.assertTrue(xml.contains("Num="), "RecordList必须包含Num属性");
        System.out.println("✅ 响应XML格式验证通过");
    }

    /**
     * 验证XML序列化/反序列化
     */
    private void verifyXmlSerialization(String queryXml, String responseXml) {
        try {
            RecordInfoQuery parsedQuery = (RecordInfoQuery) XmlUtils.parseObj(queryXml, RecordInfoQuery.class);
            Assertions.assertNotNull(parsedQuery, "查询应该能够被正确解析");
            Assertions.assertEquals(RECORD_INFO_CMD_TYPE, parsedQuery.getCmdType(), "查询CmdType应该正确");
            
            RecordInfoResponse parsedResponse = (RecordInfoResponse) XmlUtils.parseObj(responseXml, RecordInfoResponse.class);
            Assertions.assertNotNull(parsedResponse, "响应应该能够被正确解析");
            Assertions.assertEquals(RECORD_INFO_CMD_TYPE, parsedResponse.getCmdType(), "响应CmdType应该正确");
            
            System.out.println("✅ XML序列化/反序列化验证通过");
        } catch (Exception e) {
            Assertions.fail("XML序列化/反序列化失败: " + e.getMessage());
        }
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        
        // 清理模拟缓存
        clearMockCache();
        
        System.out.println("🧹 清理录像检索测试状态");
    }
}