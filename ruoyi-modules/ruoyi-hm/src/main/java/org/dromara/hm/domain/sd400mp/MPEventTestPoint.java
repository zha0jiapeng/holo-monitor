package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MP事件测点对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPEventTestPoint {

    /**
     * 测点ID
     */
    private Long id;

    /**
     * 标签列表
     */
    private List<MPEventTag> tags;
}
