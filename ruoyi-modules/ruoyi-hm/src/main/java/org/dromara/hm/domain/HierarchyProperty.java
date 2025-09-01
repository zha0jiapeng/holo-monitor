package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;

/**
 * 层级属性对象 hm_hierarchy_property
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@TableName("hm_hierarchy_property")
public class HierarchyProperty {

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
     * 属性key
     */
    private Long typePropertyId;

    /**
     * 属性值
     */
    private String propertyValue;

}
