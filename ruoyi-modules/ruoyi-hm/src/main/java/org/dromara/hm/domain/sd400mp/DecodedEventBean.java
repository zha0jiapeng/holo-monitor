package org.dromara.hm.domain.sd400mp;

import lombok.Data;
import java.util.List;

@Data
public class DecodedEventBean {
    private Long equipmentId;
    private Long testpointId;
    private Long tagId;
    private String tagType; // new tag type field
    private String tagValue; // new tag value field
    private List<DataPointBean> dataPoints;
    private List<DataPointBean> satellitePoints;
}
