package io.github.lunasaw.gbproxy.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "io.github.lunasaw.gbproxy.server",
    "io.github.lunasaw.gbproxy.client",
    "io.github.lunasaw.gb28181.common",
    "io.github.lunasaw.sip.common",
    "io.github.lunasaw.gbproxy.test"
})
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
