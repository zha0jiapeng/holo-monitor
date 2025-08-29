package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.hm.domain.HierarchyTypeProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.hm.domain.HierarchyTypePropertyDict;

import java.io.Serial;
import java.io.Serializable;

/**
 * 层级类型属性视图对象 hm_hierarchy_type_property
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = HierarchyTypeProperty.class)
public class HierarchyTypePropertyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 层级类型id
     */
    @ExcelRequired
    @ExcelProperty(value = "层级类型id")
    private Long typeId;

    /**
     * 字典id
     */
    @ExcelRequired
    @ExcelProperty(value = "字典id")
    private String propertyDictId;

    /**
     * 是否必填
     */
    @ExcelProperty(value = "是否必填")
    private Integer required;


    private HierarchyTypePropertyDictVo dict;

}
