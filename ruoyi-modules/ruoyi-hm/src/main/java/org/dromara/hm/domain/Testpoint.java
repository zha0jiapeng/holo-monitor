package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测点对象 testpoint
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@TableName("hm_testpoint")
public class Testpoint extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（使用SD400MP的ID）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 设备id
     */
    private Long hierarchyId;

    private Long hierarchyOwnerId;


    /**
     * 测点编码
     */
    private String kksCode;

    /**
     * 测点名称
     */
    private String kksName;

    /**
     * 大屏显示名称
     */
    private String showName;


    private Integer mt;

    private Integer type;


    private Integer offlineFlag;

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


    private BigDecimal positionX;

    private BigDecimal positionY;

    private BigDecimal positionZ;

    private Integer positionSource;



}
