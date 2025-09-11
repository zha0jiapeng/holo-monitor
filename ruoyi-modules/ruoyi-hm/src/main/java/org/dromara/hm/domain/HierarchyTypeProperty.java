package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.dromara.hm.domain.vo.HierarchyTypeVo;

import java.io.Serial;

/**
 * 层级类型属性对象 hm_hierarchy_type_property
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@TableName("hm_hierarchy_type_property")
public class HierarchyTypeProperty {

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
     * 字典id
     */
    private Long propertyDictId;

    /**
     * 是否必填
     */
    private Integer required;


}
