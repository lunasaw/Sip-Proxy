package io.github.lunasaw.gbproxy.client.transmit.response.register;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(name = "io.github.lunasaw.gbproxy.client.transmit.response.register.RegisterProcessorHandler")
public class DefaultRegisterProcessorHandler implements RegisterProcessorHandler {
}
