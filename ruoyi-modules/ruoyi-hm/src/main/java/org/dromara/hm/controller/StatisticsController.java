package org.dromara.hm.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;

import org.dromara.common.web.core.BaseController;
import org.dromara.hm.service.IEquipmentService;
import org.dromara.hm.service.ITestpointService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

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

    private final ITestpointService testpointService;

    @GetMapping("/getPowerPlantStatistics")
    public R<Map<String,Object>> getPowerPlantStatistics(Long hierarchyId) {
       Map<String,Object> result =  equipmentService.getPowerPlantStatistics(hierarchyId);
       return R.ok(result);
    }

    @GetMapping("/getEquipmentDetailStatistics")
    public R<Map<String,Object>> getEquipmentDetailStatistics(Long hierarchyId) {
       Map<String,Object> result =  equipmentService.getEquipmentDetailStatistics(hierarchyId);
       return R.ok(result);
    }

    @GetMapping("/getTestpointDetailStatistics")
    public R<Map<String,Object>> getTestpointDetailStatistics(Long hierarchyId) {
        Map<String,Object> result =  testpointService.getTestpointDetailStatistics(hierarchyId);
        return R.ok(result);
    }

    @GetMapping("/getReportDetailStatistics")
    public R<Map<String,Object>> getReportDetailStatistics(Long hierarchyId) {
        Map<String,Object> result =  testpointService.getReportDetailStatistics(hierarchyId);
        return R.ok(result);
    }
}
