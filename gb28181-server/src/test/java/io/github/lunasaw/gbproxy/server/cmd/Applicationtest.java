package io.github.lunasaw.gbproxy.server.cmd;

import io.github.lunasaw.gbproxy.server.Gb28181Server;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerSendCmd;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author luna
 * @date 2023/10/14
 */
@SpringBootTest(classes = Gb28181Server.class)
public class Applicationtest {


    FromDevice fromDevice;

    ToDevice   toDevice;

    static String localIp = "172.19.128.100";

    @Autowired
    SipLayer sipLayer;

    @BeforeEach
    public void before() {
        sipLayer.addListeningPoint(localIp, 8117);
        fromDevice = FromDevice.getInstance("41010500002000000001", localIp, 8117);
        toDevice = ToDevice.getInstance("33010602011187000001", localIp, 8118);
        toDevice.setPassword("luna");
        toDevice.setRealm("4101050000");
    }

    @Test
    public void test_device_info() {
        String infoQueryCallId = ServerSendCmd.deviceInfo(fromDevice, toDevice);
        System.out.println(infoQueryCallId);
    }

    @Test
    public void test_bye() {
        ServerSendCmd.deviceBye(fromDevice, toDevice);
    }
}
