package org.dromara.hm.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.hm.domain.Equipment;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
     * 父级设备ID
     */
    private Long idParent;

    /**
     * 设备名称
     */
    //@NotBlank(message = "设备名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 设备描述
     */
    private String desc;

    /**
     * 设备类型
     */
    //@NotNull(message = "设备类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer type;

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

}
