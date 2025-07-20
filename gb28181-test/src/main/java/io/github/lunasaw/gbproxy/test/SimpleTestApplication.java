package io.github.lunasaw.gbproxy.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 简化的测试应用启动类
 * 用于验证测试模块可以独立运行
 */
@Slf4j
@SpringBootApplication
public class SimpleTestApplication {

    public static void main(String[] args) {
        log.info("========================================");
        log.info("     GB28181测试模块启动验证");
        log.info("========================================");

        try {
            SpringApplication app = new SpringApplication(SimpleTestApplication.class);
            app.run(args);

            log.info("✓ GB28181测试模块启动成功！");
            log.info("✓ 所有配置和依赖加载正常");
            log.info("✓ 测试框架初始化完成");

        } catch (Exception e) {
            log.error("✗ GB28181测试模块启动失败", e);
            System.exit(1);
        }

        log.info("========================================");
        log.info("     测试模块独立运行验证完成");
        log.info("========================================");
    }
}