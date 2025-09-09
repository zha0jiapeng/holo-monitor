package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 层级类型属性字典视图对象 hm_hierarchy_type_property_dict
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = HierarchyTypePropertyDict.class)
public class HierarchyTypePropertyDictVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 属性字典名称
     */
    @ExcelRequired
    @ExcelProperty(value = "属性字典名称")
    private String dictName;

    /**
     * 属性类型
     */
    @ExcelProperty(value = "属性类型")
    private Integer dataType;

    private String dictValues;

    /**
     * 系统属性
     */
    @ExcelProperty(value = "系统属性")
    private Boolean systemFlag;

    /**
     * 字典key
     */
    @ExcelProperty(value = "字典key")
    private String dictKey;

}
