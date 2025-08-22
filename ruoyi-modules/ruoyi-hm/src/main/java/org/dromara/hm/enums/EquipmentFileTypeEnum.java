package org.dromara.hm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备文件类型枚举
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum EquipmentFileTypeEnum {

    /**
     * SVG文件
     */
    SVG(1, "SVG"),

    /**
     * 模型文件
     */
    GLBLM(2, "GLBLM"),

    /**
     * 其他文件
     */
    OTHER(-1, "其他文件");

    private final Integer code;
    private final String name;

    /**
     * 根据code获取枚举
     */
    public static EquipmentFileTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (EquipmentFileTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据name获取枚举
     */
    public static EquipmentFileTypeEnum getByName(String name) {
        if (name == null) {
            return null;
        }
        for (EquipmentFileTypeEnum type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
