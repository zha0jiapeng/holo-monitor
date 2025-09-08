 package org.dromara.hm.service;

 import org.dromara.common.core.domain.R;
 import org.dromara.hm.domain.Hierarchy;
 import org.dromara.hm.enums.StatisticsCountTypeEnum;
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


     /**
      * 获取设备详细统计数据
      *
      * @param hierarchyId 层级ID
      * @return 统计结果
      */
     Map<String, Object> getEquipmentDetailStatistics(Long hierarchyId);

     /**
      * 获取测点详细统计数据
      *
      * @param hierarchyId 层级ID
      * @return 统计结果
      */
     Map<String, Object> getTestpointDetailStatistics(Long hierarchyId);

     /**
      * 获取报告详细统计数据
      *
      * @param hierarchyId 层级ID
      * @param countType 统计数量类型
      * @return 统计结果
      */
     Map<String, Object> getReportDetailStatistics(Long hierarchyId, StatisticsCountTypeEnum countType);

     /**
      * 获取离线详细统计数据
      *
      * @param hierarchyId 层级ID
      * @param countType 统计数量类型
      * @return 统计结果
      */
     Map<String, Object> getOfflineDetailStatistics(Long hierarchyId, StatisticsCountTypeEnum countType);

     // 为了向后兼容，保留原有的方法
     /**
      * 获取报告详细统计数据（默认统计设备数量）
      *
      * @param hierarchyId 层级ID
      * @return 统计结果
      */
     default Map<String, Object> getReportDetailStatistics(Long hierarchyId) {
         return getReportDetailStatistics(hierarchyId, StatisticsCountTypeEnum.EQUIPMENT);
     }

     /**
      * 获取离线详细统计数据（默认统计设备数量）
      *
      * @param hierarchyId 层级ID
      * @return 统计结果
      */
     default Map<String, Object> getOfflineDetailStatistics(Long hierarchyId) {
         return getOfflineDetailStatistics(hierarchyId, StatisticsCountTypeEnum.EQUIPMENT);
     }

     /**
      * 获取实时报警列表
      *
      * @param hierarchyId 层级ID
      * @param alarmTypes 报警类型列表，为空则查询所有报警
      * @param minutesAgo 查询多少分钟之前的数据
      * @return 报警列表
      */
     Map<String, Object> getRealtimeAlarmList(Long hierarchyId, List<Integer> alarmTypes, Integer minutesAgo);

     /**
      * 获取实时报警列表（使用默认参数）
      *
      * @param hierarchyId 层级ID
      * @return 报警列表
      */
     default Map<String, Object> getRealtimeAlarmList(Long hierarchyId) {
         return getRealtimeAlarmList(hierarchyId, null, 60); // 默认查询1小时内的所有报警
     }

     R<Map<String, Object>> getNextHierarchyList(Long hierarchyId,Long targetTypeId);
 }
