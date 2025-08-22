package org.dromara.hm.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.hm.domain.EquipmentFile;
import org.dromara.hm.enums.AlgorithmTypeEnum;
import org.dromara.hm.enums.EquipmentFileTypeEnum;
import org.dromara.hm.validate.EnumValid;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 设备文件业务对象 hm_equipment_file
 *
 * @author ruoyi
 * @date 2024-01-01
 */

@Data
@EqualsAndHashCode
@AutoMapper(target = EquipmentFile.class, reverseConvertGenerate = false)
public class EquipmentFileBo {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 设备id
     */
    @NotNull(message = "设备id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long equipmentId;

    /**
     * 文件id
     */
    @NotNull(message = "文件id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long fileId;

    /**
     * 文件类型
     */
    @NotNull(message = "文件类型不能为空", groups = {AddGroup.class, EditGroup.class})
    @EnumValid(enumClass = EquipmentFileTypeEnum.class, message = "文件类型值不在定义的枚举范围内", groups = {AddGroup.class, EditGroup.class})
    private Integer fileType;

    /**
     * 算法类型
     */
    @EnumValid(enumClass = AlgorithmTypeEnum.class, message = "算法类型值不在定义的枚举范围内", groups = {AddGroup.class, EditGroup.class})
    private Integer algorithmType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 备注
     */
    private String remark;

}
