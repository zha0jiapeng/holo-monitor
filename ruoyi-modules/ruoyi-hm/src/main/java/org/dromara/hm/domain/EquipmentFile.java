package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 设备文件对象 hm_equipment_file
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode
@TableName("hm_equipment_file")
public class EquipmentFile {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备id
     */
    private Long equipmentId;

    /**
     * 文件id
     */
    private Long fileId;

    /**
     * 文件类型
     */
    private Integer fileType;

    /**
     * 算法类型
     */
    private Integer algorithmType;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 备注
     */
    private String remark;

}
