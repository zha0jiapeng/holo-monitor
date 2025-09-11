package org.dromara.hm.domain.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MP显示设置对象，对应JavaScript中的MPDisplaySettings
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPDisplaySettings {
    
    /**
     * SNS值
     */
    private Integer sns;
    
    /**
     * 单位值
     */
    private Integer unit;
    
    /**
     * 创建显示设置对象的静态方法
     *
     * @param sns SNS值
     * @param unit 单位值
     * @return MPDisplaySettings对象
     */
    public static MPDisplaySettings get(Integer sns, Integer unit) {
        return new MPDisplaySettings(sns, unit);
    }
}
