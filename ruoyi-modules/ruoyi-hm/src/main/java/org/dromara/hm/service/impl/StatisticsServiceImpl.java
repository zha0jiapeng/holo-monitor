package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.HierarchyProperty;
import org.dromara.hm.domain.Testpoint;
import org.dromara.hm.domain.TestpointOffline;
import org.dromara.hm.domain.TestpointData;
import org.dromara.hm.enums.EquipmentDutEnum;
import org.dromara.hm.enums.TestpointTypeEnum;
import org.dromara.hm.enums.AlarmTypeEnum;
import org.dromara.hm.enums.StatisticsCountTypeEnum;
import org.dromara.hm.mapper.HierarchyMapper;
import org.dromara.hm.mapper.HierarchyPropertyMapper;
import org.dromara.hm.mapper.TestpointMapper;
import org.dromara.hm.mapper.TestpointOfflineMapper;
import org.dromara.hm.mapper.TestpointDataMapper;
import org.dromara.hm.service.IStatisticsService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 *
 * @author Mashir0
 * @date 2025-08-21
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements IStatisticsService {

            private final HierarchyMapper hierarchyMapper;
   private final HierarchyPropertyMapper hierarchyPropertyMapper;
   private final TestpointMapper testpointMapper;
   private final TestpointOfflineMapper testpointOfflineMapper;
   private final TestpointDataMapper testpointDataMapper;

        @Override
    public Map<String, Object> getTargetTypeList(Long hierarchyId, Long targetTypeId) {
        Map<String, Object> result = new HashMap<>();
        
        // 递归获取当前层级及所有子层级的ID列表
        Set<Long> hierarchyIds = getAllChildHierarchyIds(hierarchyId);
        
        if (hierarchyIds.isEmpty()) {
            result.put("list", Collections.emptyList());
            result.put("total", 0);
            return result;
        }
        
        // 直接查询目标类型的层级数据
        LambdaQueryWrapper<Hierarchy> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(Hierarchy::getId, hierarchyIds)
                   .eq(Hierarchy::getTypeId, targetTypeId)
                   .orderBy(true, true, Hierarchy::getId);
        
        List<Hierarchy> hierarchyList = hierarchyMapper.selectList(queryWrapper);
        
        result.put("list", hierarchyList);
        result.put("total", hierarchyList.size());
        
        return result;
    }

    /**
     * 递归获取当前层级及所有子层级的ID
     *
     * @param hierarchyId 当前层级ID
     * @return 层级ID集合
     */
    private Set<Long> getAllChildHierarchyIds(Long hierarchyId) {
        Set<Long> hierarchyIds = new HashSet<>();

        // 添加当前层级ID
        hierarchyIds.add(hierarchyId);

        // 递归查找子层级
        collectChildHierarchyIds(hierarchyId, hierarchyIds);

        return hierarchyIds;
    }

    /**
     * 递归收集子层级ID
     *
     * @param parentId 父级ID
     * @param hierarchyIds 收集结果集合
     */
    private void collectChildHierarchyIds(Long parentId, Set<Long> hierarchyIds) {
        // 查询直接子层级
        LambdaQueryWrapper<Hierarchy> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Hierarchy::getParentId, parentId);

        List<Hierarchy> children = hierarchyMapper.selectList(queryWrapper);

        for (Hierarchy child : children) {
            hierarchyIds.add(child.getId());
            // 递归查找孙层级
            collectChildHierarchyIds(child.getId(), hierarchyIds);
        }
    }

    @Override
    public Map<String, Object> getTargetTypeStatistics(Long hierarchyId,Long targetTypeId) {
        Map<String, Object> result = new HashMap<>();
        return result;
    }




    @Override
    public Map<String, Object> getEquipmentDetailStatistics(Long hierarchyId) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getTestpointDetailStatistics(Long hierarchyId) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getReportDetailStatistics(Long hierarchyId, StatisticsCountTypeEnum countType) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getOfflineDetailStatistics(Long hierarchyId, StatisticsCountTypeEnum countType) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getReportDetailStatistics(Long hierarchyId) {
        return IStatisticsService.super.getReportDetailStatistics(hierarchyId);
    }

    @Override
    public Map<String, Object> getOfflineDetailStatistics(Long hierarchyId) {
        return IStatisticsService.super.getOfflineDetailStatistics(hierarchyId);
    }

    @Override
    public Map<String, Object> getRealtimeAlarmList(Long hierarchyId, List<Integer> alarmTypes, Integer minutesAgo) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getRealtimeAlarmList(Long hierarchyId) {
        return IStatisticsService.super.getRealtimeAlarmList(hierarchyId);
    }
}
