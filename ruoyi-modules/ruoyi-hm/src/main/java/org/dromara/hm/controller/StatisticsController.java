// package org.dromara.hm.controller;

// import lombok.RequiredArgsConstructor;
// import org.dromara.common.core.domain.R;

// import org.dromara.common.web.core.BaseController;
// import org.dromara.hm.service.IStatisticsService;
// import org.dromara.hm.enums.StatisticsCountTypeEnum;
// import org.springframework.validation.annotation.Validated;
// import org.springframework.web.bind.annotation.*;
// import java.util.Map;
// import java.util.List;

// /**
//  * 统计控制器
//  *
//  * @author Mashir0
//  * @date 2025-08-20
//  */
// @Validated
// @RequiredArgsConstructor
// @RestController
// @RequestMapping("/hm/statistics")
// public class StatisticsController extends BaseController {

//     private final IStatisticsService statisticsService;

//     @GetMapping("/getPowerPlantStatistics")
//     public R<Map<String,Object>> getPowerPlantStatistics(Long hierarchyId) {
//        Map<String,Object> result =  statisticsService.getPowerPlantStatistics(hierarchyId);
//        return R.ok(result);
//     }

//     @GetMapping("/getEquipmentDetailStatistics")
//     public R<Map<String,Object>> getEquipmentDetailStatistics(Long hierarchyId) {
//        Map<String,Object> result =  statisticsService.getEquipmentDetailStatistics(hierarchyId);
//        return R.ok(result);
//     }

//     @GetMapping("/getTestpointDetailStatistics")
//     public R<Map<String,Object>> getTestpointDetailStatistics(Long hierarchyId) {
//         Map<String,Object> result =  statisticsService.getTestpointDetailStatistics(hierarchyId);
//         return R.ok(result);
//     }

//     @GetMapping("/getReportDetailStatistics")
//     public R<Map<String,Object>> getReportDetailStatistics(Long hierarchyId, Integer countType) {
//         StatisticsCountTypeEnum countTypeEnum = StatisticsCountTypeEnum.getByCode(countType != null ? countType : 0);
//         if(countTypeEnum==null) return R.fail("countType错误");
//         Map<String,Object> result =  statisticsService.getReportDetailStatistics(hierarchyId, countTypeEnum);
//         return R.ok(result);
//     }

//     @GetMapping("/getOfflineDetailStatistics")
//     public R<Map<String,Object>> getOfflineDetailStatistics(Long hierarchyId, Integer countType) {
//         StatisticsCountTypeEnum countTypeEnum = StatisticsCountTypeEnum.getByCode(countType != null ? countType : 0);
//         if(countTypeEnum==null) return R.fail("countType错误");
//         Map<String,Object> result =  statisticsService.getOfflineDetailStatistics(hierarchyId, countTypeEnum);
//         return R.ok(result);
//     }

//     @GetMapping("/realtimeAlarmList")
//     public R<Map<String,Object>> getRealtimeAlarmList(Long hierarchyId, Integer minutesAgo, @RequestParam(required = false) List<Integer> alarmTypes) {
//         Map<String,Object> result =  statisticsService.getRealtimeAlarmList(hierarchyId, alarmTypes, minutesAgo);
//         return R.ok(result);
//     }
// }
