package org.dromara.hm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报警类型枚举
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Getter
@RequiredArgsConstructor
public enum AlarmTypeEnum {

    /**
     * 正常
     */
    NORMAL(0, "正常", "设备运行正常，无报警"),

    /**
     * 局放事件
     */
    PARTIAL_DISCHARGE_EVENT(1, "局放事件", "检测到局部放电事件"),

    /**
     * 突变报警
     */
    MUTATION_ALARM(2, "突变报警", "检测到设备参数突变"),

    /**
     * 持续性3级报警
     */
    PERSISTENT_LEVEL3_ALARM(3, "持续性3级报警", "持续性三级报警状态"),

    /**
     * 持续性2级报警
     */
    PERSISTENT_LEVEL2_ALARM(4, "持续性2级报警", "持续性二级报警状态"),

    /**
     * 持续性1级报警
     */
    PERSISTENT_LEVEL1_ALARM(5, "持续性1级报警", "持续性一级报警状态");

    /**
     * 报警类型编码
     */
    private final Integer code;

    /**
     * 报警类型名称
     */
    private final String name;

    /**
     * 报警类型描述
     */
    private final String description;

    /**
     * 根据报警类型编码获取枚举
     *
     * @param code 报警类型编码
     * @return 报警类型枚举，如果未找到则返回null
     */
    public static AlarmTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }

        return Arrays.stream(values())
            .filter(alarmType -> alarmType.getCode().equals(code))
            .findFirst()
            .orElse(null);
    }

    /**
     * 检查是否为正常状态
     *
     * @param code 报警类型编码
     * @return true 如果是正常状态
     */
    public static boolean isNormal(Integer code) {
        return NORMAL.getCode().equals(code);
    }

    /**
     * 检查是否为报警状态
     *
     * @param code 报警类型编码
     * @return true 如果是报警状态
     */
    public static boolean isAlarm(Integer code) {
        return code != null && code > 0;
    }

    /**
     * 获取报警级别（仅适用于持续性报警）
     *
     * @param code 报警类型编码
     * @return 报警级别，如果不是持续性报警则返回null
     */
    public static Integer getAlarmLevel(Integer code) {
        if (code == null) {
            return null;
        }
        
        return switch (code) {
            case 3 -> 3; // 持续性3级报警
            case 4 -> 2; // 持续性2级报警
            case 5 -> 1; // 持续性1级报警
            default -> null;
        };
    }

    /**
     * 获取类型映射（用于前端下拉选择等）
     *
     * @return Map<code, name>
     */
    public static Map<Integer, String> getTypeMap() {
        return Arrays.stream(values())
            .collect(Collectors.toMap(
                AlarmTypeEnum::getCode,
                AlarmTypeEnum::getName,
                (existing, replacement) -> existing
            ));
    }

    @Override
    public String toString() {
        return String.format("AlarmType{code=%d, name='%s', description='%s'}",
            code, name, description);
    }
}
