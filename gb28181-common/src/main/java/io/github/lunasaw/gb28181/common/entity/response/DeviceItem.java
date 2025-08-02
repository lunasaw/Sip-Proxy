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

    /**
     * 摄像机位置类型扩展(可选)
     * 1-省际检查站、2-党政机关、3-车站码头、4-中心广场、5-体育场馆、6-商业中心、
     * 7-宗教场所、8-校园周边、9-治安复杂区域、10-交通干线。当目录项为摄像机时可选。
     */
    @XmlElement(name = "PositionType")
    private Integer positionType;

    /**
     * 摄像机安装位置室外、室内属性(可选)
     * 1-室外、2-室内。当目录项为摄像机时可选，缺省为1。
     */
    @XmlElement(name = "RoomType")
    private Integer roomType;

    /**
     * 摄像机用途属性(可选)
     * 1-治安、2-交通、3-重点。当目录项为摄像机时可选。
     */
    @XmlElement(name = "UseType")
    private Integer useType;

    /**
     * 摄像机补光属性(可选)
     * 1-无补光、2-红外补光、3-白光补光。当目录项为摄像机时可选，缺省为1。
     */
    @XmlElement(name = "SupplyLightType")
    private Integer supplyLightType;

    /**
     * 摄像机监视方位属性(可选)
     * 1-东、2-西、3-南、4-北、5-东南、6-东北、7-西南、8-西北。
     * 当目录项为摄像机时且为固定摄像机或设置看守位摄像机时可选。
     */
    @XmlElement(name = "DirectionType")
    private Integer directionType;

    /**
     * 摄像机支持的分辨率(可选)
     * 格式应符合"幅面*高度[/帧率]"格式。当目录项为摄像机时可选。
     */
    @XmlElement(name = "Resolution")
    private String resolution;


    /**
     * 下载倍数(可选)
     * 可选项，取值0为不下载；其他值表示下载的倍数。
     */
    @XmlElement(name = "DownloadSpeed")
    private String downloadSpeed;

    /**
     * SVC时域可支持的空域层数(可选)
     * 可选项，缺省为空。
     */
    @XmlElement(name = "SVCSpaceSupportMode")
    private Integer svcSpaceSupportMode;

    /**
     * SVC时域可支持的时域层数(可选)
     * 可选项，缺省为空。
     */
    @XmlElement(name = "SVCTimeSupportMode")
    private Integer svcTimeSupportMode;

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