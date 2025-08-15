package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.hm.domain.Equipment;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 设备视图对象 hm_equipment
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = Equipment.class)
public class EquipmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;


    /**
     * 模型ossId
     */
    private String ossId;

    /**
     * 唯一键
     */
    @ExcelProperty(value = "唯一键")
    private String uniqueKey;

    /**
     * 父级设备ID
     */
    @ExcelProperty(value = "父级设备ID")
    private Long idParent;

    /**
     * 设备名称
     */
    @ExcelRequired
    @ExcelProperty(value = "设备名称")
    private String name;

    /**
     * 设备描述
     */
    @ExcelProperty(value = "设备描述")
    private String desc;

    /**
     * 设备类型
     */
    @ExcelRequired
    @ExcelProperty(value = "设备类型")
    private Integer type;

    /**
     * 设备设置
     */
    private byte[] settings;

    /**
     * 纬度
     */
    @ExcelProperty(value = "纬度")
    private Double lat;

    /**
     * 经度
     */
    @ExcelProperty(value = "经度")
    private Double lng;

    /**
     * 创建时间
     */
    @ExcelRequired
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 创建人
     */
    @ExcelProperty(value = "创建人")
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
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 更新人
     */
    @ExcelProperty(value = "更新人")
    private Long updateBy;

    /**
     * 更新人账号
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "updateBy")
    @ExcelProperty(value = "更新人账号")
    private String updateByName;

}
