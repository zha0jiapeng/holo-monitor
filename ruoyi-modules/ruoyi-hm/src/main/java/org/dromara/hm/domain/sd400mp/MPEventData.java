package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MP事件数据对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPEventData {

    /**
     * 有效载荷数据（Base64编码）
     */
    private String payload;
}
