package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.test.config.TestConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * GB28181测试应用主类
 * 用于单元测试和集成测试
 *
 * @author luna
 * @date 2023/10/11
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "io.github.lunasaw.sip.common",
    "io.github.lunasaw.gbproxy"
})
@Import(TestConfiguration.class)
public class Gb28181ApplicationTest {

    public static void main(String[] args) {
        try {
            SpringApplication.run(Gb28181ApplicationTest.class, args);
        } catch (Exception e) {
            System.err.println("应用启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
