package org.dromara.hm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 测点类型枚举
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Getter
@RequiredArgsConstructor
public enum EquipmentDutEnum {

    /**
     * GIS开关
     */
    GIS_SWITCH(1, "GIS开关类", "GIS开关类", Set.of(11, 12)),

    /**
     * 变压器
     */
    TRANSFORMER(2, "变压器类", "变压器类", Set.of(21,22)),

    /**
     * 开关柜
     */
    SWITCHGEAR(3, "开关柜类", "开关柜类", Set.of(31,32)),

    /**
     * 线路
     */
    CIRCUIT(4, "线路类", "线路类", Set.of(41,42)),
    /**
     * 旋转设备
     */
    ROTATING_EQUIPMENT(5, "旋转设备类", "旋转设备类", Set.of(51,52)),

    /**
     * 位置
     */
    OTHER(-1, "未知", "其他未分类的设备类型", Set.of());

    /**
     * 类型编码
     */
    private final Integer code;

    /**
     * 类型名称
     */
    private final String name;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 对应的dut值集合
     */
    private final Set<Integer> dutValues;

    /**
     * 根据dut值获取对应的测点类型
     *
     * @param dut 监测类型值
     * @return 测点类型枚举，如果未找到则返回OTHER
     */
    public static EquipmentDutEnum getByDutValue(Integer dut) {
        if (dut == null) {
            return OTHER;
        }

        return Arrays.stream(values())
            .filter(typeEnum -> typeEnum.getDutValues().contains(dut))
            .findFirst()
            .orElse(OTHER);
    }

    /**
     * 根据类型编码获取枚举
     *
     * @param code 类型编码
     * @return 测点类型枚举，如果未找到则返回null
     */
    public static EquipmentDutEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }

        return Arrays.stream(values())
            .filter(typeEnum -> typeEnum.getCode().equals(code))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取类型名称列表（用于前端下拉选择等）
     *
     * @return 所有类型的名称集合
     */
    public static Set<String> getAllTypeNames() {
        return Arrays.stream(values())
            .map(EquipmentDutEnum::getName)
            .collect(Collectors.toSet());
    }

    /**
     * 获取类型映射（用于前端下拉选择等）
     *
     * @return Map<code, name>
     */
    public static Map<Integer, String> getTypeMap() {
        return Arrays.stream(values())
            .collect(Collectors.toMap(
                EquipmentDutEnum::getCode,
                EquipmentDutEnum::getName,
                (existing, replacement) -> existing
            ));
    }

    @Override
    public String toString() {
        return String.format("TestPointType{code=%d, name='%s', description='%s', mtValues=%s}",
            code, name, description, dutValues);
    }
}
