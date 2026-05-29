package io.github.lunasaw.gb28181.common.entity.notify;

import java.util.List;

import jakarta.xml.bind.annotation.*;

import org.assertj.core.util.Lists;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Notify>
 * <CmdType>Catalog</CmdType>
 * <SN>422214</SN>
 * <DeviceID>device_001</DeviceID>
 * <SumNum>1</SumNum>
 * <DeviceList Num="1">
 * <Item>
 * <DeviceID>33010602011187000001</DeviceID>
 * <Event>Event</Event>
 * </Item>
 * </DeviceList>
 * </Notify>
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Notify")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceOtherUpdateNotify extends DeviceBase {

    @XmlElement(name = "SumNum")
    private int sumNum;

    @XmlElement(name = "Item")
    @XmlElementWrapper(name = "DeviceList")
    private List<OtherItem> deviceItemList;

    @Getter
    @Setter
    @XmlRootElement(name = "Item")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OtherItem {

        @XmlElement(name = "DeviceID")
        private String deviceId;

        @XmlElement(name = "Event")
        private String event;
    }

    public DeviceOtherUpdateNotify(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

    public static void main(String[] args) {
        DeviceOtherUpdateNotify deviceUpdateNotify = new DeviceOtherUpdateNotify();

        deviceUpdateNotify.setDeviceItemList(Lists.newArrayList());
        deviceUpdateNotify.setSumNum(1);
        System.out.println(deviceUpdateNotify);
    }

}
