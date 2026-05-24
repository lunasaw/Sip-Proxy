package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gb28181.common.entity.control.*;
import io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control.DeviceControlRequestHandler;
import lombok.Getter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Primary
@Component
public class TestDeviceControlHandler implements DeviceControlRequestHandler {

    @Getter
    private volatile Object lastCommand;
    private volatile CountDownLatch latch;

    public void reset(CountDownLatch latch) {
        this.latch = latch;
        this.lastCommand = null;
    }

    private void signal(Object cmd) {
        this.lastCommand = cmd;
        if (latch != null) latch.countDown();
    }

    @Override
    public void handlePtzCmd(DeviceControlPtz ptzCmd) { signal(ptzCmd); }

    @Override
    public void handleTeleBoot(DeviceControlTeleBoot teleBootCmd) { signal(teleBootCmd); }

    @Override
    public void handleRecordCmd(DeviceControlRecordCmd recordCmd) { signal(recordCmd); }

    @Override
    public void handleGuardCmd(DeviceControlGuard guardCmd) { signal(guardCmd); }

    @Override
    public void handleAlarmCmd(DeviceControlAlarm alarmCmd) { signal(alarmCmd); }

    @Override
    public void handleIFameCmd(DeviceControlIFame iFameCmd) { signal(iFameCmd); }

    @Override
    public void handleDragZoomIn(DeviceControlDragIn dragInCmd) { signal(dragInCmd); }

    @Override
    public void handleDragZoomOut(DeviceControlDragOut dragOutCmd) { signal(dragOutCmd); }

    @Override
    public void handleHomePosition(DeviceControlPosition homePositionCmd) { signal(homePositionCmd); }
}
