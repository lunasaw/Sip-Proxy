package io.github.lunasaw.gb28181.common.entity.response;

import java.util.List;

import jakarta.xml.bind.annotation.*;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.assertj.core.util.Lists;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.Getter;
import lombok.Setter;

/**
 * <?xml version="1.0"?>
 * <Response>
 *     <CmdType>Catalog</CmdType>
 *     <SN>${SN}</SN>
 *     <DeviceID>${DEVICE_ID}</DeviceID>
 *     <SumNum>1</SumNum>
 *     <DeviceList>
 *         <Item>
 *             <DeviceID>33010602011187000002</DeviceID>
 *             <Name>Camera</Name>
 *             <Manufacturer>海康</Manufacturer>
 *             <Model>Model</Model>
 *             <Owner>Owner</Owner>
 *             <CivilCode>CivilCode</CivilCode>
 *             <Block>Block</Block>
 *             <Address>上海市五角场合生汇</Address>
 *             <Parental>0</Parental>
 *             <ParentID>${DEVICE_ID}</ParentID>
 *             <SafetyWay>0</SafetyWay>
 *             <RegisterWay>1</RegisterWay>
 *             <CertNum>CertNum1</CertNum>
 *             <Certifiable>0</Certifiable>
 *             <ErrCode>400</ErrCode>
 *             <EndTime>2010-11-11T19:46:17</EndTime>
 *             <Secrecy>0</Secrecy>
 *             <IPAddress>172.19.128.50</IPAddress>
 *             <Port>5060</Port>
 *             <Password>Password1</Password>
 *             <PTZType>1</PTZType>
 *             <Status>Status1</Status>
 *             <Longitude>171.4</Longitude>
 *             <Latitude>34.2</Latitude>
 *         </Item>
 *     </DeviceList>
 * </Response>
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceResponse extends DeviceBase {

    @XmlElement(name = "SumNum")
    private int              sumNum;

    @XmlElement(name = "Item")
    @XmlElementWrapper(name = "DeviceList")
    private List<DeviceItem> deviceItemList;

    public DeviceResponse(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

    public static void main(String[] args) {
        DeviceResponse deviceResponse = new DeviceResponse();

        DeviceItem deviceItem = DeviceItem.getInstanceExample("33010602011187000001");

        deviceResponse.setDeviceItemList(Lists.newArrayList(deviceItem, deviceItem));

        System.out.println(deviceResponse);
    }
}
