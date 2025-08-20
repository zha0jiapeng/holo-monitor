package org.dromara.common.core.utils;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * 文件哈希工具类
 * 
 * @author Mashir0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileHashUtils {

    /**
     * 计算字节数组的MD5哈希值
     * 
     * @param data 字节数组
     * @return MD5哈希值（小写16进制字符串）
     */
    public static String calculateMD5(byte[] data) {
        try {
            return DigestUtil.md5Hex(data);
        } catch (Exception e) {
            log.error("计算MD5哈希失败", e);
            throw new RuntimeException("计算文件哈希失败: " + e.getMessage());
        }
    }

    /**
     * 计算输入流的MD5哈希值
     * 
     * @param inputStream 输入流
     * @return MD5哈希值（小写16进制字符串）
     */
    public static String calculateMD5(InputStream inputStream) {
        try {
            return DigestUtil.md5Hex(inputStream);
        } catch (Exception e) {
            log.error("计算MD5哈希失败", e);
            throw new RuntimeException("计算文件哈希失败: " + e.getMessage());
        }
    }

    /**
     * 计算文件的MD5哈希值
     * 
     * @param file 文件对象
     * @return MD5哈希值（小写16进制字符串）
     */
    public static String calculateMD5(File file) {
        try {
            return DigestUtil.md5Hex(file);
        } catch (Exception e) {
            log.error("计算文件MD5哈希失败: {}", file.getPath(), e);
            throw new RuntimeException("计算文件哈希失败: " + e.getMessage());
        }
    }

    /**
     * 计算字节数组的SHA256哈希值
     * 
     * @param data 字节数组
     * @return SHA256哈希值（小写16进制字符串）
     */
    public static String calculateSHA256(byte[] data) {
        try {
            return DigestUtil.sha256Hex(data);
        } catch (Exception e) {
            log.error("计算SHA256哈希失败", e);
            throw new RuntimeException("计算文件哈希失败: " + e.getMessage());
        }
    }

    /**
     * 计算输入流的SHA256哈希值
     * 
     * @param inputStream 输入流
     * @return SHA256哈希值（小写16进制字符串）
     */
    public static String calculateSHA256(InputStream inputStream) {
        try {
            return DigestUtil.sha256Hex(inputStream);
        } catch (Exception e) {
            log.error("计算SHA256哈希失败", e);
            throw new RuntimeException("计算文件哈希失败: " + e.getMessage());
        }
    }

    /**
     * 验证文件哈希值是否匹配
     * 
     * @param data 字节数组
     * @param expectedHash 期望的哈希值
     * @param algorithm 算法类型 (MD5/SHA256)
     * @return 是否匹配
     */
    public static boolean verifyHash(byte[] data, String expectedHash, String algorithm) {
        try {
            String actualHash;
            switch (algorithm.toUpperCase()) {
                case "MD5":
                    actualHash = calculateMD5(data);
                    break;
                case "SHA256":
                    actualHash = calculateSHA256(data);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的哈希算法: " + algorithm);
            }
            return actualHash.equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            log.error("验证文件哈希失败", e);
            return false;
        }
    }
}
