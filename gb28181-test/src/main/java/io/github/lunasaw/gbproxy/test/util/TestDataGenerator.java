package io.github.lunasaw.gbproxy.test.util;

import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceCatalog;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 测试数据生成器
 * 负责生成各种测试场景所需的模拟数据
 */
@Slf4j
@Component
public class TestDataGenerator {

    private final Random random = new Random();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 生成设备信息
     */
    public DeviceInfo generateDeviceInfo(String deviceId) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceName("测试设备-" + deviceId.substring(deviceId.length() - 4));
        deviceInfo.setManufacturer("测试厂商");
        deviceInfo.setModel("TestModel-" + random.nextInt(100));
        deviceInfo.setFirmware("V" + random.nextInt(5) + "." + random.nextInt(10) + "." + random.nextInt(10));
        deviceInfo.setChannel(random.nextInt(32) + 1);
        deviceInfo.setResult("OK");
        return deviceInfo;
    }

    /**
     * 生成设备状态
     */
    public DeviceStatus generateDeviceStatus(String deviceId) {
        DeviceStatus deviceStatus = new DeviceStatus();
        deviceStatus.setDeviceId(deviceId);
        deviceStatus.setResult("OK");
        deviceStatus.setOnline(random.nextBoolean() ? "ONLINE" : "OFFLINE");
        deviceStatus.setStatus(random.nextBoolean() ? "OK" : "ERROR");
        return deviceStatus;
    }

    /**
     * 生成设备目录
     */
    public DeviceCatalog generateDeviceCatalog(String deviceId, int channelCount) {
        DeviceCatalog catalog = new DeviceCatalog();
        catalog.setDeviceId(deviceId);
        catalog.setName("测试设备目录");
        catalog.setManufacturer("测试厂商");
        catalog.setModel("TestModel");
        catalog.setOwner("Admin");
        catalog.setCivilCode(deviceId.substring(0, 6));
        catalog.setAddress("测试地址");
        catalog.setParental(0);
        catalog.setParentId("");
        catalog.setSafetyWay(0);
        catalog.setRegisterWay(1);
        catalog.setSecrecy(0);
        catalog.setStatus("ON");

        return catalog;
    }

    /**
     * 生成设备通道项
     */
    public DeviceItem generateDeviceItem(String parentId, int channelIndex) {
        DeviceItem item = new DeviceItem();

        // 生成通道ID（在父设备ID基础上修改最后几位）
        String channelId = parentId.substring(0, 10) + String.format("%010d", channelIndex);
        item.setDeviceId(channelId);
        item.setName("通道-" + channelIndex);
        item.setManufacturer("测试厂商");
        item.setModel("TestChannel");
        item.setOwner("Admin");
        item.setCivilCode(parentId.substring(0, 6));
        item.setAddress("测试地址-" + channelIndex);
        item.setParental(1);
        item.setParentId(parentId);
        item.setSafetyWay(0);
        item.setRegisterWay(1);
        item.setSecrecy(0);
        item.setStatus("ON");

        return item;
    }

    /**
     * 生成随机设备ID
     */
    public String generateDeviceId(String prefix) {
        if (prefix == null) {
            prefix = "44050100";
        }

        // 确保前缀长度正确
        if (prefix.length() > 8) {
            prefix = prefix.substring(0, 8);
        } else if (prefix.length() < 8) {
            prefix = String.format("%-8s", prefix).replace(' ', '0');
        }

        // 生成12位随机数字
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }

    /**
     * 生成SIP Call-ID
     */
    public String generateCallId() {
        return "call-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
    }

    /**
     * 生成SIP Via分支
     */
    public String generateViaBranch() {
        return "z9hG4bK-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
    }

    /**
     * 生成SIP Tag
     */
    public String generateTag() {
        return "tag-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
    }

    /**
     * 生成随机IP地址（测试用）
     */
    public String generateTestIpAddress() {
        return "192.168." + random.nextInt(256) + "." + (random.nextInt(254) + 1);
    }

    /**
     * 生成随机端口号
     */
    public int generateTestPort() {
        return 5000 + random.nextInt(55000);
    }

    /**
     * 生成测试用户名
     */
    public String generateTestUsername() {
        String[] prefixes = {"test", "user", "device", "client"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        return prefix + "_" + random.nextInt(1000);
    }

    /**
     * 生成测试密码
     */
    public String generateTestPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        int length = 8 + random.nextInt(8); // 8-15位

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    /**
     * 生成XML格式的设备信息查询响应
     */
    public String generateDeviceInfoXml(String deviceId) {
        DeviceInfo deviceInfo = generateDeviceInfo(deviceId);

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
                deviceInfo.getDeviceId(),
                deviceInfo.getDeviceName(),
                deviceInfo.getManufacturer(),
                deviceInfo.getModel(),
                deviceInfo.getFirmware(),
                deviceInfo.getChannel()
        );
    }

    /**
     * 生成XML格式的设备状态查询响应
     */
    public String generateDeviceStatusXml(String deviceId) {
        DeviceStatus deviceStatus = generateDeviceStatus(deviceId);

        return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Response>\n" +
                        "    <CmdType>DeviceStatus</CmdType>\n" +
                        "    <SN>%d</SN>\n" +
                        "    <DeviceID>%s</DeviceID>\n" +
                        "    <Result>%s</Result>\n" +
                        "    <Online>%s</Online>\n" +
                        "    <Status>%s</Status>\n" +
                        "</Response>",
                System.currentTimeMillis() % 100000,
                deviceStatus.getDeviceId(),
                deviceStatus.getResult(),
                deviceStatus.getOnline(),
                deviceStatus.getStatus()
        );
    }

    /**
     * 生成XML格式的目录查询响应
     */
    public String generateCatalogXml(String deviceId, int channelCount) {
        DeviceCatalog catalog = generateDeviceCatalog(deviceId, channelCount);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Response>\n");
        xml.append("    <CmdType>Catalog</CmdType>\n");
        xml.append("    <SN>").append(System.currentTimeMillis() % 100000).append("</SN>\n");
        xml.append("    <DeviceID>").append(catalog.getDeviceId()).append("</DeviceID>\n");
        xml.append("    <SumNum>").append(channelCount).append("</SumNum>\n");
        xml.append("    <DeviceList Num=\"").append(channelCount).append("\">\n");

        for (int i = 1; i <= channelCount; i++) {
            DeviceItem item = generateDeviceItem(deviceId, i);
            xml.append("        <Item>\n");
            xml.append("            <DeviceID>").append(item.getDeviceId()).append("</DeviceID>\n");
            xml.append("            <Name>").append(item.getName()).append("</Name>\n");
            xml.append("            <Manufacturer>").append(item.getManufacturer()).append("</Manufacturer>\n");
            xml.append("            <Model>").append(item.getModel()).append("</Model>\n");
            xml.append("            <Owner>").append(item.getOwner()).append("</Owner>\n");
            xml.append("            <CivilCode>").append(item.getCivilCode()).append("</CivilCode>\n");
            xml.append("            <Address>").append(item.getAddress()).append("</Address>\n");
            xml.append("            <Parental>").append(item.getParental()).append("</Parental>\n");
            xml.append("            <ParentID>").append(item.getParentId()).append("</ParentID>\n");
            xml.append("            <SafetyWay>").append(item.getSafetyWay()).append("</SafetyWay>\n");
            xml.append("            <RegisterWay>").append(item.getRegisterWay()).append("</RegisterWay>\n");
            xml.append("            <Secrecy>").append(item.getSecrecy()).append("</Secrecy>\n");
            xml.append("            <Status>").append(item.getStatus()).append("</Status>\n");
            xml.append("        </Item>\n");
        }

        xml.append("    </DeviceList>\n");
        xml.append("</Response>\n");

        return xml.toString();
    }

    /**
     * 生成随机延迟（毫秒）
     */
    public long generateRandomDelay(int minMs, int maxMs) {
        return minMs + random.nextInt(maxMs - minMs + 1);
    }

    /**
     * 生成随机布尔值（指定概率）
     */
    public boolean generateRandomBoolean(double trueProbability) {
        return random.nextDouble() < trueProbability;
    }
}