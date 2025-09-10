package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hm.domain.*;
import org.dromara.hm.domain.bo.HierarchyTypeBo;
import org.dromara.hm.domain.bo.HierarchyTypePropertyDictBo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.domain.vo.HierarchyTypeVo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.enums.StatisticsCountTypeEnum;
import org.dromara.hm.service.*;
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
@Slf4j
public class StatisticsServiceImpl implements IStatisticsService {

    private final IHierarchyService hierarchyService;

    private final IHierarchyTypeService hierarchyTypeService;

    private final IHierarchyPropertyService hierarchyPropertyService;

    private final IHierarchyTypePropertyDictService hierarchyTypePropertyDictService;

    private final IHierarchyTypePropertyService hierarchyTypePropertyService;

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

    @Override
    public Map<String, Object> alarm(Long hierarchyId, Long targetTypeId, Integer statisticalType) {
        // 1. 查询满足条件的传感器层级类型：typeKey = sensor 且包含 dataType = 1001 的属性字典
        List<HierarchyTypeVo> sensorTypes = getSensorHierarchyTypesWithDataType1001();

        if (sensorTypes.isEmpty()) {
            log.warn("未找到满足条件的传感器层级类型（typeKey = sensor 且 dataType = 1001）");
            Map<String, Object> result = new HashMap<>();
            result.put("targetTypeId", targetTypeId);
            result.put("sensorCount", 0);
            result.put("sensorTypes", new ArrayList<>());
            return result;
        }

        // 获取所有满足条件的传感器类型ID
        List<Long> sensorTypeIds = sensorTypes.stream()
            .map(HierarchyTypeVo::getId)
            .toList();

        // 2. 查找targetTypeId类型下的所有层级
        List<Long> matchedIds = new ArrayList<>();
        findMatchingDescendants(hierarchyId, targetTypeId, matchedIds);

        // 3. 查询targetTypeId类型下的所有层级
        LambdaQueryWrapper<Hierarchy> targetTypeQuery = new LambdaQueryWrapper<Hierarchy>()
            .eq(Hierarchy::getTypeId, targetTypeId);

        if (!matchedIds.isEmpty()) {
            // 如果有级联匹配，添加到查询条件
            HierarchyTypeVo hierarchyTypeVo = hierarchyTypeService.queryById(targetTypeId);
            if (hierarchyTypeVo.getCascadeFlag() && hierarchyTypeVo.getCascadeParentId() != null) {
                targetTypeQuery.in(Hierarchy::getId, matchedIds);
            }
        }

        List<Hierarchy> targetTypeHierarchies = hierarchyService.list(targetTypeQuery);

        // 4. 统计每个targetType层级下有多少个传感器层级
        List<Map<String, Object>> sensorStatistics = new ArrayList<>();

        for (Hierarchy targetHierarchy : targetTypeHierarchies) {
            log.info("开始统计目标层级: id={}, name={}, typeId={}",
                targetHierarchy.getId(), targetHierarchy.getName(), targetHierarchy.getTypeId());

            // 为每个targetType层级查找其下的传感器层级
            List<Hierarchy> childSensorHierarchies = findSensorHierarchiesUnderTarget(targetHierarchy.getId(), sensorTypeIds);

            log.info("目标层级 {} 找到 {} 个传感器层级", targetHierarchy.getName(), childSensorHierarchies.size());

            // 验证所有找到的传感器层级是否确实是传感器类型
            for (Hierarchy sensor : childSensorHierarchies) {
                if (!sensorTypeIds.contains(sensor.getTypeId())) {
                    log.error("错误：找到的层级 {} (typeId={}) 不是传感器类型！传感器类型ID列表: {}",
                        sensor.getName(), sensor.getTypeId(), sensorTypeIds);
                }
            }

            Map<String, Object> stat = new HashMap<>();
            stat.put("targetHierarchyId", targetHierarchy.getId());
            stat.put("targetHierarchyName", targetHierarchy.getName());
            stat.put("sensorCount", childSensorHierarchies.size());
            stat.put("sensorHierarchies", childSensorHierarchies.stream()
                .map(h -> Map.of("id", h.getId(), "name", h.getName(), "typeId", h.getTypeId()))
                .toList());

            sensorStatistics.add(stat);
        }

        // 5. 计算总数
        int totalSensorCount = sensorStatistics.stream()
            .mapToInt(stat -> (Integer) stat.get("sensorCount"))
            .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("targetTypeId", targetTypeId);
        result.put("targetTypeName", hierarchyTypeService.queryById(targetTypeId).getName());
        result.put("totalSensorCount", totalSensorCount);
        result.put("sensorTypeCount", sensorTypes.size());
        result.put("sensorTypeIds", sensorTypeIds);
        result.put("statistics", sensorStatistics);

        log.info("统计完成：targetTypeId={}, 共找到{}个层级，包含{}个传感器层级",
            targetTypeId, targetTypeHierarchies.size(), totalSensorCount);

        return result;
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

    /**
     * 查询 typeKey = sensor 且包含 dataType = 1001 属性字典的层级类型
     * @return 满足条件的层级类型列表
     */
    private List<HierarchyTypeVo> getSensorHierarchyTypesWithDataType1001() {
        // 1. 查询所有 typeKey = sensor 的层级类型
        HierarchyTypeBo typeBo = new HierarchyTypeBo();
        typeBo.setTypeKey("sensor");
        List<HierarchyTypeVo> sensorTypes = hierarchyTypeService.queryList(typeBo);

        log.info("找到 {} 个 typeKey='sensor' 的层级类型", sensorTypes.size());
        for (HierarchyTypeVo type : sensorTypes) {
            log.debug("传感器类型: id={}, name={}", type.getId(), type.getName());
        }

        if (sensorTypes.isEmpty()) {
            log.warn("未找到任何 typeKey='sensor' 的层级类型");
            return new ArrayList<>();
        }

        // 2. 查询 dataType = 1001 的属性字典
        HierarchyTypePropertyDictBo dictBo = new HierarchyTypePropertyDictBo();
        dictBo.setDataType(1001);
        List<HierarchyTypePropertyDictVo> dataType1001Dicts = hierarchyTypePropertyDictService.queryList(dictBo);

        log.info("找到 {} 个 dataType=1001 的属性字典", dataType1001Dicts.size());
        for (HierarchyTypePropertyDictVo dict : dataType1001Dicts) {
            log.debug("属性字典: id={}, name={}", dict.getId(), dict.getDictName());
        }

        if (dataType1001Dicts.isEmpty()) {
            log.warn("未找到任何 dataType=1001 的属性字典");
            return new ArrayList<>();
        }

        // 获取 dataType = 1001 的字典ID列表
        List<Long> dictIds = dataType1001Dicts.stream()
            .map(HierarchyTypePropertyDictVo::getId)
            .toList();

        // 3. 过滤出包含这些字典的传感器类型
        List<HierarchyTypeVo> result = new ArrayList<>();
        for (HierarchyTypeVo sensorType : sensorTypes) {
            // 查询该类型包含的属性（通过typeId查询）
            List<HierarchyTypePropertyVo> properties = hierarchyTypePropertyService.getPropertiesByTypeId(sensorType.getId());

            log.debug("传感器类型 {} 包含 {} 个属性", sensorType.getName(), properties.size());

            // 过滤出包含dataType=1001的属性
            boolean hasDataType1001 = properties.stream()
                .anyMatch(prop -> {
                    boolean contains = dictIds.contains(prop.getPropertyDictId());
                    if (contains) {
                        log.debug("传感器类型 {} 包含 dataType=1001 的属性: propertyDictId={}",
                            sensorType.getName(), prop.getPropertyDictId());
                    }
                    return contains;
                });

            if (hasDataType1001) {
                result.add(sensorType);
                log.info("添加传感器类型到结果: id={}, name={}", sensorType.getId(), sensorType.getName());
            } else {
                log.debug("传感器类型 {} 不包含 dataType=1001 的属性，跳过", sensorType.getName());
            }
        }

        log.info("最终筛选出 {} 个满足条件的传感器类型", result.size());
        return result;
    }

    /**
     * 获取指定层级ID的 dataType = 1001 的属性
     * @param hierarchyId 层级ID
     * @return 满足条件的属性列表
     */
    private List<HierarchyProperty> getHierarchyPropertiesWithDataType1001(Long hierarchyId) {
        // 直接查询层级的属性，然后过滤出dataType=1001的属性
        LambdaQueryWrapper<HierarchyProperty> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(HierarchyProperty::getHierarchyId, hierarchyId);
        List<HierarchyProperty> allProperties = hierarchyPropertyService.list(wrapper);

        if (allProperties.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询 dataType = 1001 的属性字典ID
        HierarchyTypePropertyDictBo dictBo = new HierarchyTypePropertyDictBo();
        dictBo.setDataType(1001);
        List<HierarchyTypePropertyDictVo> dataType1001Dicts = hierarchyTypePropertyDictService.queryList(dictBo);

        if (dataType1001Dicts.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取字典ID列表
        List<Long> dictIds = dataType1001Dicts.stream()
            .map(HierarchyTypePropertyDictVo::getId)
            .toList();

        // 过滤出dataType=1001的属性
        return allProperties.stream()
            .filter(prop -> dictIds.contains(prop.getTypePropertyId()))
            .toList();
    }

    /**
     * 查找指定层级下的所有传感器层级
     * @param targetHierarchyId 目标层级ID
     * @param sensorTypeIds 传感器类型ID列表
     * @return 传感器层级列表
     */
    private List<Hierarchy> findSensorHierarchiesUnderTarget(Long targetHierarchyId, List<Long> sensorTypeIds) {
        List<Hierarchy> sensorHierarchies = new ArrayList<>();

        // 递归查找所有子孙层级
        findSensorDescendants(targetHierarchyId, sensorTypeIds, sensorHierarchies);

        return sensorHierarchies;
    }

    /**
     * 递归查找传感器类型的子孙层级
     * @param hierarchyId 当前层级ID
     * @param sensorTypeIds 传感器类型ID列表
     * @param sensorHierarchies 找到的传感器层级集合
     */
    private void findSensorDescendants(Long hierarchyId, List<Long> sensorTypeIds, List<Hierarchy> sensorHierarchies) {
        // 获取直接子级
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Hierarchy::getParentId, hierarchyId);
        List<Hierarchy> children = hierarchyService.list(wrapper);

        log.debug("层级 {} 有 {} 个直接子级", hierarchyId, children.size());

        // 检查每个子级
        for (Hierarchy child : children) {
            log.debug("检查子级: id={}, name={}, typeId={}", child.getId(), child.getName(), child.getTypeId());

            // 如果是传感器类型，添加到结果中
            if (sensorTypeIds.contains(child.getTypeId())) {
                sensorHierarchies.add(child);
                log.info("找到传感器层级: id={}, name={}, typeId={}",
                    child.getId(), child.getName(), child.getTypeId());
            } else {
                log.debug("子级 {} (typeId={}) 不是传感器类型，继续递归查找",
                    child.getName(), child.getTypeId());
                // 如果不是传感器类型，继续递归查找
                findSensorDescendants(child.getId(), sensorTypeIds, sensorHierarchies);
            }
        }
    }

    /**
     * 处理层级的属性（可以在这里添加具体的业务逻辑）
     * @param hierarchy 层级对象
     * @param properties 属性列表
     */
    private void processHierarchyProperties(Hierarchy hierarchy, List<HierarchyProperty> properties) {
        // 这里可以添加具体的业务逻辑，比如：
        // - 数据验证
        // - 数据转换
        // - 统计计算
        // - 告警判断等

        for (HierarchyProperty property : properties) {
            log.debug("处理层级 [{}] 的属性 [{}]: 值 = {}",
                hierarchy.getName(),
                property.getTypePropertyId(),
                property.getPropertyValue());
        }
    }


}
