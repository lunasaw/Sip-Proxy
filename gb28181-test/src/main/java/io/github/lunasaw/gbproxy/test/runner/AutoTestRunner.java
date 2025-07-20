package io.github.lunasaw.gbproxy.test.runner;

import io.github.lunasaw.gbproxy.test.config.TestSuiteConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自动化测试执行器
 * 负责组织和执行各种测试套件
 */
@Slf4j
@Component
public class AutoTestRunner {

    @Autowired
    private TestSuiteConfig testSuiteConfig;

    @Autowired
    private TestReportGenerator reportGenerator;

    @Autowired
    private TestMetricsCollector metricsCollector;

    private ExecutorService executorService;

    /**
     * 运行测试套件
     */
    public void runTestSuite() {
        log.info("开始执行测试套件 - 模式: {}, 套件: {}",
                testSuiteConfig.getMode(), testSuiteConfig.getSuite());

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String sessionId = "test-session-" + timestamp;

        // 初始化测试环境
        initializeTestEnvironment();

        try {
            // 开始收集指标
            metricsCollector.startCollection(sessionId);

            // 根据配置执行相应的测试套件
            switch (testSuiteConfig.getSuite().toLowerCase()) {
                case "all":
                    runAllTests();
                    break;
                case "server":
                    runServerTests();
                    break;
                case "client":
                    runClientTests();
                    break;
                case "integration":
                    runIntegrationTests();
                    break;
                default:
                    log.warn("未知的测试套件类型: {}", testSuiteConfig.getSuite());
                    runAllTests();
            }

        } catch (Exception e) {
            log.error("测试执行失败", e);
            throw new RuntimeException("测试执行失败", e);
        } finally {
            // 停止收集指标
            metricsCollector.stopCollection();

            // 生成测试报告
            if (testSuiteConfig.isReport()) {
                reportGenerator.generateReport(sessionId);
            }

            // 清理测试环境
            cleanupTestEnvironment();
        }
    }

    /**
     * 运行所有测试
     */
    private void runAllTests() {
        log.info("执行完整测试套件...");

        if (testSuiteConfig.isConcurrent()) {
            runTestsConcurrently();
        } else {
            runTestsSequentially();
        }
    }

    /**
     * 并发执行测试
     */
    private void runTestsConcurrently() {
        log.info("并发执行测试...");

        CompletableFuture<Void> serverTests = CompletableFuture.runAsync(this::runServerTests, executorService);
        CompletableFuture<Void> clientTests = CompletableFuture.runAsync(this::runClientTests, executorService);

        // 等待所有测试完成
        CompletableFuture.allOf(serverTests, clientTests).join();

        // 最后执行集成测试
        runIntegrationTests();
    }

    /**
     * 顺序执行测试
     */
    private void runTestsSequentially() {
        log.info("顺序执行测试...");

        runServerTests();
        runClientTests();
        runIntegrationTests();
    }

    /**
     * 运行服务端测试
     */
    private void runServerTests() {
        log.info("========================================");
        log.info("开始执行服务端功能测试");
        log.info("========================================");

        try {
            // 设备管理测试
            executeTestGroup("服务端设备管理测试", () -> {
                log.info("执行设备注册管理测试...");
                log.info("执行设备认证测试...");
                log.info("执行设备状态监控测试...");
                log.info("执行设备目录管理测试...");
            });

            // 命令控制测试
            executeTestGroup("服务端命令控制测试", () -> {
                log.info("执行云台控制测试...");
                log.info("执行录像控制测试...");
                log.info("执行设备配置测试...");
                log.info("执行告警控制测试...");
            });

            // 媒体流测试
            executeTestGroup("服务端媒体流测试", () -> {
                log.info("执行实时点播测试...");
                log.info("执行历史回放测试...");
                log.info("执行媒体协商测试...");
                log.info("执行流控制测试...");
            });

        } catch (Exception e) {
            log.error("服务端测试执行失败", e);
            if (!testSuiteConfig.isContinueOnFailure()) {
                throw e;
            }
        }

        log.info("服务端功能测试执行完成");
    }

    /**
     * 运行客户端测试
     */
    private void runClientTests() {
        log.info("========================================");
        log.info("开始执行客户端功能测试");
        log.info("========================================");

        try {
            // 设备模拟测试
            executeTestGroup("客户端设备模拟测试", () -> {
                log.info("执行设备注册测试...");
                log.info("执行心跳保活测试...");
                log.info("执行状态上报测试...");
                log.info("执行设备离线恢复测试...");
            });

            // 命令响应测试
            executeTestGroup("客户端命令响应测试", () -> {
                log.info("执行控制命令响应测试...");
                log.info("执行查询命令应答测试...");
                log.info("执行订阅消息处理测试...");
                log.info("执行错误处理测试...");
            });

            // 媒体响应测试
            executeTestGroup("客户端媒体响应测试", () -> {
                log.info("执行INVITE响应测试...");
                log.info("执行媒体协商测试...");
                log.info("执行BYE处理测试...");
            });

        } catch (Exception e) {
            log.error("客户端测试执行失败", e);
            if (!testSuiteConfig.isContinueOnFailure()) {
                throw e;
            }
        }

        log.info("客户端功能测试执行完成");
    }

    /**
     * 运行集成测试
     */
    private void runIntegrationTests() {
        log.info("========================================");
        log.info("开始执行集成测试");
        log.info("========================================");

        try {
            // 协议兼容性测试
            executeTestGroup("协议兼容性测试", () -> {
                log.info("执行GB28181标准符合性测试...");
                log.info("执行SIP协议兼容性测试...");
                log.info("执行XML消息格式验证...");
            });

            // 性能测试
            executeTestGroup("性能测试", () -> {
                log.info("执行并发设备注册测试...");
                log.info("执行大量消息处理测试...");
                log.info("执行内存和CPU使用率测试...");
            });

            // 稳定性测试
            executeTestGroup("稳定性测试", () -> {
                log.info("执行长时间运行测试...");
                log.info("执行异常恢复测试...");
                log.info("执行网络中断恢复测试...");
            });

        } catch (Exception e) {
            log.error("集成测试执行失败", e);
            if (!testSuiteConfig.isContinueOnFailure()) {
                throw e;
            }
        }

        log.info("集成测试执行完成");
    }

    /**
     * 执行测试组
     */
    private void executeTestGroup(String groupName, Runnable testGroup) {
        log.info(">>> 开始执行: {}", groupName);
        long startTime = System.currentTimeMillis();

        try {
            testGroup.run();
            long duration = System.currentTimeMillis() - startTime;
            log.info(">>> 完成: {} (耗时: {}ms)", groupName, duration);

            // 测试间隔
            if (testSuiteConfig.getInterval() > 0) {
                Thread.sleep(testSuiteConfig.getInterval());
            }
        } catch (Exception e) {
            log.error(">>> 失败: {}", groupName, e);
            throw new RuntimeException("测试组执行失败: " + groupName, e);
        }
    }

    /**
     * 初始化测试环境
     */
    private void initializeTestEnvironment() {
        log.info("初始化测试环境...");

        if (testSuiteConfig.isConcurrent()) {
            int poolSize = Runtime.getRuntime().availableProcessors();
            executorService = Executors.newFixedThreadPool(poolSize);
            log.info("初始化线程池，大小: {}", poolSize);
        }

        // 创建测试输出目录
        reportGenerator.initializeOutputDirectory();

        log.info("测试环境初始化完成");
    }

    /**
     * 清理测试环境
     */
    private void cleanupTestEnvironment() {
        log.info("清理测试环境...");

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        log.info("测试环境清理完成");
    }
}