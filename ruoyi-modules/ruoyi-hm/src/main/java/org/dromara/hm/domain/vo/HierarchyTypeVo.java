package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.hm.domain.HierarchyType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 层级类型视图对象 hm_hierarchy_type
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = HierarchyType.class)
public class HierarchyTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 层级类型名称
     */
    @ExcelRequired
    @ExcelProperty(value = "层级类型名称")
    private String name;

    /**
     * 级联父级id
     */
    @ExcelProperty(value = "级联父级id")
    private Long cascadeParentId;

}
