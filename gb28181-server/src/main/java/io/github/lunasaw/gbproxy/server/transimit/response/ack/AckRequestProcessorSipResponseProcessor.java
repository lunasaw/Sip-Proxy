package io.github.lunasaw.gbproxy.server.transimit.response.ack;

import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;

/**
 * SIP命令类型： 收到ACK请求*
 *
 * @author luna
 */
@Component
@Getter
@Setter
public class AckRequestProcessorSipResponseProcessor extends AbstractSipResponseProcessor {

    public static final String METHOD = "ACK";

    private String method = METHOD;

    /**
     * 收到ACK响应处理
     *
     * @param evt
     */
    @Override
    public void process(ResponseEvent evt) {

    }
}
