package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;

/**
 * 字典数据对象 hm_dict_data
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@TableName("hm_dict_data")
public class DictData {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典编码
     */
    @TableId(value = "dict_code", type = IdType.AUTO)
    private Long dictCode;

    /**
     * 字典标签
     */
    private String dictLabel;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 备注
     */
    private String remark;

}
