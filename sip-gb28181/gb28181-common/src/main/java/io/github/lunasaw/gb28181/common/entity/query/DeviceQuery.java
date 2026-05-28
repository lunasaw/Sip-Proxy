package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.Getter;
import lombok.Setter;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Query>
 *   <CmdType>Catalog</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>123</DeviceID>
 * </Query>
 * @author luna
 */
@Getter
@Setter
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceQuery extends DeviceBase {


    public DeviceQuery() {
    }

    public DeviceQuery(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }


}
