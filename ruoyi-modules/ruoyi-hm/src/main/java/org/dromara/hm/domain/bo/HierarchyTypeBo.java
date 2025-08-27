package org.dromara.hm.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.hm.domain.HierarchyType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 层级类型业务对象 hm_hierarchy_type
 *
 * @author Mashir0
 * @date 2024-01-01
 */

@Data
@AutoMapper(target = HierarchyType.class, reverseConvertGenerate = false)
public class HierarchyTypeBo {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 层级类型名称
     */
    @NotBlank(message = "层级类型名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 级联父级id
     */
    private Long cascadeParentId;

}
