package org.dromara.hm.utils;

import org.dromara.hm.enums.TestpointTypeEnum;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 测点类型工具类
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@UtilityClass
public class TestpointTypeUtils {

    /**
     * 判断是否为局部放电类型的测点
     *
     * @param mt 监测类型值
     * @return true 如果是局部放电类型
     */
    public static boolean isPartialDischargeType(Integer mt) {
        return TestpointTypeEnum.isPartialDischarge(mt);
    }

    /**
     * 判断是否为局部放电类型的测点（通过type字段）
     *
     * @param type 测点类型
     * @return true 如果是局部放电类型
     */
    public static boolean isPartialDischargeTypeByCode(Integer type) {
        return TestpointTypeEnum.PARTIAL_DISCHARGE.getCode().equals(type);
    }

    /**
     * 获取测点类型描述
     *
     * @param type 测点类型编码
     * @return 类型描述
     */
    public static String getTypeDescription(Integer type) {
        TestpointTypeEnum typeEnum = TestpointTypeEnum.getByCode(type);
        return typeEnum != null ? typeEnum.getDescription() : "未知类型";
    }

    /**
     * 获取测点类型名称
     *
     * @param type 测点类型编码
     * @return 类型名称
     */
    public static String getTypeName(Integer type) {
        TestpointTypeEnum typeEnum = TestpointTypeEnum.getByCode(type);
        return typeEnum != null ? typeEnum.getName() : "未知";
    }

    /**
     * 根据mt值列表获取对应的测点类型统计
     *
     * @param mtValues mt值列表
     * @return Map<类型名称, 数量>
     */
    public static Map<String, Long> getTypeStatistics(List<Integer> mtValues) {
        return mtValues.stream()
            .map(TestpointTypeEnum::getByMtValue)
            .collect(Collectors.groupingBy(
                TestpointTypeEnum::getName,
                Collectors.counting()
            ));
    }

    /**
     * 验证mt值是否有效（是否在枚举定义的范围内）
     *
     * @param mt 监测类型值
     * @return true 如果mt值有效
     */
    public static boolean isValidMtValue(Integer mt) {
        if (mt == null) {
            return false;
        }

        TestpointTypeEnum typeEnum = TestpointTypeEnum.getByMtValue(mt);
        return !TestpointTypeEnum.OTHER.equals(typeEnum) || TestpointTypeEnum.OTHER.getMtValues().contains(mt);
    }

    /**
     * 获取类型对应的mt值范围描述
     *
     * @param type 测点类型编码
     * @return mt值范围描述
     */
    public static String getMtRangeDescription(Integer type) {
        TestpointTypeEnum typeEnum = TestpointTypeEnum.getByCode(type);
        if (typeEnum == null || typeEnum.getMtValues().isEmpty()) {
            return "无";
        }

        return typeEnum.getMtValues().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", "));
    }

    /**
     * 检查mt值变更是否需要更新type
     *
     * @param oldMt 原mt值
     * @param newMt 新mt值
     * @return true 如果需要更新type
     */
    public static boolean shouldUpdateType(Integer oldMt, Integer newMt) {
        if (oldMt == null && newMt == null) {
            return false;
        }

        if (oldMt == null || newMt == null) {
            return true;
        }

        TestpointTypeEnum oldType = TestpointTypeEnum.getByMtValue(oldMt);
        TestpointTypeEnum newType = TestpointTypeEnum.getByMtValue(newMt);

        return !oldType.equals(newType);
    }

    /**
     * 格式化测点类型信息
     *
     * @param mt 监测类型值
     * @param type 测点类型编码
     * @return 格式化的信息字符串
     */
    public static String formatTypeInfo(Integer mt, Integer type) {
        TestpointTypeEnum typeEnum = TestpointTypeEnum.getByCode(type);
        String typeName = typeEnum != null ? typeEnum.getName() : "未知";

        return String.format("类型: %s (code=%d, mt=%d)", typeName, type, mt);
    }
}
