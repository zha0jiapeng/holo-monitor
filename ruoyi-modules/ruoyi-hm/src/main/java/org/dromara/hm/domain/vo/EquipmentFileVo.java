package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.hm.domain.EquipmentFile;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备文件视图对象 hm_equipment_file
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = EquipmentFile.class)
public class EquipmentFileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 设备id
     */
    @ExcelRequired
    @ExcelProperty(value = "设备id")
    private Long equipmentId;

    /**
     * 设备名称
     */
    @ExcelProperty(value = "设备名称")
    private String equipmentName;

    /**
     * 文件id
     */
    @ExcelRequired
    @ExcelProperty(value = "文件id")
    private Long fileId;

    /**
     * 文件名称
     */
    @ExcelProperty(value = "文件名称")
    private String fileName;

    /**
     * 文件类型
     */
    @ExcelRequired
    @ExcelProperty(value = "文件类型")
    private Integer fileType;

    /**
     * 文件类型名称
     */
    @ExcelProperty(value = "文件类型名称")
    private String fileTypeName;

    /**
     * 算法类型
     */
    @ExcelProperty(value = "算法类型")
    private Integer algorithmType;

    /**
     * 算法类型名称
     */
    @ExcelProperty(value = "算法类型名称")
    private String algorithmTypeName;

    /**
     * 创建时间
     */
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建人账号
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    @ExcelProperty(value = "创建人账号")
    private String createByName;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 更新人账号
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "updateBy")
    @ExcelProperty(value = "更新人账号")
    private String updateByName;

}
