 package org.dromara.hm.controller;

 import cn.dev33.satoken.annotation.SaIgnore;
 import lombok.RequiredArgsConstructor;
 import org.dromara.common.core.domain.R;

 import org.dromara.common.web.annotation.BrotliCompress;
 import org.dromara.common.web.core.BaseController;
 import org.dromara.hm.domain.vo.HierarchyVo;
 import org.dromara.hm.service.IStatisticsService;
 import org.dromara.hm.service.IHierarchyService;
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
     private final IHierarchyService hierarchyService;

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

     @GetMapping("/alarmList")
     public R<Map<String,Object>> alarmList(Long hierarchyId) {
        Map<String,Object> result =  statisticsService.alarmList(hierarchyId);
        return R.ok(result);
     }

 }
