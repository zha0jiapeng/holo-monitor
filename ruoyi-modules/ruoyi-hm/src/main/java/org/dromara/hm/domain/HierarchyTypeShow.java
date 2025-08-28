package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;

/**
 * 层级类型展示对象 hm_hierarchy_type_show
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@TableName("hm_hierarchy_type_show")
public class HierarchyTypeShow {

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
    private Long typeId;

    /**
     * 展示类型id
     */
    private Long showTypeId;

}
