package org.dromara.hm.domain.sd400mp;

import lombok.Data;
import java.util.Date;

@Data
public class DataPointBean {
    private Date time;
    private Double value;
}
