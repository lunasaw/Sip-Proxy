package io.github.lunasaw.gbproxy.client.transmit.response.register;

public interface RegisterProcessorHandler {

    default Integer getExpire(String userId) {
        return 3600;
    }
}
