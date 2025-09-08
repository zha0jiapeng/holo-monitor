package org.dromara.hm.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.hm.domain.HierarchyData;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 层级数据业务对象 hm_hierarchy_data
 *
 * @author ruoyi
 * @date 2024-01-01
 */

@Data
@AutoMapper(target = HierarchyData.class, reverseConvertGenerate = false)
public class HierarchyDataBo {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 层级id
     */
    @NotNull(message = "层级id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long hierarchyId;

    /**
     * 时间
     */
    @NotNull(message = "时间不能为空", groups = {AddGroup.class, EditGroup.class})
    private LocalDateTime time;

    /**
     * 标签
     */
    private String tag;

    /**
     * 名称
     */
    private String name;

    /**
     * 值
     */
    private BigDecimal value;

}
