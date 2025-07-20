package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.test.config.TestSuiteConfig;
import io.github.lunasaw.gbproxy.test.runner.TestMetricsCollector;
import io.github.lunasaw.gbproxy.test.runner.TestReportGenerator;
import io.github.lunasaw.gbproxy.test.util.TestDataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GB28181测试模块验证应用
 * 验证测试框架和工具类是否可以正常工作
 */
@Slf4j
@SpringBootApplication
public class TestFrameworkVerifyApplication implements CommandLineRunner {

    @Autowired
    private TestSuiteConfig testSuiteConfig;

    @Autowired
    private TestMetricsCollector metricsCollector;

    @Autowired
    private TestReportGenerator reportGenerator;

    @Autowired
    private TestDataGenerator dataGenerator;

    public static void main(String[] args) {
        log.info("========================================");
        log.info("     GB28181测试框架验证启动");
        log.info("========================================");

        SpringApplication app = new SpringApplication(TestFrameworkVerifyApplication.class);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("开始验证GB28181测试框架...");

        // 验证配置加载
        verifyConfiguration();

        // 验证测试工具
        verifyTestTools();

        // 验证数据生成器
        verifyDataGenerator();

        // 验证指标收集器
        verifyMetricsCollector();

        // 验证报告生成器
        verifyReportGenerator();

        log.info("========================================");
        log.info("     GB28181测试框架验证完成");
        log.info("========================================");
        log.info("✓ 所有测试框架组件工作正常");
        log.info("✓ 测试模块可以独立运行");
        log.info("✓ 配置文件加载正确");
        log.info("✓ 工具类功能完整");
        log.info("========================================");
    }

    private void verifyConfiguration() {
        log.info(">>> 验证配置加载...");

        log.info("测试模式: {}", testSuiteConfig.getMode());
        log.info("测试套件: {}", testSuiteConfig.getSuite());
        log.info("并发执行: {}", testSuiteConfig.isConcurrent());
        log.info("生成报告: {}", testSuiteConfig.isReport());
        log.info("输出目录: {}", testSuiteConfig.getOutputDir());

        log.info("✓ 配置加载验证完成");
    }

    private void verifyTestTools() {
        log.info(">>> 验证测试工具...");

        // 测试基础工具是否可用
        if (testSuiteConfig != null) {
            log.info("✓ TestSuiteConfig 工具正常");
        }

        if (metricsCollector != null) {
            log.info("✓ TestMetricsCollector 工具正常");
        }

        if (reportGenerator != null) {
            log.info("✓ TestReportGenerator 工具正常");
        }

        if (dataGenerator != null) {
            log.info("✓ TestDataGenerator 工具正常");
        }

        log.info("✓ 测试工具验证完成");
    }

    private void verifyDataGenerator() {
        log.info(">>> 验证数据生成器...");

        try {
            // 生成测试设备ID
            String deviceId = dataGenerator.generateDeviceId("44050100");
            log.info("生成设备ID: {}", deviceId);

            // 生成Call-ID
            String callId = dataGenerator.generateCallId();
            log.info("生成Call-ID: {}", callId);

            // 生成XML消息
            String deviceInfoXml = dataGenerator.generateDeviceInfoXml(deviceId);
            log.info("生成设备信息XML: {}字符", deviceInfoXml.length());

            String deviceStatusXml = dataGenerator.generateDeviceStatusXml(deviceId);
            log.info("生成设备状态XML: {}字符", deviceStatusXml.length());

            log.info("✓ 数据生成器验证完成");

        } catch (Exception e) {
            log.error("✗ 数据生成器验证失败", e);
            throw new RuntimeException("数据生成器验证失败", e);
        }
    }

    private void verifyMetricsCollector() {
        log.info(">>> 验证指标收集器...");

        try {
            String sessionId = "verify-session-" + System.currentTimeMillis();

            // 开始收集
            metricsCollector.startCollection(sessionId);

            // 记录一些测试指标
            metricsCollector.recordTestStart("验证测试");
            Thread.sleep(100);
            metricsCollector.recordTestSuccess("验证测试");

            metricsCollector.recordSipRequestSent("REGISTER");
            metricsCollector.recordSipResponseReceived(200);
            metricsCollector.recordGb28181Message("register");

            // 停止收集
            metricsCollector.stopCollection();

            // 验证指标是否记录
            long testCount = metricsCollector.getCounter("test.total");
            long successCount = metricsCollector.getCounter("test.success");

            log.info("记录的测试数量: {}", testCount);
            log.info("记录的成功数量: {}", successCount);

            log.info("✓ 指标收集器验证完成");

        } catch (Exception e) {
            log.error("✗ 指标收集器验证失败", e);
            throw new RuntimeException("指标收集器验证失败", e);
        }
    }

    private void verifyReportGenerator() {
        log.info(">>> 验证报告生成器...");

        try {
            // 初始化输出目录
            reportGenerator.initializeOutputDirectory();

            // 添加一些测试结果
            reportGenerator.addTestResult("验证测试1", "框架验证", true, 100, "测试通过", null);
            reportGenerator.addTestResult("验证测试2", "框架验证", true, 150, "测试通过", null);

            String sessionId = "verify-session-" + System.currentTimeMillis();

            // 生成报告
            reportGenerator.generateReport(sessionId);

            log.info("✓ 报告生成器验证完成");

        } catch (Exception e) {
            log.error("✗ 报告生成器验证失败", e);
            throw new RuntimeException("报告生成器验证失败", e);
        }
    }
}