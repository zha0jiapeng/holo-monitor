package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.enums.StatisticsCountTypeEnum;
import org.dromara.hm.mapper.HierarchyMapper;
import org.dromara.hm.service.IHierarchyService;
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

    private final IHierarchyService hierarchyService;

    @Override
    public List<Map<String, Object>> getTargetTypeList(Long hierarchyId, Long targetTypeId) {
        List<Long> list = hierarchyService.selectTargetTypeHierarchyList(hierarchyService.selectChildHierarchyIds(hierarchyId),targetTypeId);
        List<Hierarchy> hierarchies = hierarchyService.listByIds(list);

        Map<String, Long> nameStatistics = hierarchies.stream()
            .filter(h -> h.getName() != null) // 过滤掉 name 为 null 的记录
            .collect(Collectors.groupingBy(
                Hierarchy::getName,
                Collectors.counting()
            ));

        List<Map<String, Object>> resultList = nameStatistics.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());

        return resultList;
    }


    @Override
    public List<HierarchyVo> getNextHierarchyList(Long hierarchyId, Long targetTypeId) {

        // 递归获取包含目标类型的所有子孙层级，找到目标类型就停止递归
        List<Long> matchedIds = new ArrayList<>();
        findMatchingDescendants(hierarchyId, targetTypeId, matchedIds);

        if (matchedIds.isEmpty()) {
            return new ArrayList<HierarchyVo>();
        }

        return hierarchyService.selectByIds(matchedIds);
    }

    /**
     * 递归查找匹配指定类型的子孙层级，找到匹配类型就停止递归
     * @param hierarchyId 当前层级ID
     * @param targetTypeId 目标类型ID
     * @param matchedIds 匹配的层级ID集合
     */
    private void findMatchingDescendants(Long hierarchyId, Long targetTypeId, List<Long> matchedIds) {
        // 先检查当前层级是否匹配
        Hierarchy current = hierarchyService.getById(hierarchyId);
        if (current != null && targetTypeId.equals(current.getTypeId())) {
            matchedIds.add(hierarchyId);
            return; // 找到匹配的类型，停止递归
        }

        // 获取直接子级
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Hierarchy::getParentId, hierarchyId);
        List<Hierarchy> children = hierarchyService.list(wrapper);

        // 递归查找子级，但只在子级不匹配目标类型时才继续
        for (Hierarchy child : children) {
            if (!targetTypeId.equals(child.getTypeId())) {
                // 子级不匹配目标类型，继续递归查找
                findMatchingDescendants(child.getId(), targetTypeId, matchedIds);
            } else {
                // 子级匹配目标类型，添加到结果中
                matchedIds.add(child.getId());
            }
        }
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
