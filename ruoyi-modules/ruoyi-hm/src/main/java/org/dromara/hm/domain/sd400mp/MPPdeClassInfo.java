package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MP PDE类信息对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPPdeClassInfo {

    /**
     * 类索引
     */
    private Integer index;

    /**
     * 类名称
     */
    private String name;

    /**
     * 构造函数
     *
     * @param data JSON数据对象
     */
    public MPPdeClassInfo(Object data) {
        // 这里需要根据实际的JSON结构进行解析
        // 暂时保持空实现，具体实现需要根据API响应格式确定
    }

    /**
     * 获取索引
     *
     * @return 索引值
     */
    public Integer getIndex() {
        return this.index;
    }
}
