package io.github.lunasaw.gbproxy.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 测试套件配置类
 * 管理测试执行的各种参数和选项
 */
@Data
@Component
@ConfigurationProperties(prefix = "test")
public class TestSuiteConfig {

    /**
     * 测试模式
     * auto: 自动化测试，执行所有测试用例
     * manual: 手动测试，提供交互式界面
     */
    private String mode = "auto";

    /**
     * 测试套件选择
     * all: 执行所有测试
     * server: 仅执行服务端测试
     * client: 仅执行客户端测试
     * integration: 仅执行集成测试
     */
    private String suite = "all";

    /**
     * 是否并发执行测试
     */
    private boolean concurrent = false;

    /**
     * 是否生成测试报告
     */
    private boolean report = true;

    /**
     * 测试超时时间（秒）
     */
    private int timeout = 30;

    /**
     * 测试重试次数
     */
    private int retryCount = 3;

    /**
     * 测试间隔时间（毫秒）
     */
    private long interval = 1000;

    /**
     * 是否在测试失败时继续执行
     */
    private boolean continueOnFailure = true;

    /**
     * 测试结果输出目录
     */
    private String outputDir = "target/test-results";

    /**
     * 是否输出详细日志
     */
    private boolean verbose = true;

    /**
     * 测试数据目录
     */
    private String testDataDir = "src/test/resources/test-data";

    /**
     * 测试场景配置目录
     */
    private String scenarioDir = "src/test/resources/test-scenarios";
}