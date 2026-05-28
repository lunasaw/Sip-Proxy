package io.github.lunasaw.gbproxy.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * GB28181 设备客户端 Spring Boot 启动入口（仅用于独立运行测试，生产环境通过 {@code @EnableSipClient} 嵌入）。
 *
 * @author luna
 */
@SpringBootApplication
public class Gb28181Client {

    /**
     * 应用启动入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Gb28181Client.class, args);
    }
}
