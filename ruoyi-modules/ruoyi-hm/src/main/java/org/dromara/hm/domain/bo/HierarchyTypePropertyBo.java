package org.dromara.hm.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.hm.domain.HierarchyTypeProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 层级类型属性业务对象 hm_hierarchy_type_property
 *
 * @author Mashir0
 * @date 2024-01-01
 */

@Data
@AutoMapper(target = HierarchyTypeProperty.class, reverseConvertGenerate = false)
public class HierarchyTypePropertyBo {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 层级类型id
     */
    @NotNull(message = "层级类型id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long typeId;

    /**
     * 字典id
     */
    @NotBlank(message = "字典id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long propertyDictId;

    /**
     * 是否必填
     */
    private Integer required;

}
