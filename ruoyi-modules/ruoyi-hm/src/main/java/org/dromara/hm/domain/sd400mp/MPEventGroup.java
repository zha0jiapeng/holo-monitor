package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    private MPTag tag;

    /**
     * 卫星标签
     */
    private MPTag satelliteTag;

    /**
     * 事件列表
     */
    @ToString.Exclude
    private List<MPEvent> events = new ArrayList<>();

    /**
     * 显示设置映射
     */
    private Map<Long, MPDisplaySettings> displaySettings = new HashMap<>();

    /**
     * 构造函数
     *
     * @param tag 标签对象
     * @param result 事件列表结果
     */
    public MPEventGroup(MPTag tag, MPEventList result) {
        this.key = tag.getKey();
        this.tag = tag;
        this.events = new ArrayList<>();
        this.displaySettings = new HashMap<>();
    }
}
