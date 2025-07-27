package io.github.lunasw.gbproxy.client.test.cmd;



import io.github.lunasaw.sip.common.transmit.CustomerSipListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.lunasaw.gbproxy.client.Gb28181Client;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.transmit.event.EventResult;

import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.SneakyThrows;

/**
 * @author luna
 * @date 2023/10/13
 */
@SpringBootTest(classes = Gb28181Client.class)
public class ApplicationTest {

    static String localIp = "172.19.128.100";
    FromDevice fromDevice;
    ToDevice toDevice;
    @Autowired
    SipLayer sipLayer;

    @BeforeEach
    public void before() {
        sipLayer.addListeningPoint(localIp, 8117);
        fromDevice = FromDevice.getInstance("33010602011187000001", localIp, 8117);
        toDevice = ToDevice.getInstance("41010500002000000001", localIp, 8118);
        toDevice.setPassword("luna");
        toDevice.setRealm("4101050000");
    }

    @SneakyThrows
    @Test
    public void atest() {
        String resultCallId = ClientCommandSender.sendCommand("MESSAGE", fromDevice, toDevice, "123123");
        System.out.println("MESSAGE请求发送成功，CallId: " + resultCallId);
    }

    @SneakyThrows
    @Test
    public void register() {
        String resultCallId = ClientCommandSender.sendRegisterCommand(fromDevice, toDevice, 300);
        System.out.println("REGISTER请求发送成功，CallId: " + resultCallId);
    }

    @SneakyThrows
    @Test
    public void registerResponse() {


    }

    @AfterEach
    public void after() {
        while (true) {

        }
    }

    @Test
    public void demo() {

    }
}
