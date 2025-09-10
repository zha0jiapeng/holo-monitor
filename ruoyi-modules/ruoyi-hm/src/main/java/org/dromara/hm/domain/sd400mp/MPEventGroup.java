package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MP事件分组对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPEventGroup {

    /**
     * 分组键
     */
    private String key;

    /**
     * 标签信息
     */
    private Object tag;

    /**
     * 卫星标签
     */
    private Object satelliteTag;

    /**
     * 事件列表
     */
    private List<MPEvent> events = new ArrayList<>();

    /**
     * 显示设置映射
     */
    private Map<Long, Object> displaySettings = new HashMap<>();

    /**
     * 构造函数
     *
     * @param tag 标签对象
     * @param result 事件列表结果
     */
    public MPEventGroup(Object tag, MPEventList result) {
        // 这里需要根据实际的标签结构进行初始化
        // 暂时保持空实现
    }
}
