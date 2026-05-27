package io.github.lunasaw.sip.common.utils;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.ResourceUtils;

import com.google.common.base.Joiner;

import lombok.SneakyThrows;

/**
 * XML工具类，提供JAXB序列化/反序列化及GB28181 XML消息解析能力。
 */
public class XmlUtils {

    /**
     * 将对象序列化为XML字符串。
     *
     * @param charset 字符集
     * @param object  待序列化对象
     * @return XML字符串
     */
    @SneakyThrows
    public static String toString(String charset, Object object) {
        JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, charset);

        StringWriter writer = new StringWriter();
        marshaller.marshal(object, writer);
        return writer.toString();
    }

    /**
     * 将XML字符串反序列化为指定类型对象（指定字符集）。
     *
     * @param xmlStr  XML字符串
     * @param clazz   目标类型
     * @param charset 字符集
     * @return 反序列化对象
     */
    @SneakyThrows
    public static <T> Object parseObj(String xmlStr, Class<T> clazz, String charset) {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StringReader(new String(xmlStr.getBytes(charset), charset)));
    }

    /**
     * 将XML字符串反序列化为指定类型对象（UTF-8）。
     *
     * @param xmlStr XML字符串
     * @param clazz  目标类型
     * @return 反序列化对象
     */
    @SneakyThrows
    public static <T> Object parseObj(String xmlStr, Class<T> clazz) {
        return parseObj(xmlStr, clazz, "UTF-8");
    }

    /**
     * 从资源文件解析XML为指定类型对象（UTF-8）。
     *
     * @param resource 资源路径
     * @param clazz    目标类型
     * @return 反序列化对象
     */
    @SneakyThrows
    public static <T> Object parseFile(String resource, Class<T> clazz) {
        return parseFile(resource, clazz, StandardCharsets.UTF_8);
    }

    /**
     * 从资源文件解析XML为指定类型对象（指定字符集）。
     *
     * @param resource 资源路径
     * @param clazz    目标类型
     * @param charset  字符集
     * @return 反序列化对象
     */
    @SneakyThrows
    public static <T> Object parseFile(String resource, Class<T> clazz, Charset charset) {
        File file = ResourceUtils.getFile(resource);
        List<String> strings = Files.readAllLines(Paths.get(file.getAbsolutePath()), charset);

        String join = Joiner.on("\n").join(strings);
        return parseObj(join, clazz);
    }

    /**
     * 从XML字符串中提取 CmdType 元素的文本值。
     *
     * @param xmlStr XML字符串
     * @return CmdType值，不存在时返回null
     */
    @SneakyThrows
    public static String getCmdType(String xmlStr) {
        SAXReader reader = new SAXReader();

        // 清理XML字符串，移除BOM和前导空白字符
        String cleanXmlStr = cleanXmlString(xmlStr);

        Document document = reader.read(new StringReader(cleanXmlStr));
        // 获取根元素
        Element root = document.getRootElement();
        // 获取CmdType子元素
        Element cmdType = root.element("CmdType");

        if (cmdType == null) {
            return null;
        }
        return cmdType.getText();
    }

    /**
     * 清理XML字符串，移除BOM和前导/尾随空白字符
     *
     * @param xmlStr 原始XML字符串
     * @return 清理后的XML字符串
     */
    private static String cleanXmlString(String xmlStr) {
        if (xmlStr == null) {
            return null;
        }

        // 移除BOM标记 (UTF-8: EF BB BF, UTF-16BE: FE FF, UTF-16LE: FF FE)
        String cleaned = xmlStr;
        if (!cleaned.isEmpty() && cleaned.charAt(0) == '\uFEFF') {
            cleaned = cleaned.substring(1);
        }

        // 移除前导和尾随空白字符（包括空格、制表符、换行符等）
        cleaned = cleaned.trim();

        return cleaned;
    }


    /**
     * 从XML字符串中提取根元素名称。
     *
     * @param xmlStr XML字符串
     * @return 根元素名称
     */
    @SneakyThrows
    public static String getRootType(String xmlStr) {
        SAXReader reader = new SAXReader();

        // 清理XML字符串，移除BOM和前导空白字符
        String cleanXmlStr = cleanXmlString(xmlStr);

        Document document = reader.read(new StringReader(cleanXmlStr));
        // 获取根元素
        Element root = document.getRootElement();

        return root.getName();
    }
}
