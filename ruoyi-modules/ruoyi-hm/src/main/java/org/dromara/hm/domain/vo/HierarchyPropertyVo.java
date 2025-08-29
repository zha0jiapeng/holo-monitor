package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.hm.domain.HierarchyProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 层级属性视图对象 hm_hierarchy_property
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = HierarchyProperty.class)
public class HierarchyPropertyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 层级id
     */
    @ExcelRequired
    @ExcelProperty(value = "层级id")
    private Long hierarchyId;

    /**
     * 属性key
     */
    @ExcelRequired
    @ExcelProperty(value = "属性key")
    private String typePropertyId;

    /**
     * 属性值
     */
    @ExcelProperty(value = "属性值")
    private String propertyValue;

    private HierarchyTypePropertyVo typeProperty;
}
