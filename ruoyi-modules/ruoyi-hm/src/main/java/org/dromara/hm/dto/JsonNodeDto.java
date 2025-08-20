package org.dromara.hm.dto;

import lombok.Data;
import java.util.List;

@Data
public class JsonNodeDto {
    private String id;
    private Integer type;
    private String val;
    private List<JsonNodeDto> children;
    private Integer bt;
    private List<Integer> d;
}
