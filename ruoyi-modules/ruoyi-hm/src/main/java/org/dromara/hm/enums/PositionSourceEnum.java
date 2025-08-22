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
public enum PositionSourceEnum {

    /**
     * SD400MP
     */
    SD400MP(0, "SD400MP同步"),

    /**
     * 4D
     */
    LOCAL(1, "4D定位");

    private final Integer code;
    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举
     */
    public static PositionSourceEnum getByCode(Integer code) {
        for (PositionSourceEnum value : values()) {
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
    public static PositionSourceEnum getByName(String name) {
        for (PositionSourceEnum value : values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return null;
    }
}
