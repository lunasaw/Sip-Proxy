package io.github.lunasaw.gbproxy.client.transmit.request.ack;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * @author luna
 * @date 2023/12/29
 */
@Component
@ConditionalOnMissingBean(name = "io.github.lunasaw.gbproxy.client.transmit.request.ack.AckRequestHandler")
public class DefaultAckRequestHandler implements AckRequestHandler {
    @Override
    public void processAck(RequestEvent evt) {

    }
}
