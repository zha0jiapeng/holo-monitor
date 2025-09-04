package org.dromara.hm.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;

/**
 * 字典类型对象 hm_dict_type
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@Data
@TableName("hm_dict_type")
public class DictType {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典主键
     */
    @TableId(value = "dict_id", type = IdType.AUTO)
    private Long dictId;

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 字典类型
     */
    private String dictType;

}
