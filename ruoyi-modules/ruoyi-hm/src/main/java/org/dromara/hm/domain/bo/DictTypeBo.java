package org.dromara.hm.domain.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.hm.domain.DictType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 字典类型业务对象 hm_dict_type
 *
 * @author Mashir0
 * @date 2024-01-01
 */

@Data
@AutoMapper(target = DictType.class, reverseConvertGenerate = false)
public class DictTypeBo {

    /**
     * 字典主键
     */
    @NotNull(message = "字典主键不能为空", groups = {EditGroup.class})
    private Long dictId;

    /**
     * 字典名称
     */
    @NotBlank(message = "字典名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String dictName;

    /**
     * 字典类型
     */
    @NotBlank(message = "字典类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String dictType;

}
