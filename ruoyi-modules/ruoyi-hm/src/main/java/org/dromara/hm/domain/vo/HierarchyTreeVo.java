package org.dromara.hm.domain.vo;

import cn.hutool.json.JSONArray;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 层级树形视图对象 hm_hierarchy
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HierarchyTreeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 层级类型id
     */
    private Long typeId;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 层级名称
     */
    private String name;

    /**
     * 层级编码
     */
    private String code;

    /**
     * 完整编码
     */
    private String fullCode;

    private String typeKey;

    /**
     * 子节点列表
     */
    private List<HierarchyTreeVo> children;

    /**
     * 层级属性列表
     */
    private List<HierarchyPropertyVo> properties;

    /**
     * 是否有传感器标志
     */
    private boolean haveSensorFlag;

    /**
     * 未绑定传感器列表（当当前节点为unit类型时）
     */
    private List<HierarchyVo> unboundSensors;

    /**
     * 已绑定传感器列表（当当前节点为unit类型时）
     */
    private List<HierarchyVo> boundSensors;
}
