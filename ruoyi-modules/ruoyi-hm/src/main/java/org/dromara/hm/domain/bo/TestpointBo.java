package org.dromara.hm.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotNull;
import org.dromara.hm.domain.Testpoint;
import org.dromara.hm.validate.BindGroup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测点业务对象 testpoint
 *
 * @author ruoyi
 * @date 2024-01-01
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = Testpoint.class, reverseConvertGenerate = false)
public class TestpointBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键不能为空", groups = {BindGroup.class})
    private Long id;

    /**
     * 设备id
     */
    //@NotNull(message = "设备ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long equipmentId;

    /**
     * 测点编码
     */
    //@NotBlank(message = "测点编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String kksCode;

    /**
     * 测点名称
     */
    //@NotBlank(message = "测点名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String kksName;

    private Integer mt;


    private Integer type;

    /**
     * 最近幅值
     */
    private BigDecimal lastMagnitude;

    /**
     * 最近报警状态
     */
    private Integer lastSt;

    /**
     * 最近自计算报警
     */
    private Integer lastAlarmType;

    /**
     * 最近采集时间
     */
    private LocalDateTime lastAcquisitionTime;

    /**
     * 最近平台诊断
     */
    private String lastPdtypePlatform;

    /**
     * 最近站端诊断
     */
    private String lastPdtypeSite;

    /**
     * 特高频忽略阈值
     */
    private BigDecimal uhfIgnoreThreshold;

    /**
     * 特高频突变阈值
     */
    private BigDecimal uhfMutationThreshold;

    /**
     * 特高频1级报警阈值
     */
    private BigDecimal uhfLevel1AlarmThreshold;

    /**
     * 特高频2级报警阈值
     */
    private BigDecimal uhfLevel2AlarmThreshold;

    /**
     * 特高频3级报警阈值
     */
    private BigDecimal uhfLevel3AlarmThreshold;

    /**
     * 放电事件数比例阈值
     */
    private BigDecimal dischargeEventRatioThreshold;

    /**
     * 事件数阈值周期（小时）
     */
    private Integer eventCountThresholdPeriod;

    /**
     * 报警复位延时（小时）
     */
    private Integer alarmResetDelay;

    /**
     * 离线判断（小时）
     */
    private Integer offlineJudgmentThreshold;

    @NotNull(message = "位置坐标X不能为空", groups = {BindGroup.class})
    private BigDecimal positionX;

    @NotNull(message = "位置坐标Y不能为空", groups = {BindGroup.class})
    private BigDecimal positionY;

    @NotNull(message = "位置坐标Z不能为空", groups = {BindGroup.class})
    private BigDecimal positionZ;

}
