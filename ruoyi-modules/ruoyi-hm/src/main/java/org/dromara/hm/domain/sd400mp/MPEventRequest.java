package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MP事件请求对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPEventRequest {

    /**
     * 设备ID
     */
    private String idEquipment;

    /**
     * 开始时间
     */
    private String from;

    /**
     * 结束时间
     */
    private String to;

    /**
     * 是否包含连接状态
     */
    private Boolean withConnectionState;

    /**
     * 测点ID列表
     */
    private List<Long> testpoints;

    /**
     * 构造基本请求
     *
     * @param idEquipment 设备ID
     * @param from 开始时间
     * @param to 结束时间
     * @param withConnectionState 是否包含连接状态
     */
    public MPEventRequest(String idEquipment, String from, String to, Boolean withConnectionState) {
        this.idEquipment = idEquipment;
        this.from = from;
        this.to = to;
        this.withConnectionState = withConnectionState;
    }
}
