package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;

/**
 * 层级对象 hm_hierarchy
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@TableName("hm_hierarchy")
public class Hierarchy {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 层级类型id
     */
    private Long typeId;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 层级名称
     */
    private String name;

    /**
     * 层级编码
     */
    private String code;

}
