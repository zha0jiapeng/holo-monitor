package org.dromara.hm.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformedIndexDataDto {

    private String devid;
    private String time;
    private BigDecimal f;
    private BigDecimal mag;
    private BigDecimal cnt;
    private Integer st;
    private BigDecimal magAv;
    private BigDecimal external;
    private BigDecimal unknown;
    private BigDecimal floating;
    private BigDecimal corona;
    private BigDecimal insulation;
    private BigDecimal particle;
    private String pdType;
    private BigDecimal pdTypeValue;
    private String pdTypeJson;

}
