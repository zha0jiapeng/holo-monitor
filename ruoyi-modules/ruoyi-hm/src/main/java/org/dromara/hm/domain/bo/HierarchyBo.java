package org.dromara.hm.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.hm.domain.Hierarchy;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 层级业务对象 hm_hierarchy
 *
 * @author ruoyi
 * @date 2024-01-01
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = Hierarchy.class, reverseConvertGenerate = false)
public class HierarchyBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
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
    @NotBlank(message = "名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 描述
     */
    private String desc;

    /**
     * 类型
     */
    @NotNull(message = "类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer type;

    /**
     * 设置
     */
    private byte[] settings;

}
