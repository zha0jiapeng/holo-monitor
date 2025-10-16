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


     List<Map<String, Object>> getTargetTypeList(Long hierarchyId, Long targetTypeId, Long statisticsTypeId, Long sensorGroupId);


     List<HierarchyVo> getNextHierarchyList(Long hierarchyId, Long targetTypeId);

    Map<String, Object> alarm(Long hierarchyId, Long targetTypeId, Integer statisticalType);

    /**
     * 实时报警列表统计
     * @param hierarchyId 层级ID
     * @return 实时报警统计数据
     */
    Map<String, Object> alarmList(Long hierarchyId);

     List<HierarchyVo> sensorList(Long hierarchyId, boolean showAllFlag);

    /**
     * 根据sensor_group分组获取传感器列表
     * @param hierarchyId 层级ID
     * @param showAllFlag 是否显示所有
     * @param sensorGroupId sensor_group ID(可选,用于筛选指定的sensor_group)
     * @return 按sensor_group分组的传感器列表，包含分组结构信息
     */
    Map<String, Object> sensorListGroupByThreeSystem(Long hierarchyId, boolean showAllFlag, Long sensorGroupId);

    /**
     * 按设备类型获取设备树形结构
     * @param hierarchyId 层级ID
     * @param deviceCategoryId 设备大类ID(必填,用于筛选指定的设备类型)
     * @return 设备类型下的设备区域、设备、设备点树形列表
     */
    Map<String, Object> statisticsByDeviceCategory(Long hierarchyId, Long deviceCategoryId);
}
