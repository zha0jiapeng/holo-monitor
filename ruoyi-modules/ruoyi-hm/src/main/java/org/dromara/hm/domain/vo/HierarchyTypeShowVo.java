package org.dromara.hm.domain.vo;

import lombok.Data;

/**
 * 层级类型展示视图对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
public class HierarchyTypeShowVo {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 层级id
     */
    private Long typeId;

    /**
     * 展示类型id
     */
    private Long showTypeId;

}
