package org.dromara.hm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 算法类型枚举
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum AlgorithmTypeEnum {

    /**
     * 局放定位算法
     */
    PARTIAL_DISCHARGE_LOCATION(1, "局放定位算法"),


    /**
     * 其他算法
     */
    OTHER(-1, "其他算法");

    private final Integer code;
    private final String name;

    /**
     * 根据code获取枚举
     */
    public static AlgorithmTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AlgorithmTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据name获取枚举
     */
    public static AlgorithmTypeEnum getByName(String name) {
        if (name == null) {
            return null;
        }
        for (AlgorithmTypeEnum type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
