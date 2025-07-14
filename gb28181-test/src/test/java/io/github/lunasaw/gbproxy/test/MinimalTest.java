package io.github.lunasaw.gbproxy.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 最小化测试类
 * 用于验证Spring上下文是否能正常加载
 *
 * @author luna
 * @date 2025/01/23
 */
@SpringBootTest(classes = Gb28181ApplicationTest.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "logging.level.root=WARN",
    "logging.level.io.github.lunasaw=INFO"
})
public class MinimalTest {

    @Test
    public void testContextLoads() {
        // 这个测试只验证Spring上下文是否能正常加载
        System.out.println("Spring上下文加载成功");
    }
}