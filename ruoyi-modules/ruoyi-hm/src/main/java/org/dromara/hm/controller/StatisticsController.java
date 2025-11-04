 package org.dromara.hm.controller;

 import cn.dev33.satoken.annotation.SaIgnore;
 import lombok.RequiredArgsConstructor;
 import org.dromara.common.core.domain.R;

 import org.dromara.common.web.annotation.BrotliCompress;
 import org.dromara.common.web.core.BaseController;
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
    public R<List<Map<String, Object>>> getTargetTypeList(Long hierarchyId, Long targetTypeId, @RequestParam(required = false) Long statisticsTypeId, @RequestParam(required = false) Long sensorGroupId) {
        return R.ok(statisticsService.getTargetTypeList(hierarchyId, targetTypeId, statisticsTypeId, sensorGroupId));
     }

     @GetMapping("/getNextHierarchyList")
     public R<List<HierarchyVo>> getNextHierarchyList(Long hierarchyId, Long targetTypeId) {
         List<HierarchyVo> result =  statisticsService.getNextHierarchyList(hierarchyId, targetTypeId);
         return R.ok(result);
     }

     @GetMapping("/alarm")
     public R<Map<String,Object>> alarm(Long hierarchyId,Long targetTypeId,Integer statisticalType, @RequestParam(required = false) Long sensorGroupId) {
        Map<String,Object> result =  statisticsService.alarm(hierarchyId,targetTypeId,statisticalType, sensorGroupId);
        return R.ok(result);
     }

     @GetMapping("/alarmList")
     public R<Map<String,Object>> alarmList(Long hierarchyId, @RequestParam(required = false) Long sensorGroupId,
                                            @RequestParam(required = false)String startTime,
                                            @RequestParam(required = false)String endTime,
                                            @RequestParam(required = false)String groupKeys) {
        Map<String,Object> result =  statisticsService.alarmList(hierarchyId, sensorGroupId,startTime,endTime,groupKeys);
        return R.ok(result);
     }

    @GetMapping("/sensorList")
    public R<List<HierarchyVo>> sensorList(Long hierarchyId,boolean showAllFlag) {
        List<HierarchyVo> list =  statisticsService.sensorList(hierarchyId,showAllFlag);
        for (HierarchyVo hierarchyVo : list) {
            hierarchyVo.setCode(hierarchyVo.getFullCode());
        }
        return R.ok(list);
    }

    @GetMapping("/sensorGroupList")
    public R<Map<String, Object>> sensorListGroupByThreeSystem(Long hierarchyId, boolean showAllFlag, Long sensorGroupId) {
        Map<String, Object> result = statisticsService.sensorListGroupByThreeSystem(hierarchyId, showAllFlag, sensorGroupId);
        return R.ok(result);
    }
    @GetMapping("/deviceCategoryStatistics")
    public R<Map<String, Object> > statisticsByDeviceCategory(Long hierarchyId, Long deviceCategoryId) {
        Map<String, Object>  result = statisticsService.statisticsByDeviceCategory(hierarchyId, deviceCategoryId);
        return R.ok(result);
    }

}
