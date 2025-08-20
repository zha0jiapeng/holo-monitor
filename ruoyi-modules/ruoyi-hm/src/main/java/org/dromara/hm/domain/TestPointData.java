package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.dromara.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 测点数据对象 hm_testpoint_data
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode
@TableName("hm_testpoint_data")
public class TestPointData {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * kks编码
     */
    private String kksCode;

    /**
     * 频率
     */
    private BigDecimal frequency;

    /**
     * 脉冲数
     */
    private BigDecimal pulseCount;

    /**
     * 幅值
     */
    private BigDecimal magnitude;

    /**
     * 自计算报警状态
     */
    private Integer alarmType;

    /**
     * SD400MP报警状态
     */
    private Integer st;

    /**
     * 采集时间
     */
    private LocalDateTime acquisitionTime;

    /**
     * zds文件流
     */
    private byte[] zds;

    /**
     * 站端诊断结果
     */
    private byte[] pdexpertSite;

    /**
     * 平台诊断结果
     */
    private byte[] pdexpertPlatform;

    /**
     * 站端诊断占比最高
     */
    private String pdtypeSite;

    /**
     * 平台诊断占比最高
     */
    private String pdtypePlatform;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

}
