package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;

/**
 * 层级类型属性字典对象 hm_hierarchy_type_property_dict
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@TableName("hm_hierarchy_type_property_dict")
public class HierarchyTypePropertyDict {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 属性字典名称
     */
    private String dictName;

    /**
     * 属性类型
     */
    private Integer dataType;

}
