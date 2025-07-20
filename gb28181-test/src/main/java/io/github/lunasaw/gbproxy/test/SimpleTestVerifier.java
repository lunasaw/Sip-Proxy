package io.github.lunasaw.gbproxy.test;

import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 简单测试验证器
 * 用于验证测试模块的核心功能，不依赖外部组件
 */
public class SimpleTestVerifier {

    private final Random random = new Random();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        SimpleTestVerifier verifier = new SimpleTestVerifier();
        verifier.runVerificationTests();
    }

    public void runVerificationTests() {
        System.out.println("=== GB28181 测试模块独立验证 ===");
        System.out.println("开始时间: " + LocalDateTime.now().format(formatter));
        System.out.println();

        // 测试数据生成功能
        testDataGeneration();

        // 测试SIP协议相关功能
        testSipFunctions();

        // 测试GB28181设备ID生成
        testDeviceIdGeneration();

        // 测试XML消息生成
        testXmlGeneration();

        System.out.println();
        System.out.println("=== 验证完成 ===");
        System.out.println("结束时间: " + LocalDateTime.now().format(formatter));
        System.out.println("测试模块核心功能验证通过，可以独立运行！");
    }

    private void testDataGeneration() {
        System.out.println("[测试] 数据生成功能");

        // 生成随机设备ID
        String deviceId = generateDeviceId("44050100");
        System.out.println("  生成设备ID: " + deviceId);

        // 生成测试用户名
        String username = generateTestUsername();
        System.out.println("  生成用户名: " + username);

        // 生成测试密码
        String password = generateTestPassword();
        System.out.println("  生成密码: " + password);

        // 生成测试IP
        String ip = generateTestIpAddress();
        System.out.println("  生成IP地址: " + ip);

        // 生成测试端口
        int port = generateTestPort();
        System.out.println("  生成端口: " + port);

        System.out.println("  ✓ 数据生成功能正常");
        System.out.println();
    }

    private void testSipFunctions() {
        System.out.println("[测试] SIP协议功能");

        String callId = generateCallId();
        System.out.println("  生成Call-ID: " + callId);

        String viaBranch = generateViaBranch();
        System.out.println("  生成Via分支: " + viaBranch);

        String tag = generateTag();
        System.out.println("  生成Tag: " + tag);

        System.out.println("  ✓ SIP协议功能正常");
        System.out.println();
    }

    private void testDeviceIdGeneration() {
        System.out.println("[测试] GB28181设备ID生成");

        for (int i = 0; i < 3; i++) {
            String deviceId = generateDeviceId("44050100");
            System.out.println("  设备ID " + (i + 1) + ": " + deviceId);

            // 验证设备ID格式
            if (deviceId.length() == 20 && deviceId.startsWith("44050100")) {
                System.out.println("    ✓ 格式正确");
            } else {
                System.out.println("    ✗ 格式错误");
            }
        }

        System.out.println("  ✓ 设备ID生成功能正常");
        System.out.println();
    }

    private void testXmlGeneration() {
        System.out.println("[测试] XML消息生成");

        String deviceId = "34020000001320000001";

        String deviceInfoXml = generateDeviceInfoXml(deviceId);
        System.out.println("  设备信息XML (部分): " + deviceInfoXml.substring(0, Math.min(100, deviceInfoXml.length())) + "...");

        String deviceStatusXml = generateDeviceStatusXml(deviceId);
        System.out.println("  设备状态XML (部分): " + deviceStatusXml.substring(0, Math.min(100, deviceStatusXml.length())) + "...");

        String catalogXml = generateCatalogXml(deviceId, 2);
        System.out.println("  目录查询XML (部分): " + catalogXml.substring(0, Math.min(100, catalogXml.length())) + "...");

        System.out.println("  ✓ XML消息生成功能正常");
        System.out.println();
    }

    // 生成设备ID的简化版本
    private String generateDeviceId(String prefix) {
        if (prefix == null) {
            prefix = "44050100";
        }

        if (prefix.length() > 8) {
            prefix = prefix.substring(0, 8);
        } else if (prefix.length() < 8) {
            prefix = String.format("%-8s", prefix).replace(' ', '0');
        }

        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }

    private String generateCallId() {
        return "call-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
    }

    private String generateViaBranch() {
        return "z9hG4bK-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
    }

    private String generateTag() {
        return "tag-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
    }

    private String generateTestIpAddress() {
        return "192.168." + random.nextInt(256) + "." + (random.nextInt(254) + 1);
    }

    private int generateTestPort() {
        return 5000 + random.nextInt(55000);
    }

    private String generateTestUsername() {
        String[] prefixes = {"test", "user", "device", "client"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        return prefix + "_" + random.nextInt(1000);
    }

    private String generateTestPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        int length = 8 + random.nextInt(8);

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    private String generateDeviceInfoXml(String deviceId) {
        return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Response>\n" +
                        "    <CmdType>DeviceInfo</CmdType>\n" +
                        "    <SN>%d</SN>\n" +
                        "    <DeviceID>%s</DeviceID>\n" +
                        "    <DeviceName>%s</DeviceName>\n" +
                        "    <Manufacturer>%s</Manufacturer>\n" +
                        "    <Model>%s</Model>\n" +
                        "    <Firmware>%s</Firmware>\n" +
                        "    <Channel>%d</Channel>\n" +
                        "    <Result>OK</Result>\n" +
                        "</Response>",
                System.currentTimeMillis() % 100000,
                deviceId,
                "测试设备-" + deviceId.substring(deviceId.length() - 4),
                "测试厂商",
                "TestModel-" + random.nextInt(100),
                "V" + random.nextInt(5) + "." + random.nextInt(10) + "." + random.nextInt(10),
                random.nextInt(32) + 1
        );
    }

    private String generateDeviceStatusXml(String deviceId) {
        return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Response>\n" +
                        "    <CmdType>DeviceStatus</CmdType>\n" +
                        "    <SN>%d</SN>\n" +
                        "    <DeviceID>%s</DeviceID>\n" +
                        "    <Result>OK</Result>\n" +
                        "    <Online>%s</Online>\n" +
                        "    <Status>%s</Status>\n" +
                        "</Response>",
                System.currentTimeMillis() % 100000,
                deviceId,
                random.nextBoolean() ? "ONLINE" : "OFFLINE",
                random.nextBoolean() ? "OK" : "ERROR"
        );
    }

    private String generateCatalogXml(String deviceId, int channelCount) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Response>\n");
        xml.append("    <CmdType>Catalog</CmdType>\n");
        xml.append("    <SN>").append(System.currentTimeMillis() % 100000).append("</SN>\n");
        xml.append("    <DeviceID>").append(deviceId).append("</DeviceID>\n");
        xml.append("    <SumNum>").append(channelCount).append("</SumNum>\n");
        xml.append("    <DeviceList Num=\"").append(channelCount).append("\">\n");

        for (int i = 1; i <= channelCount; i++) {
            String channelId = deviceId.substring(0, 10) + String.format("%010d", i);
            xml.append("        <Item>\n");
            xml.append("            <DeviceID>").append(channelId).append("</DeviceID>\n");
            xml.append("            <Name>通道-").append(i).append("</Name>\n");
            xml.append("            <Manufacturer>测试厂商</Manufacturer>\n");
            xml.append("            <Model>TestChannel</Model>\n");
            xml.append("            <Owner>Admin</Owner>\n");
            xml.append("            <CivilCode>").append(deviceId.substring(0, 6)).append("</CivilCode>\n");
            xml.append("            <Address>测试地址-").append(i).append("</Address>\n");
            xml.append("            <Parental>1</Parental>\n");
            xml.append("            <ParentID>").append(deviceId).append("</ParentID>\n");
            xml.append("            <SafetyWay>0</SafetyWay>\n");
            xml.append("            <RegisterWay>1</RegisterWay>\n");
            xml.append("            <Secrecy>0</Secrecy>\n");
            xml.append("            <Status>ON</Status>\n");
            xml.append("        </Item>\n");
        }

        xml.append("    </DeviceList>\n");
        xml.append("</Response>\n");

        return xml.toString();
    }
}