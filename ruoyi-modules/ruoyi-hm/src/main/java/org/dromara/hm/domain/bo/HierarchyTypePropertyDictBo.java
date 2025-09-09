package org.dromara.hm.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 层级类型属性字典业务对象 hm_hierarchy_type_property_dict
 *
 * @author Mashir0
 * @date 2024-01-01
 */

@Data
@AutoMapper(target = HierarchyTypePropertyDict.class, reverseConvertGenerate = false)
public class HierarchyTypePropertyDictBo {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 属性字典名称
     */
    @NotEmpty(message = "属性字典名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String dictName;

    /**
     * 属性类型
     */
    private Integer dataType;

    private String dictValues;

    /**
     * 系统属性
     */
    private Boolean systemFlag;

    /**
     * 字典key
     */
    private String dictKey;

}
