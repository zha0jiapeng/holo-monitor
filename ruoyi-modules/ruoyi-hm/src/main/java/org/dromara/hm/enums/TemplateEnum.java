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
public enum TemplateEnum {


    PD("局放配置", "[{\"name\":\"uhf_ignore_threshold\",\"cn\":\"特高频忽略阈值\",\"value\":\"-20.00\",\"data_type\":\"5\"},{\"name\":\"uhf_mutation_threshold\",\"cn\":\"特高频突变阈值\",\"value\":\"-20.00\",\"data_type\":\"5\"},{\"name\":\"uhf_level1_alarm_threshold\",\"cn\":\"特高频1级报警阈值\",\"value\":\"-20.00\",\"data_type\":\"5\"},{\"name\":\"uhf_level2_alarm_threshold\",\"cn\":\"特高频2级报警阈值\",\"value\":\"-40.00\",\"data_type\":\"5\"},{\"name\":\"uhf_level3_alarm_threshold\",\"cn\":\"特高频3级报警阈值\",\"value\":\"-60.00\",\"data_type\":\"5\"},{\"name\":\"discharge_event_ratio_threshold\",\"cn\":\"放电事件数比例阈值\",\"value\":\"0.25\",\"data_type\":\"5\"},{\"name\":\"event_count_threshold_period\",\"cn\":\"事件数阈值周期\",\"value\":\"24\",\"data_type\":\"2\"},{\"name\":\"alarm_reset_delay\",\"cn\":\"报警复位延时\",\"value\":\"4\",\"data_type\":\"2\"},{\"name\":\"offline_judgment_threshold\",\"cn\":\"离线判断\",\"value\":\"24\",\"data_type\":\"2\"},{\"name\":\"magnitude\",\"value\":\"/mont/pd/mag\",\"cn\":\"幅值\",\"data_type\":\"1\"},{\"name\":\"st\",\"cn\":\"报警状态\",\"value\":\"/sys/st\",\"data_type\":\"1\"},{\"name\":\"pdtype_platform\",\"cn\":\"平台诊断结果\",\"value\":\"custom\",\"data_type\":\"5\"}]");


    private final String name;

    /**
     * 数据类型名称
     */
    private final String Template;

    /**
     * 根据名称获取枚举
     *
     * @param name 数据类型名称
     * @return 数据类型枚举，如果未找到则返回null
     */
    public static TemplateEnum getByName(String name) {
        if (name == null) {
            return null;
        }
        for (TemplateEnum type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取类型映射（用于前端下拉选择等）
     *
     * @return Map<name, Template>
     */
    public static Map<String, String> getTypeMap() {
        return Arrays.stream(values())
            .collect(Collectors.toMap(
                TemplateEnum::getName,
                TemplateEnum::getTemplate,
                (existing, replacement) -> existing
            ));
    }

    @Override
    public String toString() {
        return String.format("DataType{name='%s', Template='%s'}", name, Template);
    }
}
