package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.dromara.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测点离线记录对象 testpoint_offline
 *
 * @author Mashir0
 * @date 2025-01-27
 */
@Data
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@TableName("hm_testpoint_offline")
public class TestpointOffline implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;


    private Long equipmentId;

    /**
     * 测点编码
     */
    private String kksCode;

    /**
     * 离线时间
     */
    private LocalDateTime offlineTime;

    /**
     * 恢复时间
     */
    private LocalDateTime recoveryTime;

    /**
     * 离线持续时长(秒)
     */
    private Long offlineDuration;

    /**
     * 状态：0-已恢复, 1-离线中
     */
    private Integer status;

    /**
     * 当时的离线阈值
     */
    private Integer offlineJudgmentThreshold;

    private LocalDateTime createTime;

}
