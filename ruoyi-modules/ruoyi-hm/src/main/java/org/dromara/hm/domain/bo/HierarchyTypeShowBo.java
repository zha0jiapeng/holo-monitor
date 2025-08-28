package org.dromara.hm.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotNull;

/**
 * 层级类型展示业务对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HierarchyTypeShowBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 层级id
     */
    @NotNull(message = "层级id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long typeId;

    /**
     * 展示类型id
     */
    @NotNull(message = "展示类型id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long showTypeId;

}
