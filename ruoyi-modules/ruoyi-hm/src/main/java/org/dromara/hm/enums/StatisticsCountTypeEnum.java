package org.dromara.hm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统计数量类型枚举
 *
 * @author Mashir0
 * @date 2025-08-21
 */
@Getter
@AllArgsConstructor
public enum StatisticsCountTypeEnum {

    /**
     * 设备数量
     */
    EQUIPMENT(0, "设备数量"),

    /**
     * 测点数量
     */
    TESTPOINT(1, "测点数量");

    private final Integer code;
    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举
     */
    public static StatisticsCountTypeEnum getByCode(Integer code) {
        for (StatisticsCountTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据名称获取枚举
     *
     * @param name 名称
     * @return 枚举
     */
    public static StatisticsCountTypeEnum getByName(String name) {
        for (StatisticsCountTypeEnum value : values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return null;
    }
}
