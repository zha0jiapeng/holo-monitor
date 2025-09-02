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
public enum TestpointTypeEnum {


    PARTIAL_DISCHARGE(1, "局部放电", "监测设备的局部放电信号", Set.of(11, 12, 21, 22, 23, 31, 41)),

    SF6(2, "SF6密度微水", "SF6密度微水", Set.of(51)),

    CIRCUIT_BREAKER_CHARACTERISTICS(3, "断路器特性", "断路器特性", Set.of(62)),

    GAS_LEAK(4, "气体泄露", "气体泄露", Set.of(52)),

    OTHER(-1, "未知", "其他未分类的监测类型", Set.of());


    private final Integer code;

    private final String name;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 对应的mt值集合
     */
    private final Set<Integer> mtValues;

    /**
     * 根据mt值获取对应的测点类型
     *
     * @param mt 监测类型值
     * @return 测点类型枚举，如果未找到则返回OTHER
     */
    public static TestpointTypeEnum getByMtValue(Integer mt) {
        if (mt == null) {
            return OTHER;
        }

        return Arrays.stream(values())
            .filter(typeEnum -> typeEnum.getMtValues().contains(mt))
            .findFirst()
            .orElse(OTHER);
    }

    /**
     * 根据类型编码获取枚举
     *
     * @param code 类型编码
     * @return 测点类型枚举，如果未找到则返回null
     */
    public static TestpointTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }

        return Arrays.stream(values())
            .filter(typeEnum -> typeEnum.getCode().equals(code))
            .findFirst()
            .orElse(null);
    }

    /**
     * 检查mt值是否为局部放电类型
     *
     * @param mt 监测类型值
     * @return true 如果是局部放电类型
     */
    public static boolean isPartialDischarge(Integer mt) {
        return PARTIAL_DISCHARGE.getMtValues().contains(mt);
    }

    /**
     * 获取所有局部放电的mt值
     *
     * @return 局部放电的mt值集合
     */
    public static Set<Integer> getPartialDischargeMtValues() {
        return PARTIAL_DISCHARGE.getMtValues();
    }

    /**
     * 获取类型名称列表（用于前端下拉选择等）
     *
     * @return 所有类型的名称集合
     */
    public static Set<String> getAllTypeNames() {
        return Arrays.stream(values())
            .map(TestpointTypeEnum::getName)
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
                TestpointTypeEnum::getCode,
                TestpointTypeEnum::getName,
                (existing, replacement) -> existing
            ));
    }

    @Override
    public String toString() {
        return String.format("TestpointType{code=%d, name='%s', description='%s', mtValues=%s}",
            code, name, description, mtValues);
    }
}
