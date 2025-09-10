package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MP事件标签对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPEventTag {

    /**
     * 标签ID
     */
    private Long tag;

    /**
     * 事件数据
     */
    private MPEventData events;

    /**
     * 卫星数据
     */
    private MPEventSatellite satelite;
}
