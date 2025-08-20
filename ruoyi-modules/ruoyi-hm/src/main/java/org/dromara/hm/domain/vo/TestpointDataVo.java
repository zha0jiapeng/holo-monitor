package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.hm.domain.TestpointData;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 测点数据视图对象 hm_testpoint_data
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = TestpointData.class)
public class TestpointDataVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * kks编码
     */
    @ExcelRequired
    @ExcelProperty(value = "KKS编码")
    private String kksCode;

    /**
     * 频率
     */
    @ExcelProperty(value = "频率")
    private BigDecimal frequency;

    /**
     * 脉冲数
     */
    @ExcelProperty(value = "脉冲数")
    private BigDecimal pulseCount;

    /**
     * 幅值
     */
    @ExcelProperty(value = "幅值")
    private BigDecimal magnitude;

    /**
     * 自计算报警状态
     */
    @ExcelProperty(value = "自计算报警状态")
    private String alarmType;

    /**
     * SD400MP报警状态
     */
    @ExcelProperty(value = "SD400MP报警状态")
    private Integer st;

    /**
     * 采集时间
     */
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "采集时间")
    private LocalDateTime acquisitionTime;

    /**
     * 站端诊断占比最高
     */
    @ExcelProperty(value = "站端诊断占比最高")
    private String pdtypeSite;

    /**
     * 平台诊断占比最高
     */
    @ExcelProperty(value = "平台诊断占比最高")
    private String pdtypePlatform;

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
