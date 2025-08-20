package org.dromara.hm.utils;

import org.dromara.hm.enums.TestPointTypeEnum;
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
public class TestPointTypeUtils {

    /**
     * 判断是否为局部放电类型的测点
     *
     * @param mt 监测类型值
     * @return true 如果是局部放电类型
     */
    public static boolean isPartialDischargeType(Integer mt) {
        return TestPointTypeEnum.isPartialDischarge(mt);
    }

    /**
     * 判断是否为局部放电类型的测点（通过type字段）
     *
     * @param type 测点类型
     * @return true 如果是局部放电类型
     */
    public static boolean isPartialDischargeTypeByCode(Integer type) {
        return TestPointTypeEnum.PARTIAL_DISCHARGE.getCode().equals(type);
    }

    /**
     * 获取测点类型描述
     *
     * @param type 测点类型编码
     * @return 类型描述
     */
    public static String getTypeDescription(Integer type) {
        TestPointTypeEnum typeEnum = TestPointTypeEnum.getByCode(type);
        return typeEnum != null ? typeEnum.getDescription() : "未知类型";
    }

    /**
     * 获取测点类型名称
     *
     * @param type 测点类型编码
     * @return 类型名称
     */
    public static String getTypeName(Integer type) {
        TestPointTypeEnum typeEnum = TestPointTypeEnum.getByCode(type);
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
            .map(TestPointTypeEnum::getByMtValue)
            .collect(Collectors.groupingBy(
                TestPointTypeEnum::getName,
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
        
        TestPointTypeEnum typeEnum = TestPointTypeEnum.getByMtValue(mt);
        return !TestPointTypeEnum.OTHER.equals(typeEnum) || TestPointTypeEnum.OTHER.getMtValues().contains(mt);
    }

    /**
     * 获取类型对应的mt值范围描述
     *
     * @param type 测点类型编码
     * @return mt值范围描述
     */
    public static String getMtRangeDescription(Integer type) {
        TestPointTypeEnum typeEnum = TestPointTypeEnum.getByCode(type);
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
        
        TestPointTypeEnum oldType = TestPointTypeEnum.getByMtValue(oldMt);
        TestPointTypeEnum newType = TestPointTypeEnum.getByMtValue(newMt);
        
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
        TestPointTypeEnum typeEnum = TestPointTypeEnum.getByCode(type);
        String typeName = typeEnum != null ? typeEnum.getName() : "未知";
        
        return String.format("类型: %s (code=%d, mt=%d)", typeName, type, mt);
    }
}
