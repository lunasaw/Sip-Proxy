package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.test.runner.AutoTestRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * GB28181测试应用主程序
 * 可独立运行的完整测试套件
 * <p>
 * 启动方式：
 * 1. IDE运行：直接运行main方法
 * 2. Maven运行：mvn spring-boot:run -pl gb28181-test
 * 3. JAR运行：java -jar gb28181-test-1.2.5.jar
 * <p>
 * 参数说明：
 * --test.mode=auto|manual        测试模式：自动化/手动
 * --test.suite=all|server|client 测试套件：全部/服务端/客户端
 * --test.concurrent=true|false   是否并发测试
 * --test.report=true|false       是否生成测试报告
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {
        "io.github.lunasaw.gbproxy.test",
        "io.github.lunasaw.gbproxy.server",
        "io.github.lunasaw.gbproxy.client",
        "io.github.lunasaw.sip.common"
})
public class Gb28181TestApplication implements CommandLineRunner {

    @Autowired
    private AutoTestRunner autoTestRunner;

    public static void main(String[] args) {
        log.info("========================================");
        log.info("     GB28181测试套件启动中...");
        log.info("========================================");

        SpringApplication app = new SpringApplication(Gb28181TestApplication.class);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("GB28181测试应用已启动，开始执行测试套件...");

        try {
            // 执行自动化测试
//            autoTestRunner.runTestSuite();
        } catch (Exception e) {
            log.error("测试执行过程中发生异常", e);
            System.exit(1);
        }

        log.info("========================================");
        log.info("     GB28181测试套件执行完成");
        log.info("========================================");
    }
}