 package org.dromara.hm.service;

 import org.dromara.hm.domain.vo.HierarchyVo;
 import java.util.Map;
 import java.util.List;

 /**
  * 统计服务接口
  *
  * @author Mashir0
  * @date 2025-08-21
  */
 public interface IStatisticsService {


     List<Map<String, Object>> getTargetTypeList(Long hierarchyId, Long targetTypeId);


     List<HierarchyVo> getNextHierarchyList(Long hierarchyId, Long targetTypeId);

    Map<String, Object> alarm(Long hierarchyId, Long targetTypeId, Integer statisticalType);

    /**
     * 实时报警列表统计
     * @param hierarchyId 层级ID
     * @return 实时报警统计数据
     */
    Map<String, Object> alarmList(Long hierarchyId);
}
