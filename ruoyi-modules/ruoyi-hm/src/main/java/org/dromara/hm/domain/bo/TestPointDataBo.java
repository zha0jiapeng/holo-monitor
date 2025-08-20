package org.dromara.hm.domain.bo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.hm.domain.TestPointData;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 测点数据业务对象 hm_testpoint_data
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
@EqualsAndHashCode
@AutoMapper(target = TestPointData.class, reverseConvertGenerate = false)
public class TestPointDataBo {

    /**
     * id
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * kks编码
     */
    @NotBlank(message = "kks编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String kksCode;

    /**
     * 频率
     */
    private BigDecimal frequency;

    /**
     * 脉冲数
     */
    private BigDecimal pulseCount;

    /**
     * 幅值
     */
    private BigDecimal magnitude;

    /**
     * 自计算报警状态
     */
    private String alarmType;

    /**
     * SD400MP报警状态
     */
    private Integer st;

    /**
     * 采集时间
     */
    private LocalDateTime acquisitionTime;

    /**
     * zds文件流
     */
    private byte[] zds;

    /**
     * 站端诊断结果
     */
    private byte[] pdexpertSide;

    /**
     * 平台诊断结果
     */
    private byte[] pdexpertPlatform;

    /**
     * 站端诊断占比最高
     */
    private String pdtypeSite;

    /**
     * 平台诊断占比最高
     */
    private String pdtypePlatform;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

}
