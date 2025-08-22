package org.dromara.hm.domain.bo;

import cn.idev.excel.annotation.ExcelProperty;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.hm.domain.Equipment;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.dromara.hm.validate.BindGroup;

import java.math.BigDecimal;

/**
 * 设备业务对象 hm_equipment
 *
 * @author ruoyi
 * @date 2024-01-01
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = Equipment.class, reverseConvertGenerate = false)
public class EquipmentBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;


    /**
     * 模型ossId
     */
    @NotBlank(message = "模型不能为空", groups = {EditGroup.class})
    private String ossId;

    /**
     * 唯一键
     */
    private String uniqueKey;

    /**
     * 层级id
     */
    private Long hierarchyId;

    /**
     * 设备名称
     */
    //@NotBlank(message = "设备名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    private String showName;

    /**
     * 设备描述
     */
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

    @NotNull(message = "位置坐标X不能为空", groups = {BindGroup.class})
    private BigDecimal positionX;

    @NotNull(message = "位置坐标Y不能为空", groups = {BindGroup.class})
    private BigDecimal positionY;

    @NotNull(message = "位置坐标Z不能为空", groups = {BindGroup.class})
    private BigDecimal positionZ;

}
