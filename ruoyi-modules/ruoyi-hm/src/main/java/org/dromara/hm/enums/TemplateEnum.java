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


    PD("局放配置", "[{\"name\":\"magnitude\",\"value\":\"sys:cs\",\"cn\":\"离线状态\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"magnitude\",\"value\":\"mont/pd/mag\",\"cn\":\"幅值\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"magnitude\",\"value\":\"mont/pd/au\",\"cn\":\"幅值单位\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"st\",\"cn\":\"报警状态\",\"hidden\":1,\"value\":\"sys:st\",\"data_type\":\"1\"}]"),
    SF6("六氟化硫", "[{\"name\":\"magnitude\",\"value\":\"sys:cs\",\"cn\":\"离线状态\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"st\",\"cn\":\"报警状态\",\"hidden\":1,\"value\":\"sys:st\",\"data_type\":\"1\"},{\"name\":\"SF6temp\",\"value\":\"custom:SF6temp\",\"cn\":\"温度\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"SF6NormHum\",\"value\":\"custom:SF6NormHum\",\"cn\":\"带压微水\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"SF6Dens\",\"value\":\"custom:SF6Dens\",\"cn\":\"密度\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"SF6NormHum@20\",\"value\":\"custom:SF6NormHum@20\",\"cn\":\"常压微水20℃\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"SF6PressHum\",\"value\":\"custom:SF6PressHum\",\"cn\":\"常压微水\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"SF6Press\",\"value\":\"custom:SF6Press\",\"cn\":\"压力\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"SF6Dew\",\"value\":\"custom:SF6Dew\",\"cn\":\"露点\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"SF6Press@20\",\"value\":\"custom:SF6Press@20\",\"cn\":\"压力@20℃\",\"hidden\":1,\"data_type\":\"1\"},{\"name\":\"thr\",\"value\":\"custom:sys:st/thr\",\"cn\":\"状态\",\"hidden\":1,\"data_type\":\"1\"}]")

    ;


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
