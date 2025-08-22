package org.dromara.hm.utils;

import cn.hutool.core.io.IoUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZdsUtils {

    public static Map<String, byte[]> unzip(byte[] zipData) {
        Map<String, byte[]> unzippedFiles = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    unzippedFiles.put(zipEntry.getName(), IoUtil.readBytes(zis, false));
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return unzippedFiles;
    }
}
