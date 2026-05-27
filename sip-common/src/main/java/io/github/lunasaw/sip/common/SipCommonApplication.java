package io.github.lunasaw.sip.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * sip-common 模块启动入口（仅用于独立运行/测试，正常作为库嵌入使用）。
 */
@SpringBootApplication
public class SipCommonApplication {

    public static void main(String[] args) {
        SpringApplication.run(SipCommonApplication.class, args);
    }
}