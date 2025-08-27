package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;

/**
 * 层级类型对象 hm_hierarchy_type
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@TableName("hm_hierarchy_type")
public class HierarchyType {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;


    /**
     * 层级类型名称
     */
    private String name;

    /**
     * 级联父级id
     */
    private Long cascadeParentId;

}
