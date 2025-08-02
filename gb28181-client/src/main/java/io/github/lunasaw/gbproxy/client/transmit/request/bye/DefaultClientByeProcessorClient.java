package io.github.lunasaw.gbproxy.client.transmit.request.bye;

import io.github.lunasaw.gbproxy.client.transmit.response.bye.ClientByeProcessorHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * @author luna
 * @date 2023/12/29
 */
@Component
@ConditionalOnMissingBean(ClientByeProcessorHandler.class)
public class DefaultClientByeProcessorClient implements ClientByeProcessorHandler {
    @Override
    public void closeStream(String callId) {

    }
}
