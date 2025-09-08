package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 层级数据对象 hm_hierarchy_data
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@TableName("hm_hierarchy_data")
public class HierarchyData {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 层级id
     */
    private Long hierarchyId;

    /**
     * 时间
     */
    private LocalDateTime time;

    /**
     * 标签
     */
    private String tag;

    /**
     * 名称
     */
    private String name;

    /**
     * 值
     */
    private BigDecimal value;

}
