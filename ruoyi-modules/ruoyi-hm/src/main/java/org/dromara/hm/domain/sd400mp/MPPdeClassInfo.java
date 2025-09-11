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
    public MPPdeClassInfo(cn.hutool.json.JSONObject data) {
        this.index = data.getInt("index");
        this.name = data.getStr("name");
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
