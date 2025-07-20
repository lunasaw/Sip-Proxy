package io.github.lunasaw.gbproxy.test.runner;

import io.github.lunasaw.gbproxy.test.config.TestSuiteConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试报告生成器
 * 负责生成详细的测试执行报告
 */
@Slf4j
@Component
public class TestReportGenerator {

    @Autowired
    private TestSuiteConfig testSuiteConfig;

    @Autowired
    private TestMetricsCollector metricsCollector;

    private List<TestResult> testResults = new ArrayList<>();

    /**
     * 初始化输出目录
     */
    public void initializeOutputDirectory() {
        File outputDir = new File(testSuiteConfig.getOutputDir());
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (created) {
                log.info("创建测试输出目录: {}", outputDir.getAbsolutePath());
            } else {
                log.warn("无法创建测试输出目录: {}", outputDir.getAbsolutePath());
            }
        }
    }

    /**
     * 添加测试结果
     */
    public void addTestResult(String testName, String testGroup, boolean success,
                              long duration, String details, Throwable error) {
        TestResult result = new TestResult();
        result.setTestName(testName);
        result.setTestGroup(testGroup);
        result.setSuccess(success);
        result.setDuration(duration);
        result.setDetails(details);
        result.setError(error);
        result.setTimestamp(LocalDateTime.now());

        testResults.add(result);

        if (testSuiteConfig.isVerbose()) {
            log.info("测试结果: {} - {} - {} ({}ms)",
                    testGroup, testName, success ? "成功" : "失败", duration);
        }
    }

    /**
     * 生成测试报告
     */
    public void generateReport(String sessionId) {
        log.info("开始生成测试报告...");

        try {
            // 生成HTML报告
            generateHtmlReport(sessionId);

            // 生成JSON报告
            generateJsonReport(sessionId);

            // 生成控制台摘要
            generateConsoleSummary();

            log.info("测试报告生成完成，输出目录: {}", testSuiteConfig.getOutputDir());

        } catch (Exception e) {
            log.error("生成测试报告失败", e);
        }
    }

    /**
     * 生成HTML报告
     */
    private void generateHtmlReport(String sessionId) throws IOException {
        String fileName = String.format("%s/test-report-%s.html",
                testSuiteConfig.getOutputDir(), sessionId);

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(generateHtmlContent(sessionId));
        }

        log.info("HTML测试报告已生成: {}", fileName);
    }

    /**
     * 生成HTML内容
     */
    private String generateHtmlContent(String sessionId) {
        StringBuilder html = new StringBuilder();

        // HTML头部
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='zh-CN'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <title>GB28181测试报告 - ").append(sessionId).append("</title>\n");
        html.append("    <style>\n");
        html.append(getHtmlStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // 报告头部
        html.append("    <h1>GB28181功能测试报告</h1>\n");
        html.append("    <div class='summary'>\n");
        html.append("        <h2>测试摘要</h2>\n");
        html.append("        <table>\n");
        html.append("            <tr><td>会话ID:</td><td>").append(sessionId).append("</td></tr>\n");
        html.append("            <tr><td>测试时间:</td><td>").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</td></tr>\n");
        html.append("            <tr><td>测试模式:</td><td>").append(testSuiteConfig.getMode()).append("</td></tr>\n");
        html.append("            <tr><td>测试套件:</td><td>").append(testSuiteConfig.getSuite()).append("</td></tr>\n");
        html.append("            <tr><td>总测试数:</td><td>").append(testResults.size()).append("</td></tr>\n");
        html.append("            <tr><td>成功数:</td><td class='success'>").append(getSuccessCount()).append("</td></tr>\n");
        html.append("            <tr><td>失败数:</td><td class='failure'>").append(getFailureCount()).append("</td></tr>\n");
        html.append("            <tr><td>成功率:</td><td>").append(String.format("%.2f%%", getSuccessRate())).append("</td></tr>\n");
        html.append("        </table>\n");
        html.append("    </div>\n");

        // 测试结果详情
        html.append("    <div class='details'>\n");
        html.append("        <h2>测试结果详情</h2>\n");
        html.append("        <table>\n");
        html.append("            <thead>\n");
        html.append("                <tr>\n");
        html.append("                    <th>测试组</th>\n");
        html.append("                    <th>测试名称</th>\n");
        html.append("                    <th>结果</th>\n");
        html.append("                    <th>耗时(ms)</th>\n");
        html.append("                    <th>时间</th>\n");
        html.append("                    <th>详情</th>\n");
        html.append("                </tr>\n");
        html.append("            </thead>\n");
        html.append("            <tbody>\n");

        for (TestResult result : testResults) {
            html.append("                <tr class='").append(result.isSuccess() ? "success" : "failure").append("'>\n");
            html.append("                    <td>").append(result.getTestGroup()).append("</td>\n");
            html.append("                    <td>").append(result.getTestName()).append("</td>\n");
            html.append("                    <td>").append(result.isSuccess() ? "成功" : "失败").append("</td>\n");
            html.append("                    <td>").append(result.getDuration()).append("</td>\n");
            html.append("                    <td>").append(result.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</td>\n");
            html.append("                    <td>").append(result.getDetails() != null ? result.getDetails() : "").append("</td>\n");
            html.append("                </tr>\n");
        }

        html.append("            </tbody>\n");
        html.append("        </table>\n");
        html.append("    </div>\n");

        // 性能指标
        if (metricsCollector.hasMetrics()) {
            html.append("    <div class='metrics'>\n");
            html.append("        <h2>性能指标</h2>\n");
            html.append("        <div class='metrics-content'>\n");
            html.append(metricsCollector.getMetricsHtml());
            html.append("        </div>\n");
            html.append("    </div>\n");
        }

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * 获取HTML样式
     */
    private String getHtmlStyles() {
        return "body { font-family: Arial, sans-serif; margin: 20px; }\n" +
                "h1 { color: #333; text-align: center; }\n" +
                "h2 { color: #666; border-bottom: 2px solid #eee; padding-bottom: 5px; }\n" +
                ".summary table, .details table { width: 100%; border-collapse: collapse; margin: 10px 0; }\n" +
                ".summary td, .details th, .details td { padding: 8px; border: 1px solid #ddd; text-align: left; }\n" +
                ".summary td:first-child { font-weight: bold; background-color: #f5f5f5; width: 120px; }\n" +
                ".details th { background-color: #f5f5f5; font-weight: bold; }\n" +
                ".success { color: #28a745; }\n" +
                ".failure { color: #dc3545; }\n" +
                "tr.success { background-color: #d4edda; }\n" +
                "tr.failure { background-color: #f8d7da; }\n" +
                ".metrics { margin-top: 30px; }\n" +
                ".metrics-content { background-color: #f8f9fa; padding: 15px; border-radius: 5px; }";
    }

    /**
     * 生成JSON报告
     */
    private void generateJsonReport(String sessionId) throws IOException {
        String fileName = String.format("%s/test-report-%s.json",
                testSuiteConfig.getOutputDir(), sessionId);

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"sessionId\": \"").append(sessionId).append("\",\n");
        json.append("  \"timestamp\": \"").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        json.append("  \"config\": {\n");
        json.append("    \"mode\": \"").append(testSuiteConfig.getMode()).append("\",\n");
        json.append("    \"suite\": \"").append(testSuiteConfig.getSuite()).append("\",\n");
        json.append("    \"concurrent\": ").append(testSuiteConfig.isConcurrent()).append("\n");
        json.append("  },\n");
        json.append("  \"summary\": {\n");
        json.append("    \"total\": ").append(testResults.size()).append(",\n");
        json.append("    \"success\": ").append(getSuccessCount()).append(",\n");
        json.append("    \"failure\": ").append(getFailureCount()).append(",\n");
        json.append("    \"successRate\": ").append(String.format("%.2f", getSuccessRate())).append("\n");
        json.append("  },\n");
        json.append("  \"results\": [\n");

        for (int i = 0; i < testResults.size(); i++) {
            TestResult result = testResults.get(i);
            json.append("    {\n");
            json.append("      \"testGroup\": \"").append(result.getTestGroup()).append("\",\n");
            json.append("      \"testName\": \"").append(result.getTestName()).append("\",\n");
            json.append("      \"success\": ").append(result.isSuccess()).append(",\n");
            json.append("      \"duration\": ").append(result.getDuration()).append(",\n");
            json.append("      \"timestamp\": \"").append(result.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\"\n");
            json.append("    }");
            if (i < testResults.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(json.toString());
        }

        log.info("JSON测试报告已生成: {}", fileName);
    }

    /**
     * 生成控制台摘要
     */
    private void generateConsoleSummary() {
        log.info("========================================");
        log.info("          测试执行摘要");
        log.info("========================================");
        log.info("总测试数: {}", testResults.size());
        log.info("成功数: {}", getSuccessCount());
        log.info("失败数: {}", getFailureCount());
        log.info("成功率: {:.2f}%", getSuccessRate());

        if (getFailureCount() > 0) {
            log.info("========================================");
            log.info("          失败测试详情");
            log.info("========================================");
            testResults.stream()
                    .filter(result -> !result.isSuccess())
                    .forEach(result -> log.error("失败: {} - {} ({}ms)",
                            result.getTestGroup(), result.getTestName(), result.getDuration()));
        }

        log.info("========================================");
    }

    /**
     * 获取成功测试数量
     */
    private long getSuccessCount() {
        return testResults.stream().mapToLong(result -> result.isSuccess() ? 1 : 0).sum();
    }

    /**
     * 获取失败测试数量
     */
    private long getFailureCount() {
        return testResults.stream().mapToLong(result -> result.isSuccess() ? 0 : 1).sum();
    }

    /**
     * 获取成功率
     */
    private double getSuccessRate() {
        if (testResults.isEmpty()) {
            return 0.0;
        }
        return (double) getSuccessCount() / testResults.size() * 100;
    }

    /**
     * 测试结果数据类
     */
    @Data
    public static class TestResult {
        private String testName;
        private String testGroup;
        private boolean success;
        private long duration;
        private String details;
        private Throwable error;
        private LocalDateTime timestamp;
    }
}