package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MP事件对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPEvent {

    /**
     * 事件分组
     */
    private MPEventGroup group;

    /**
     * 状态值
     */
    private Integer state;

    /**
     * 开始时间
     */
    private Date start;

    /**
     * 结束时间
     */
    private Date end;

    /**
     * 设备ID
     */
    private Long equipmentId;

    /**
     * 测点ID
     */
    private Long testpointId;

    /**
     * 卫星值
     */
    private Double satelliteValue;

    /**
     * 构造函数
     *
     * @param group 事件分组
     * @param state 状态值
     * @param time 时间
     * @param equipmentId 设备ID
     * @param testpointId 测点ID
     */
    public MPEvent(MPEventGroup group, Integer state, Date time, Long equipmentId, Long testpointId) {
        this.group = group;
        this.state = state;
        this.start = time;
        this.equipmentId = equipmentId;
        this.testpointId = testpointId;
    }
}
