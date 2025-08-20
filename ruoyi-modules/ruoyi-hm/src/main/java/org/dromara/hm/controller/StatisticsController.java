package org.dromara.hm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.core.validate.QueryGroup;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.domain.bo.EquipmentBo;
import org.dromara.hm.domain.vo.EquipmentVo;
import org.dromara.hm.service.IEquipmentService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 统计控制器
 *
 * @author Mashir0
 * @date 2025-08-20
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/hm/statistics")
public class StatisticsController extends BaseController {

    private final IEquipmentService equipmentService;

    @GetMapping("/byDutMajor")
    public void byDutMajor(Long equipmentId) {
       // Map<String,Object> result =  equipmentService.byDutMajor(equipmentId);
    }
}
