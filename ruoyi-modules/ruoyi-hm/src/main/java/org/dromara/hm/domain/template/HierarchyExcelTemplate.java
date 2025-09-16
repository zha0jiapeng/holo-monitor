package org.dromara.hm.domain.template;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class HierarchyExcelTemplate {

    @ExcelProperty("采集单元名称")
    private String hierarchy;

    @ExcelProperty("通道名称")
    private String name;

}
