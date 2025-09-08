package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.hm.domain.HierarchyData;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 层级数据视图对象 hm_hierarchy_data
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = HierarchyData.class)
public class HierarchyDataVo implements Serializable {

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
     * 时间
     */
    @ExcelRequired
    @ExcelProperty(value = "时间")
    private LocalDateTime time;

    /**
     * 标签
     */
    @ExcelProperty(value = "标签")
    private String tag;

    /**
     * 名称
     */
    @ExcelProperty(value = "名称")
    private String name;

    /**
     * 值
     */
    @ExcelProperty(value = "值")
    private BigDecimal value;

}
