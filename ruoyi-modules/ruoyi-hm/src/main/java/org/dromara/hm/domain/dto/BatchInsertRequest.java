package org.dromara.hm.domain.dto;

import org.dromara.hm.enums.AlgorithmTypeEnum;
import org.dromara.hm.enums.EquipmentFileTypeEnum;
import org.dromara.hm.validate.EnumValid;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量操作请求DTO
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public class BatchInsertRequest {

    /**
     * 文件ID列表
     */
    @NotEmpty(message = "文件ID列表不能为空")
    private List<Long> fileIds;

    /**
     * 文件类型
     */
    @NotNull(message = "文件类型不能为空")
    @EnumValid(enumClass = EquipmentFileTypeEnum.class, message = "文件类型值不在定义的枚举范围内")
    private Integer fileType;

    /**
     * 算法类型
     */
    @EnumValid(enumClass = AlgorithmTypeEnum.class, message = "算法类型值不在定义的枚举范围内")
    private Integer algorithmType;

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    public Integer getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(Integer algorithmType) {
        this.algorithmType = algorithmType;
    }
}
