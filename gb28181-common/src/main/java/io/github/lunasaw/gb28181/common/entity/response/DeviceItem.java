package io.github.lunasaw.gb28181.common.entity.response;

import jakarta.xml.bind.annotation.*;

import io.github.lunasaw.gb28181.common.entity.enums.DeviceGbType;
import org.apache.commons.lang3.StringUtils;

import com.luna.common.check.Assert;
import com.luna.common.date.DateUtils;
import com.luna.common.os.SystemInfoUtil;

import lombok.Getter;
import lombok.Setter;

/**
 * toString 使用父类方法
 * @author luna
 */
@Getter
@Setter
@XmlRootElement(name = "Item")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceItem extends DeviceCatalog {

    /**
     * 业务分组
     */
    @XmlElement(name = "BusinessGroupID")
    private String  businessGroupId;
    /**
     * 警区(可选)
     */
    @XmlElement(name = "Block")
    private String block;
    /**
     * 证书序列号(有证书的设备必选)
     */
    @XmlElement(name = "CertNum")
    private String certNum;
    /**
     * 证书有效标识(有证书的设备必选) 缺省为0;证书有效标识:0:无效 1: 有效
     */
    @XmlElement(name = "Certifiable")
    private int     certifiable = 0;
    /**
     * 无效原因码(有证书且证书无效的设备必选)
     */
    @XmlElement(name = "ErrCode")
    private Integer errCode;
    /**
     * 证书终止有效期(有证书的设备必选)
     */
    @XmlElement(name = "EndTime")
    private String endTime;

    /**
     * 设备/区域/系统IP地址(可选)
     */
    @XmlElement(name = "IPAddress")
    private String ipAddress;
    /**
     * 设备/区域/系统端口(可选)
     */
    @XmlElement(name = "Port")
    private Integer port;
    /**
     * 设备口令(可选)
     */
    @XmlElement(name = "Password")
    private String password;
    /**
     * 云台类型(可选) 1-球机;2-半球;3-固定枪机;4-遥控枪机
     */
    @XmlElement(name = "PTZType")
    private Integer ptzType;
    /**
     * 经度(可选)
     */
    @XmlElement(name = "Longitude")
    private Double  longitude;
    /**
     * 纬度(可选)
     */
    @XmlElement(name = "Latitude")
    private Double  latitude;

    public static DeviceItem getInstanceExample(String deviceId) {
        Assert.notNull(deviceId, "设备ID不能为空");
        Assert.isTrue(deviceId.length() == 20, "设备ID长度必须为20位");

        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setName("Camera");
        deviceItem.setManufacturer("Lunasaw");


        String substring = deviceId.substring(10, 13);

        DeviceGbType deviceGbType = DeviceGbType.fromCode(Integer.parseInt(substring));

        deviceItem.setBlock("block");
        deviceItem.setCertifiable(0);
        deviceItem.setErrCode(500);
        deviceItem.setEndTime(DateUtils.formatTime(DateUtils.ISO8601_PATTERN, DateUtils.parseDate("2099-01-01 01:01:01")));
        deviceItem.setSecrecy(0);
        deviceItem.setSafetyWay(0);
        deviceItem.setIpAddress(SystemInfoUtil.getNoLoopbackIP());
        deviceItem.setPort(8116);
        deviceItem.setPassword("luna");
        deviceItem.setPtzType(3);
        deviceItem.setStatus("ok");
        deviceItem.setLongitude(121.472644);
        deviceItem.setLatitude(31.231706);

        if (DeviceGbType.CENTER_SERVER.equals(deviceGbType)) {
            deviceItem.setRegisterWay(1);
            deviceItem.setSecrecy(0);
        }
        if (DeviceGbType.VIRTUAL_ORGANIZATION_DIRECTORY.equals(deviceGbType)) {
            deviceItem.setParentId("0");
        } else {
            // 业务分组/虚拟组织/行政区划 不设置以下属性
            deviceItem.setModel("Model-2312");
            deviceItem.setOwner("luna");
            if (StringUtils.isNotBlank(deviceId)) {
                deviceItem.setCivilCode(deviceId.substring(0, 6));
            }
            deviceItem.setAddress("上海市xxx区xxx街道");
        }
        if (DeviceGbType.CENTER_SIGNAL_CONTROL_SERVER.equals(deviceGbType)) {
            deviceItem.setParentId("0");
            deviceItem.setBusinessGroupId("0");
        }

        return deviceItem;
    }

    public static void main(String[] args) throws Exception {

        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setDeviceId("12312312");

        System.out.println(deviceItem);
    }
}