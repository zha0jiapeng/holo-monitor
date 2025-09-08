package org.dromara.hm.domain.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import org.apache.poi.ss.formula.functions.T;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.hm.domain.Hierarchy;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.dromara.hm.domain.HierarchyProperty;

import java.util.List;

/**
 * 层级业务对象 hm_hierarchy
 *
 * @author Mashir0
 * @date 2024-01-01
 */

@Data
@AutoMapper(target = Hierarchy.class, reverseConvertGenerate = false)
public class HierarchyBo {

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
     * 父级ID
     */
    private Long parentId;

    /**
     * 层级名称
     */
    @NotBlank(message = "层级名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 层级编码
     */
    private String code;

    @TableField(exist = false)
    private List<HierarchyProperty> properties;

    /**
     * 是否需要生成编码（临时标志字段）
     */
    @TableField(exist = false)
    private Boolean needGenerateCode;

    @TableField(exist = false)
    private Boolean needProperty = false;

}
