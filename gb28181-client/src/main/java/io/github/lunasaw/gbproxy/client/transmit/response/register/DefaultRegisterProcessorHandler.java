package io.github.lunasaw.gbproxy.client.transmit.response.register;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 自定义Register处理器实现
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnMissingBean(RegisterProcessorHandler.class)
public class DefaultRegisterProcessorHandler implements RegisterProcessorHandler {

    @Override
    public void registerSuccess(String toUserId) {

    }
}
