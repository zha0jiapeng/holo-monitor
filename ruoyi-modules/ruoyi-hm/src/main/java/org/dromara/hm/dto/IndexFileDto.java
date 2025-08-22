package org.dromara.hm.dto;

import lombok.Data;
import java.util.List;

@Data
public class IndexFileDto {
    private int version;
    private int type;
    private List<JsonNodeDto> children;
}
