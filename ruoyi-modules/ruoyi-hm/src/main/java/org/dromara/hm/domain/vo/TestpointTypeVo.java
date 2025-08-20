package org.dromara.hm.domain.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Set;

/**
 * 测点类型VO
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestpointTypeVo {

    /**
     * 类型编码
     */
    private Integer code;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 类型描述
     */
    private String description;

    /**
     * 对应的mt值集合
     */
    private Set<Integer> mtValues;

    /**
     * mt值范围描述（用于前端展示）
     */
    private String mtRangeText;

    /**
     * 该类型下的测点数量
     */
    private Long testPointCount;

    /**
     * 是否为默认类型
     */
    private Boolean isDefault;
}
