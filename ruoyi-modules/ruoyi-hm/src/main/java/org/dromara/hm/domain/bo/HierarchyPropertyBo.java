package org.dromara.hm.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.hm.domain.HierarchyProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 层级属性业务对象 hm_hierarchy_property
 *
 * @author Mashir0
 * @date 2024-01-01
 */

@Data
@AutoMapper(target = HierarchyProperty.class, reverseConvertGenerate = false)
public class HierarchyPropertyBo {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 层级id
     */
    @NotNull(message = "层级id不能为空", groups = {AddGroup.class})
    private Long hierarchyId;

    /**
     * 属性key
     */
    @NotBlank(message = "属性key不能为空", groups = {AddGroup.class})
    private Long typePropertyId;

    private Long propertyDictId;

    /**
     * 属性值
     */
    private String propertyValue;

    private String dictKey;

    private Integer scope;

}
