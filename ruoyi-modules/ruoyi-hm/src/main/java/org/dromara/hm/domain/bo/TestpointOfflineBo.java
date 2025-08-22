package org.dromara.hm.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.dromara.hm.domain.TestpointOffline;

import java.time.LocalDateTime;

/**
 * 测点离线记录业务对象 testpoint_offline
 *
 * @author Mashir0
 * @date 2025-01-27
 */

@Data
@EqualsAndHashCode
@AutoMapper(target = TestpointOffline.class, reverseConvertGenerate = false)
public class TestpointOfflineBo {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;


    private Long equipmentId;
    /**
     * 测点编码
     */
    @NotBlank(message = "测点编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String kksCode;

    /**
     * 离线时间
     */
    @NotNull(message = "离线时间不能为空", groups = {AddGroup.class, EditGroup.class})
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
    @NotNull(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer status;

    /**
     * 当时的离线阈值
     */
    private Integer offlineJudgmentThreshold;


    private LocalDateTime createTime;

}
