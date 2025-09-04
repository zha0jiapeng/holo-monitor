package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.hm.domain.Hierarchy;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

// 添加导入
import java.util.List;

/**
 * 层级视图对象 hm_hierarchy
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = Hierarchy.class)
public class HierarchyVo implements Serializable {

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
     * 父级ID
     */
    @ExcelProperty(value = "父级ID")
    private Long parentId;

    /**
     * 层级名称
     */
    @ExcelRequired
    @ExcelProperty(value = "层级名称")
    private String name;

    /**
     * 层级编码
     */
    @ExcelProperty(value = "层级编码")
    private String code;

    // 添加字段
    private List<HierarchyPropertyVo> properties;

}
