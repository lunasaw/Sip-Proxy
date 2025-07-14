package io.github.lunasaw.sip.common.test;

import com.luna.common.os.SystemInfoUtil;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.transmit.CustomerSipListener;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author luna
 * @date 2023/10/13
 */
public class ApplicationTest {

    private SipLayer sipLayer;

    @BeforeEach
    public void setUp() {
        sipLayer = new SipLayer();
        sipLayer.setSipListener(CustomerSipListener.getInstance());
        sipLayer.addListeningPoint(SystemInfoUtil.getIP(), 5060);
    }

}
