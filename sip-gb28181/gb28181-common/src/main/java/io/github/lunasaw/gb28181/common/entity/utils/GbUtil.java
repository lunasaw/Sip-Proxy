package io.github.lunasaw.gb28181.common.entity.utils;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.util.StringUtils;

/**
 * @author weidian
 */
public class GbUtil {

    public static String generateGbCode(Long id) {
        return generateGbCode("127.0.0.1", id);
    }

    public static String generateGbCode(String ip, Long id) {
        if (StringUtils.isEmpty(ip) || null == id) {
            return null;
        }
        // 将nvrId转成10位数字

        return getAreaCodeByIp(ip) + String.format("%010d", id);
    }

    public static String generateGbCode(String ip, String id) {
        if (StringUtils.isEmpty(ip) || null == id) {
            return null;
        }
        // 将nvrId转成10位数字

        return getAreaCodeByIp(ip) + id;

    }

    public static String getAreaCodeByIp(String ip) {
        /**
         * 33010602 (浙江杭州西湖区) 01(社区) 118 (NVR设备) 7(internel) 000001 (设备编码)
         *
         * 33010602011187000001
         */
        return "3301060201";
    }

    public static void main(String[] args) {
        System.out.println(generateGbCode("111", 2345L));
    }

    public static String getAreaByGbCode(String GbCode) {
        return StringUtils.isEmpty(GbCode) || GbCode.length() < 10 ? null : GbCode.substring(0, 10);
    }

    /**
     * 按 GB28181 编码规范拼接 20 位国标编码：
     * 中心编码(8) + 行业编码(2) + 类型编码(3) + 序列号(7)。
     */
    public static String generateGB28181Code(int centerCode, int industryCode, int typeCode, int serialNumber) {
        String centerCodeStr = String.format("%08d", centerCode);
        String industryCodeStr = String.format("%02d", industryCode);
        String typeCodeStr = String.format("%03d", typeCode);
        String serialNumberStr = String.format("%07d", serialNumber);
        return centerCodeStr + industryCodeStr + typeCodeStr + serialNumberStr;
    }

    /**
     * 按 GB28181 规则生成 SSRC：取 userId 第 4-8 位作为前缀，再追加 4 位随机数；
     * userId 为空时返回 6 位随机数。
     */
    public static String genSsrc(String userId) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(userId)) {
            return String.valueOf(RandomUtils.nextLong(100000, 500000));
        }
        String ssrcPrefix = userId.substring(3, 8);
        return String.format("%s%04d", ssrcPrefix, RandomUtils.nextLong(1000, 9999));
    }

}
