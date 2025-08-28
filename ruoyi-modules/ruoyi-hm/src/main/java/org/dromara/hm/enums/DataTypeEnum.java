package org.dromara.hm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据类型枚举
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum DataTypeEnum {

    /**
     * 字符串类型
     */
    STRING(1, "字符串"),

    /**
     * 整数类型
     */
    INTEGER(2, "整数"),

    /**
     * 长整数类型
     */
    LONG(3, "长整数"),

    /**
     * 浮点数类型
     */
    DOUBLE(4, "浮点数"),

    /**
     * 高精度数值类型
     */
    BIG_DECIMAL(5, "高精度数值"),

    /**
     * 布尔类型
     */
    BOOLEAN(6, "布尔"),

    /**
     * 日期时间类型
     */
    DATE_TIME(7, "日期时间"),

    /**
     * 日期类型
     */
    DATE(8, "日期"),

    /**
     * 时间类型
     */
    TIME(9, "时间"),

    /**
     * 文本类型
     */
    TEXT(10, "文本"),

    /**
     * 二进制类型
     */
    BINARY(11, "二进制"),

    /**
     * JSON类型
     */
    JSON(12, "JSON"),

    /**
     * 数组类型
     */
    ARRAY(13, "数组"),

    /**
     * 对象类型
     */
    OBJECT(14, "对象"),

    Hierarchy(15,"层级");

    /**
     * 数据类型编码
     */
    private final Integer code;

    /**
     * 数据类型名称
     */
    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 数据类型编码
     * @return 数据类型枚举，如果未找到则返回null
     */
    public static DataTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据名称获取枚举
     *
     * @param name 数据类型名称
     * @return 数据类型枚举，如果未找到则返回null
     */
    public static DataTypeEnum getByName(String name) {
        if (name == null) {
            return null;
        }
        for (DataTypeEnum type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取类型映射（用于前端下拉选择等）
     *
     * @return Map<code, name>
     */
    public static Map<Integer, String> getTypeMap() {
        return Arrays.stream(values())
            .collect(Collectors.toMap(
                DataTypeEnum::getCode,
                DataTypeEnum::getName,
                (existing, replacement) -> existing
            ));
    }

    @Override
    public String toString() {
        return String.format("DataType{code=%d, name='%s'}", code, name);
    }
}
