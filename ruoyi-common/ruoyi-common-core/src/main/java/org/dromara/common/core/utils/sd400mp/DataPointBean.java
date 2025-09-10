package org.dromara.common.core.utils.sd400mp;

import lombok.Data;

import java.util.Date;

/**
 * 数据点实体类，包含时间戳和数值
 *
 * @author your-name
 */
@Data
public class DataPointBean {
    /**
     * 时间戳
     */
    private Date time;

    /**
     * 数值
     */
    private Double value;
}
