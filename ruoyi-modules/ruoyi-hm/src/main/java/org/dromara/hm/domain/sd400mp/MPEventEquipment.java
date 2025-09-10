package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MP事件设备对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPEventEquipment {

    /**
     * 设备ID
     */
    private Long id;

    /**
     * 测点列表
     */
    private List<MPEventTestPoint> testpoints;
}
