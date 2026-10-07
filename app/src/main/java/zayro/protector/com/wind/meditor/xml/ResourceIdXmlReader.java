package com.wind.meditor.xml;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Hardened ResourceIdXmlReader: works WITHOUT assets/public.xml on device.
 * Common android:* attribute ids are hardcoded; unknown names fall back to
 * the original file-based lookup.
 */
public class ResourceIdXmlReader {

    private static final Map<String, Integer> attrCachedMap = new HashMap<>();

    private static final String[][] KNOWN_ATTRS = {
            {"theme", "0x01010000"},
            {"label", "0x01010001"},
            {"icon", "0x01010002"},
            {"name", "0x01010003"},
            {"debuggable", "0x0101000f"},
            {"allowBackup", "0x01010280"},
            {"extractNativeLibs", "0x010104ea"},
            {"usesCleartextTraffic", "0x010104ec"},
            {"appComponentFactory", "0x0101057a"},
            {"versionCode", "0x0101021b"},
            {"versionName", "0x0101021c"},
            {"permission", "0x01010006"},
            {"readPermission", "0x01010007"},
            {"exported", "0x01010010"},
            {"enabled", "0x0101000e"},
            {"hardwareAccelerated", "0x010102d3"},
            {"largeHeap", "0x01010358"},
            {"supportsRtl", "0x010103af"},
            {"installLocation", "0x01010229"},
            {"compileSdkVersion", "0x01010572"},
            {"minSdkVersion", "0x0101020c"},
            {"targetSdkVersion", "0x01010270"},
    };

    static {
        for (String[] pair : KNOWN_ATTRS) {
            try {
                attrCachedMap.put(pair[0], (int) Long.parseLong(pair[1].substring(2), 16));
            } catch (Throwable ignored) {}
        }
    }

    public static int parseIdFromXml(String name) {
        if (name == null) return -1;
        Integer cached = attrCachedMap.get(name);
        if (cached != null && cached > 0) {
            return cached;
        }
        String filePath = "assets/public.xml";
        InputStream inputStream = null;
        try {
            inputStream = ResourceIdXmlReader.class.getClassLoader().getResourceAsStream(filePath);
        } catch (Throwable ignored) {}
        try {
            if (inputStream != null) {
                String id = findIdFromXmlFile(inputStream, "attr", name);
                if (id != null) {
                    int idInt = (int) Long.parseLong(id.substring(2), 16);
                    attrCachedMap.put(name, idInt);
                    return idInt;
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    private static String findIdFromXmlFile(InputStream inputStream, String type, String name) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (builder == null) return null;
            Document document = builder.parse(inputStream);
            if (document == null) return null;
            NodeList publicList = document.getElementsByTagName("public");
            if (publicList == null) return null;
            for (int i = 0; i < publicList.getLength(); i++) {
                Node node = publicList.item(i);
                NamedNodeMap attrs = node.getAttributes();
                if (attrs == null) continue;
                Node typeNode = attrs.getNamedItem("type");
                Node nameNode = attrs.getNamedItem("name");
                Node idNode = attrs.getNamedItem("id");
                if (typeNode == null || nameNode == null || idNode == null) continue;
                if (type.equals(typeNode.getNodeValue()) && name.equals(nameNode.getNodeValue())) {
                    return idNode.getNodeValue();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
