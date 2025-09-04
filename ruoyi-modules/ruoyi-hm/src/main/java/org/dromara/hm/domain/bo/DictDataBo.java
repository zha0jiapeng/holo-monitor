package org.dromara.hm.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.hm.domain.DictData;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 字典数据业务对象 hm_dict_data
 *
 * @author Mashir0
 * @date 2024-01-01
 */

@Data
@AutoMapper(target = DictData.class, reverseConvertGenerate = false)
public class DictDataBo {

    /**
     * 字典编码
     */
    private Long dictCode;

    /**
     * 字典标签
     */
    @NotBlank(message = "字典标签不能为空", groups = {AddGroup.class, EditGroup.class})
    private String dictLabel;

    /**
     * 字典类型
     */
    @NotBlank(message = "字典类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String dictType;

    /**
     * 备注
     */
    private String remark;

}
