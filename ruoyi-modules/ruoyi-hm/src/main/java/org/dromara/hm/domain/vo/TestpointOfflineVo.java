package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.hm.domain.TestpointOffline;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 测点离线记录视图对象 testpoint_offline
 *
 * @author Mashir0
 * @date 2025-01-27
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = TestpointOffline.class)
public class TestpointOfflineVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    @ExcelProperty(value = "设备id")
    private Long equipmentId;
    /**
     * 测点编码
     */
    @ExcelRequired
    @ExcelProperty(value = "测点编码")
    private String kksCode;

    /**
     * 离线时间
     */
    @ExcelRequired
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "离线时间")
    private LocalDateTime offlineTime;

    /**
     * 恢复时间
     */
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "恢复时间")
    private LocalDateTime recoveryTime;

    /**
     * 离线持续时长(秒)
     */
    @ExcelProperty(value = "离线持续时长(秒)")
    private Long offlineDuration;

    /**
     * 状态：0-已恢复, 1-离线中
     */
    @ExcelRequired
    @ExcelProperty(value = "状态")
    private Integer status;

    /**
     * 当时的离线阈值
     */
    @ExcelProperty(value = "离线判断阈值(小时)")
    private Integer offlineJudgmentThreshold;

    /**
     * 创建时间
     */
    @ExcelRequired
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

}
