package io.github.lunasaw.gbproxy.client.transmit.response.cancel;

import javax.sip.ResponseEvent;

import lombok.Data;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.transmit.event.response.AbstractSipResponseProcessor;

/**
 * CANCEL响应处理器
 *
 * @author luna
 */
@Data
@Component
public class CancelResponseProcessorSipResponseProcessor extends AbstractSipResponseProcessor {

    public static final String METHOD = "CANCEL";

    private String method = METHOD;

    /**
     * 处理CANCEL响应
     *
     * @param evt
     */
    @Override
    public void process(ResponseEvent evt) {

    }

}
