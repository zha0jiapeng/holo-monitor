package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MP事件卫星数据对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPEventSatellite {

    /**
     * 标签ID
     */
    private Long tag;

    /**
     * SNS值
     */
    private Integer sns;

    /**
     * 单位值
     */
    private Integer unit;

    /**
     * 事件数据
     */
    private MPEventData events;
}
