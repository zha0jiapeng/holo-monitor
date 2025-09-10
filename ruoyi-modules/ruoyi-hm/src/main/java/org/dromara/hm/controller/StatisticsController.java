 package org.dromara.hm.controller;

 import cn.dev33.satoken.annotation.SaIgnore;
 import lombok.RequiredArgsConstructor;
 import org.dromara.common.core.domain.R;

 import org.dromara.common.web.annotation.BrotliCompress;
 import org.dromara.common.web.core.BaseController;
 import org.dromara.hm.domain.Hierarchy;
 import org.dromara.hm.domain.vo.HierarchyVo;
 import org.dromara.hm.service.IStatisticsService;
 import org.springframework.validation.annotation.Validated;
 import org.springframework.web.bind.annotation.*;

 import java.util.List;
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
 @BrotliCompress
 @SaIgnore
 public class StatisticsController extends BaseController {

     private final IStatisticsService statisticsService;

    @GetMapping("/getTargetTypeCount")
    public R<List<Map<String, Object>>> getTargetTypeList(Long hierarchyId,Long targetTypeId) {
         List<Map<String, Object>> result =  statisticsService.getTargetTypeList(hierarchyId,targetTypeId);
        return R.ok(result);
     }

     @GetMapping("/getNextHierarchyList")
     public R<List<HierarchyVo>> getNextHierarchyList(Long hierarchyId, Long targetTypeId) {
         List<HierarchyVo> result =  statisticsService.getNextHierarchyList(hierarchyId, targetTypeId);
         return R.ok(result);
     }

     @GetMapping("/alarm")
     public R<Map<String,Object>> alarm(Long hierarchyId,Long targetTypeId,Integer statisticalType) {
        Map<String,Object> result =  statisticsService.alarm(hierarchyId,targetTypeId,statisticalType);
        return R.ok(result);
     }

//     @GetMapping("/getTestpointDetailStatistics")
//     public R<Map<String,Object>> getTestpointDetailStatistics(Long hierarchyId) {
//         Map<String,Object> result =  statisticsService.getTestpointDetailStatistics(hierarchyId);
//         return R.ok(result);
//     }
//
//     @GetMapping("/getReportDetailStatistics")
//     public R<Map<String,Object>> getReportDetailStatistics(Long hierarchyId, Integer countType) {
//         StatisticsCountTypeEnum countTypeEnum = StatisticsCountTypeEnum.getByCode(countType != null ? countType : 0);
//         if(countTypeEnum==null) return R.fail("countType错误");
//         Map<String,Object> result =  statisticsService.getReportDetailStatistics(hierarchyId, countTypeEnum);
//         return R.ok(result);
//     }
//
//     @GetMapping("/getOfflineDetailStatistics")
//     public R<Map<String,Object>> getOfflineDetailStatistics(Long hierarchyId, Integer countType) {
//         StatisticsCountTypeEnum countTypeEnum = StatisticsCountTypeEnum.getByCode(countType != null ? countType : 0);
//         if(countTypeEnum==null) return R.fail("countType错误");
//         Map<String,Object> result =  statisticsService.getOfflineDetailStatistics(hierarchyId, countTypeEnum);
//         return R.ok(result);
//     }
//
//     @GetMapping("/realtimeAlarmList")
//     public R<Map<String,Object>> getRealtimeAlarmList(Long hierarchyId, Integer minutesAgo, @RequestParam(required = false) List<Integer> alarmTypes) {
//         Map<String,Object> result =  statisticsService.getRealtimeAlarmList(hierarchyId, alarmTypes, minutesAgo);
//         return R.ok(result);
//     }
 }
