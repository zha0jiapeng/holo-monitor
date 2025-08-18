package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import org.dromara.common.excel.annotation.ExcelNotation;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.hm.domain.TestPoint;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;


/**
 * 测点视图对象 testpoint
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = TestPoint.class)
public class TestPointVo implements Serializable {

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
    @ExcelProperty(value = "设备ID")
    private Long equipmentId;

    /**
     * 测点编码
     */
    @ExcelRequired
    @ExcelProperty(value = "测点编码")
    private String kksCode;

    /**
     * 测点名称
     */
    @ExcelRequired
    @ExcelProperty(value = "测点名称")
    private String kksName;

    @ExcelRequired
    @ExcelProperty(value = "测点具体类型")
    private Integer mt;

    @ExcelRequired
    @ExcelProperty(value = "测点类型")
    private Integer type;

    /**
     * 最近幅值
     */
    @ExcelProperty(value = "最近幅值")
    private BigDecimal lastMagnitude;

    /**
     * 最近报警状态
     */
    @ExcelProperty(value = "最近报警状态")
    private Integer lastSt;

    /**
     * 最近自计算报警
     */
    @ExcelProperty(value = "最近自计算报警")
    private Integer lastAlarmType;

    /**
     * 最近采集时间
     */
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "最近采集时间")
    private LocalDateTime lastAcquisitionTime;

    /**
     * 最近平台诊断
     */
    @ExcelProperty(value = "最近平台诊断")
    private String lastPdtypePlatform;

    /**
     * 最近站端诊断
     */
    @ExcelProperty(value = "最近站端诊断")
    private String lastPdtypeSite;

    /**
     * 特高频忽略阈值
     */
    @ExcelProperty(value = "特高频忽略阈值")
    private BigDecimal uhfIgnoreThreshold;

    /**
     * 特高频突变阈值
     */
    @ExcelProperty(value = "特高频突变阈值")
    private BigDecimal uhfMutationThreshold;

    /**
     * 特高频1级报警阈值
     */
    @ExcelProperty(value = "特高频1级报警阈值")
    private BigDecimal uhfLevel1AlarmThreshold;

    /**
     * 特高频2级报警阈值
     */
    @ExcelProperty(value = "特高频2级报警阈值")
    private BigDecimal uhfLevel2AlarmThreshold;

    /**
     * 特高频3级报警阈值
     */
    @ExcelProperty(value = "特高频3级报警阈值")
    private BigDecimal uhfLevel3AlarmThreshold;

    /**
     * 放电事件数比例阈值
     */
    @ExcelProperty(value = "放电事件数比例阈值")
    private BigDecimal dischargeEventRatioThreshold;

    /**
     * 事件数阈值周期（小时）
     */
    @ExcelProperty(value = "事件数阈值周期")
    private Integer eventCountThresholdPeriod;

    /**
     * 报警复位延时（小时）
     */
    @ExcelProperty(value = "报警复位延时")
    private Integer alarmResetDelay;

    /**
     * 离线判断（小时）
     */
    @ExcelProperty(value = "离线判断阈值")
    private Integer offlineJudgmentThreshold;

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

    @ExcelProperty(value = "位置坐标X")
    private BigDecimal positionX;

    @ExcelProperty(value = "位置坐标Y")
    private BigDecimal positionY;

    @ExcelProperty(value = "位置坐标Z")
    private BigDecimal positionZ;

}
