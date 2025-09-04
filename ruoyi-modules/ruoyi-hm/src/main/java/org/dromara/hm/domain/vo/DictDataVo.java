package org.dromara.hm.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelRequired;
import org.dromara.hm.domain.DictData;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典数据视图对象 hm_dict_data
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DictData.class)
public class DictDataVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典编码
     */
    @ExcelProperty(value = "字典编码")
    private Long dictCode;

    /**
     * 字典标签
     */
    @ExcelRequired
    @ExcelProperty(value = "字典标签")
    private String dictLabel;

    /**
     * 字典类型
     */
    @ExcelRequired
    @ExcelProperty(value = "字典类型")
    private String dictType;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

}
