package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.dromara.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 层级对象 hm_hierarchy
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hm_hierarchy")
public class Hierarchy extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 唯一id
     */
    private String uniqueKey;

    /**
     * 父级id
     */
    private Long idParent;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    @TableField("`desc`")
    private String desc;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 设置
     */
    private byte[] settings;

}
