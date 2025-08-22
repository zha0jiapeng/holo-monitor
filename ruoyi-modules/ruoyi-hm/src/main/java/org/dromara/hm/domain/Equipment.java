package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.dromara.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 设备对象 hm_equipment
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hm_equipment")
public class Equipment extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键（使用SD400MP的ID）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 唯一键
     */
    private String uniqueKey;

    /**
     * 模型ossId
     */
    private String ossId;

    /**
     * 层级id
     */
    private Long hierarchyId;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 大屏显示名称
     */
    private String showName;

    /**
     * 设备描述
     */
    @TableField("`desc`")
    private String desc;


    /**
     * 设备设置
     */
    private byte[] settings;

    /**
     * 纬度
     */
    private Double lat;

    /**
     * 经度
     */
    private Double lng;

    /**
     * 设备类型
     */
    private Integer dut;

    /**
     * 设备大类
     */
    private Integer dutMajor;

    /**
     * 电压等级
     */
    private String voltageLevel;

    private BigDecimal positionX;

    private BigDecimal positionY;

    private BigDecimal positionZ;

}
