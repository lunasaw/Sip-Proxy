package io.github.lunasaw.gb28181.common.entity.notify;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Notify>
 * <CmdType>Keepalive</CmdType>
 * <SN>340917</SN>
 * <DeviceID>parentPlatform.getDeviceGBId()</DeviceID>
 * <Status>OK</Status>
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
public class DeviceKeepLiveNotify extends DeviceBase {


    @XmlElement(name = "Status")
    public String status;


    public DeviceKeepLiveNotify(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

    public static void main(String[] args) {
        DeviceKeepLiveNotify deviceKeepLiveNotify = new DeviceKeepLiveNotify();
        deviceKeepLiveNotify.setStatus("OK");
        System.out.println(deviceKeepLiveNotify);
    }
}
