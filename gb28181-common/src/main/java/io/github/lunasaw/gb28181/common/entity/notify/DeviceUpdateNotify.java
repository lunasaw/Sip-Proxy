package io.github.lunasaw.gb28181.common.entity.notify;

import jakarta.xml.bind.annotation.*;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.assertj.core.util.Lists;

import java.util.List;

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
 * <Name>Channel 1</Name>
 * <ParentID></ParentID>
 * <Parental>0</Parental>
 * <Manufacturer>ABC Inc.</Manufacturer>
 * <Secrecy></Secrecy>
 * <RegisterWay>1</RegisterWay>
 * <Status>ON</Status>
 * <Model>Model 123</Model>
 * <Owner> John Doe</Owner>
 * <CivilCode>123456</CivilCode>
 * <Address>123 Main St.</Address>
 * <Event>Event</Event>
 * </Item>
 * </DeviceList>
 * </Notify>

 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Notify")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceUpdateNotify extends DeviceBase {

    @XmlElement(name = "SumNum")
    private int              sumNum;

    @XmlElement(name = "Item")
    @XmlElementWrapper(name = "DeviceList")
    private List<DeviceUpdateItem> deviceItemList;

    public DeviceUpdateNotify(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

    public static void main(String[] args) {
        DeviceUpdateNotify deviceUpdateNotify = new DeviceUpdateNotify();
        deviceUpdateNotify.setDeviceId("123123");
        DeviceUpdateItem deviceUpdateItem = new DeviceUpdateItem();
        deviceUpdateItem.setDeviceId("33010602011187000001");
        deviceUpdateItem.setName("Channel 1");
        deviceUpdateItem.setParentId(null);
        deviceUpdateItem.setParental(0);
        deviceUpdateItem.setManufacturer("ABC Inc.");
        deviceUpdateItem.setSecrecy(0);
        deviceUpdateItem.setRegisterWay(1);
        deviceUpdateItem.setStatus("ON");
        deviceUpdateItem.setModel("model");
        deviceUpdateItem.setOwner("John Dow");
        deviceUpdateItem.setCivilCode("123456");
        deviceUpdateItem.setEvent("event");

        deviceUpdateNotify.setDeviceItemList(Lists.newArrayList(deviceUpdateItem));
        deviceUpdateNotify.setSumNum(1);
        System.out.println(deviceUpdateNotify);
    }

}
