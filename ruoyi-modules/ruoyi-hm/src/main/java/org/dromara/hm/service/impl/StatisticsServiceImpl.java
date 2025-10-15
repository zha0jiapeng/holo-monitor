package org.dromara.hm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.MPIDMultipleJson;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.domain.*;
import org.dromara.hm.domain.sd400mp.MPEventList;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.domain.vo.HierarchyTypeVo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.enums.DataTypeEnum;
import org.dromara.hm.service.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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


    private final EventParserService eventParserService;

    // ==================== 公共方法区域 ====================

    /**
     * 获取级联子类型ID列表
     *
     * @param parentTypeId 父类型ID
     * @return 包含父类型在内的所有级联子类型ID列表
     */
    private List<Long> getCascadeChildTypeIds(Long parentTypeId) {
        List<Long> typeIds = new ArrayList<>();
        typeIds.add(parentTypeId);

            // 批量查询所有级联子类型，避免循环查询
            List<HierarchyType> allChildTypes = hierarchyTypeService.list();
            Map<Long, List<HierarchyType>> parentToChildMap = allChildTypes.stream()
                .filter(ht -> ht.getCascadeParentId() != null)
                .collect(Collectors.groupingBy(HierarchyType::getCascadeParentId));

            // 使用队列进行广度优先遍历，收集所有子类型
            Queue<Long> queue = new LinkedList<>();
        queue.offer(parentTypeId);

            while (!queue.isEmpty()) {
                Long currentTypeId = queue.poll();
                List<HierarchyType> children = parentToChildMap.get(currentTypeId);
                if (children != null) {
                    for (HierarchyType child : children) {
                        if (!typeIds.contains(child.getId())) {
                            typeIds.add(child.getId());
                            queue.offer(child.getId());
                        }
                    }
                }
            }
        return typeIds;
    }

    /**
     * 根据字典key获取HierarchyTypePropertyDict
     *
     * @param dictKey 字典key
     * @return HierarchyTypePropertyDict对象，未找到返回null
     */
    private HierarchyTypePropertyDict getPropertyDictByKey(String dictKey) {
        return hierarchyTypePropertyDictService.lambdaQuery()
                .eq(HierarchyTypePropertyDict::getDictKey, dictKey)
                .one();
    }

    /**
     * 根据字典ID和类型ID获取HierarchyTypeProperty
     *
     * @param dictId 字典ID
     * @param typeId 类型ID
     * @return HierarchyTypeProperty对象，未找到返回null
     */
    private HierarchyTypeProperty getTypePropertyByDictAndType(Long dictId, Long typeId) {
        return hierarchyTypePropertyService.lambdaQuery()
                .eq(HierarchyTypeProperty::getPropertyDictId, dictId)
                .eq(HierarchyTypeProperty::getTypeId, typeId)
                .one();
    }

    /**
     * 解析逗号分隔的传感器ID字符串
     *
     * @param propertyValue 属性值（逗号分隔的ID字符串）
     * @return 传感器ID列表
     */
    private List<Long> parseSensorIds(String propertyValue) {
        List<Long> sensorIds = new ArrayList<>();
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            String[] split = propertyValue.split("\\,");
            for (String s : split) {
                try {
                    sensorIds.add(Long.parseLong(s.trim()));
                } catch (NumberFormatException e) {
                    log.warn("解析传感器ID失败: {}", s);
                }
            }
        }
        return sensorIds;
    }

    /**
     * 获取传感器通过设备类型绑定
     *
     * @param hierarchyId 层级ID
     * @param typeIds 类型ID列表
     * @return 传感器ID列表
     */
    private List<Long> getSensorIdsByDeviceBinding(Long hierarchyId, List<Long> typeIds) {
        // 查找sensors字典属性
        HierarchyTypePropertyDict sensorsDict = getPropertyDictByKey("sensors");
        if (sensorsDict == null) {
            log.warn("未找到sensors字典属性");
            return new ArrayList<>();
        }

            List<HierarchyTypeProperty> list = hierarchyTypePropertyService.lambdaQuery()
                    .eq(HierarchyTypeProperty::getPropertyDictId, sensorsDict.getId())
                    .in(HierarchyTypeProperty::getTypeId, typeIds).list();

        List<Long> typePropertyIds = list.stream()
                .map(HierarchyTypeProperty::getId)
                .collect(Collectors.toList());

        if (typePropertyIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取hierarchyId范围内符合typeIds的设备层级
        Set<Long> hierarchyDescendants = getAllDescendantIds(hierarchyId);
        List<Hierarchy> deviceHierarchies = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, hierarchyDescendants)
                .in(Hierarchy::getTypeId, typeIds)
                .list();

        if (deviceHierarchies.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> deviceHierarchyIds = deviceHierarchies.stream()
                .map(Hierarchy::getId)
                .collect(Collectors.toList());

        // 查找这些设备的传感器绑定属性
            List<HierarchyProperty> properties = hierarchyPropertyService.lambdaQuery()
                .ne(HierarchyProperty::getPropertyValue, "")
                .in(HierarchyProperty::getTypePropertyId, typePropertyIds)
                .in(HierarchyProperty::getHierarchyId, deviceHierarchyIds)
                .list();

        // 解析传感器ID列表
            List<Long> sensorIds = new ArrayList<>();
            for (HierarchyProperty property : properties) {
            sensorIds.addAll(parseSensorIds(property.getPropertyValue()));
        }

        return sensorIds;
    }

    /**
     * 获取传感器到device_point的映射关系
     *
     * @param hierarchyId 起始层级ID
     * @param typeIds 类型ID列表（device和device_point）
     * @return Map<传感器ID, device_point的Hierarchy对象>
     */
    private Map<Long, Hierarchy> getSensorToDevicePointMapping(Long hierarchyId, List<Long> typeIds) {
        // 查找sensors字典属性
        HierarchyTypePropertyDict sensorsDict = getPropertyDictByKey("sensors");
        if (sensorsDict == null) {
            log.warn("未找到sensors字典属性");
            return new HashMap<>();
        }

        List<HierarchyTypeProperty> list = hierarchyTypePropertyService.lambdaQuery()
                .eq(HierarchyTypeProperty::getPropertyDictId, sensorsDict.getId())
                .in(HierarchyTypeProperty::getTypeId, typeIds).list();

        List<Long> typePropertyIds = list.stream()
                .map(HierarchyTypeProperty::getId)
                .collect(Collectors.toList());

        if (typePropertyIds.isEmpty()) {
            return new HashMap<>();
        }

        // 获取hierarchyId范围内符合typeIds的设备层级
        Set<Long> hierarchyDescendants = getAllDescendantIds(hierarchyId);
        List<Hierarchy> deviceHierarchies = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, hierarchyDescendants)
                .in(Hierarchy::getTypeId, typeIds)
                .list();

        if (deviceHierarchies.isEmpty()) {
            return new HashMap<>();
        }

        // 创建device_point的typeId集合（只统计device_point）
        HierarchyType devicePointType = getHierarchyTypeByKey("device_point");
        if (devicePointType == null) {
            log.warn("未找到device_point类型");
            return new HashMap<>();
        }

        List<Long> deviceHierarchyIds = deviceHierarchies.stream()
                .map(Hierarchy::getId)
                .collect(Collectors.toList());

        // 查找这些设备的传感器绑定属性
        List<HierarchyProperty> properties = hierarchyPropertyService.lambdaQuery()
                .ne(HierarchyProperty::getPropertyValue, "")
                .in(HierarchyProperty::getTypePropertyId, typePropertyIds)
                .in(HierarchyProperty::getHierarchyId, deviceHierarchyIds)
                .list();

        // 建立传感器到device_point的映射（只映射device_point类型的设备）
        Map<Long, Hierarchy> sensorToDevicePointMap = new HashMap<>();
        for (HierarchyProperty property : properties) {
            Hierarchy deviceHierarchy = deviceHierarchies.stream()
                    .filter(h -> h.getId().equals(property.getHierarchyId()))
                    .findFirst()
                    .orElse(null);

            // 只处理device_point类型
            if (deviceHierarchy != null && deviceHierarchy.getTypeId().equals(devicePointType.getId())) {
                List<Long> sensorIds = parseSensorIds(property.getPropertyValue());
                for (Long sensorId : sensorIds) {
                    sensorToDevicePointMap.put(sensorId, deviceHierarchy);
                }
            }
        }

        log.info("建立了 {} 个传感器到device_point的映射关系", sensorToDevicePointMap.size());
        return sensorToDevicePointMap;
    }

    /**
     * 获取指定字典key和类型的层级ID列表（属性值大于0）
     *
     * @param dictKey 字典key
     * @param typeId 类型ID
     * @return 层级ID列表
     */
    private List<Long> getHierarchyIdsByDictAndType(String dictKey, Long typeId) {
        HierarchyTypePropertyDict dict = getPropertyDictByKey(dictKey);
        if (dict == null) {
            log.warn("未找到{}字典属性", dictKey);
            return new ArrayList<>();
        }

        HierarchyTypeProperty typeProperty = getTypePropertyByDictAndType(dict.getId(), typeId);
        if (typeProperty == null) {
            log.warn("类型{}未配置{}属性", typeId, dictKey);
            return new ArrayList<>();
        }

        List<HierarchyProperty> properties = hierarchyPropertyService.lambdaQuery()
                .eq(HierarchyProperty::getTypePropertyId, typeProperty.getId())
                .list();

        return properties.stream()
                .filter(this::isValidPropertyValue)
                .map(HierarchyProperty::getHierarchyId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 判断设备类型key是否为设备相关类型
     *
     * @param typeKey 类型key
     * @return 是否为设备类型
     */
    private boolean isDeviceType(String typeKey) {
        List<String> devices = List.of("device_group", "device", "device_point");
        return devices.contains(typeKey);
    }

    /**
     * 根据类型key列表获取类型ID列表
     *
     * @param typeKeys 类型key列表
     * @return 类型ID列表
     */
    private List<Long> getTypeIdsByKeys(List<String> typeKeys) {
        List<HierarchyType> types = hierarchyTypeService.lambdaQuery()
                .in(HierarchyType::getTypeKey, typeKeys)
                .list();
        return types.stream()
                .map(HierarchyType::getId)
                .collect(Collectors.toList());
    }

    /**
     * 根据typeKey获取HierarchyType
     *
     * @param typeKey 类型key
     * @return HierarchyType对象，未找到返回null
     */
    private HierarchyType getHierarchyTypeByKey(String typeKey) {
        return hierarchyTypeService.getOne(
                new LambdaQueryWrapper<HierarchyType>().eq(HierarchyType::getTypeKey, typeKey));
    }

    /**
     * 构建统计结果项
     *
     * @param name 名称
     * @param count 数量
     * @return 统计结果项
     */
    private Map<String, Object> buildResultItem(String name, Long count) {
        Map<String, Object> resultItem = new HashMap<>();
        resultItem.put("name", name);
        resultItem.put("count", count);
        return resultItem;
    }

    /**
     * 构建统计结果项（包含sort和icon）
     *
     * @param name 名称
     * @param count 数量
     * @param sort 排序值
     * @param icon 图标值
     * @return 统计结果项
     */
    private Map<String, Object> buildResultItem(String name, Long count, String sort, String icon) {
        Map<String, Object> resultItem = new HashMap<>();
        resultItem.put("name", name);
        resultItem.put("count", count);
        if (sort != null && !sort.isEmpty()) {
            resultItem.put("sort", sort);
        }
        if (icon != null && !icon.isEmpty()) {
            resultItem.put("icon", icon);
        }
        return resultItem;
    }

    /**
     * 按count降序排列结果列表
     *
     * @param result 结果列表
     */
    private void sortResultByCountDesc(List<Map<String, Object>> result) {
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
    }

    @Override
    public List<Map<String, Object>> getTargetTypeList(Long hierarchyId, Long targetTypeId, Long statisticsTypeId) {
        Hierarchy h = hierarchyService.getById(hierarchyId);
        HierarchyType type = hierarchyTypeService.getById(h.getTypeId());

        if (isDeviceType(type.getTypeKey())) {
            // 设备类型：通过传感器绑定统计
            return getTargetTypeListByDeviceBinding(hierarchyId, targetTypeId, type);
        } else {
            // 非设备类型：通过层级关系统计
            return getTargetTypeListByHierarchyRelation(hierarchyId, targetTypeId, statisticsTypeId);
        }
    }

    /**
     * 设备类型通过传感器绑定统计目标类型列表
     */
    private List<Map<String, Object>> getTargetTypeListByDeviceBinding(Long hierarchyId, Long targetTypeId, HierarchyType type) {
        // 获取级联子类型ID
        List<Long> typeIds = getCascadeChildTypeIds(type.getId());

        // 获取传感器ID列表
        List<Long> sensorIds = getSensorIdsByDeviceBinding(hierarchyId, typeIds);

            // 目标层级类型具体层级
            List<Hierarchy> targetHierarchys = hierarchyService.lambdaQuery()
                .eq(Hierarchy::getTypeId, targetTypeId).list();

            // 根据传感器ID列表，统计每个目标层级下的传感器数量
            Map<Long, Long> sensorCountMap = new HashMap<>();

        if (!sensorIds.isEmpty()) {
            // 批量查询传感器层级信息，避免N+1查询问题
            List<Hierarchy> sensorHierarchies = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, sensorIds)
                .list();

            // 批量查找传感器所属的目标层级，避免重复递归查询
            Map<Long, Long> sensorToTargetMap = findBelongingTargetHierarchiesBatch(
                sensorHierarchies.stream().map(Hierarchy::getId).collect(Collectors.toList()),
                targetTypeId
            );

            // 统计每个目标层级的传感器数量
            for (Map.Entry<Long, Long> entry : sensorToTargetMap.entrySet()) {
                Long targetHierarchyId = entry.getValue();
                sensorCountMap.put(targetHierarchyId, sensorCountMap.getOrDefault(targetHierarchyId, 0L) + 1);
            }
            }

        // 查询目标类型的 sort 和 icon 属性值
        List<Long> targetHierarchyIds = targetHierarchys.stream().map(Hierarchy::getId).collect(Collectors.toList());
        Map<Long, String> sortMap = getSortValuesForHierarchies(targetHierarchyIds);
        Map<Long, String> iconMap = getIconValuesForHierarchies(targetHierarchyIds);

        // 构造返回结果 - 返回所有目标层级,包括count为0的
        List<Map<String, Object>> result = new ArrayList<>();
        for (Hierarchy targetHierarchy : targetHierarchys) {
            Long count = sensorCountMap.getOrDefault(targetHierarchy.getId(), 0L);
            String sort = sortMap.get(targetHierarchy.getId());
            String icon = iconMap.get(targetHierarchy.getId());
            result.add(buildResultItem(targetHierarchy.getName(), count, sort, icon));
        }

        // 按count降序排列
        sortResultByCountDesc(result);
        return result;
    }

    /**
     * 非设备类型通过层级关系统计目标类型列表
     */
    private List<Map<String, Object>> getTargetTypeListByHierarchyRelation(Long hierarchyId, Long targetTypeId, Long statisticsTypeId) {
            List<Map<String, Object>> result = new ArrayList<>();

            // 直接查询所有targetTypeId类型的层级（统计维度，如±1100kV、±800kV等）
            List<Hierarchy> targetHierarchies = hierarchyService.lambdaQuery()
                    .eq(Hierarchy::getTypeId, targetTypeId)
                    .list();

            if (targetHierarchies.isEmpty()) {
                return result;
            }

            // 提取所有目标层级ID
            List<Long> targetHierarchyIds = targetHierarchies.stream()
                .map(Hierarchy::getId)
                .collect(Collectors.toList());

            // 从hierarchyId开始，查找statisticsTypeId类型的层级（通过parentId关系或属性关系）
            List<Long> statisticsTypeHierarchyIds = findHierarchiesByTypeIncludingPropertyRelation(hierarchyId, statisticsTypeId);

        if (statisticsTypeHierarchyIds.isEmpty()) {
            // 如果没有statisticsTypeId类型的层级，直接返回空结果
            return result;
        }

        // 批量查询层级属性：只查询statisticsTypeId类型层级中property_value为目标层级ID的属性记录
        List<String> targetIdStrings = targetHierarchyIds.stream()
            .map(String::valueOf)
            .collect(Collectors.toList());

        List<HierarchyProperty> relatedProperties = hierarchyPropertyService.list(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .in(HierarchyProperty::getHierarchyId, statisticsTypeHierarchyIds)
                .in(HierarchyProperty::getPropertyValue, targetIdStrings)
        );

        if (relatedProperties.isEmpty()) {
            // 如果没有关联属性，直接返回空结果
            return result;
        }

        // 批量查询类型属性
        Set<Long> typePropertyIds = relatedProperties.stream()
            .map(HierarchyProperty::getTypePropertyId)
            .collect(Collectors.toSet());

        List<HierarchyTypeProperty> typeProperties = hierarchyTypePropertyService.list(
            Wrappers.<HierarchyTypeProperty>lambdaQuery()
                .in(HierarchyTypeProperty::getId, typePropertyIds)
        );

        Map<Long, HierarchyTypeProperty> typePropertyMap = typeProperties.stream()
            .collect(Collectors.toMap(HierarchyTypeProperty::getId, tp -> tp));

        // 批量查询属性字典，筛选data_type=1001的
        Set<Long> dictIds = typeProperties.stream()
            .map(HierarchyTypeProperty::getPropertyDictId)
            .collect(Collectors.toSet());

        List<HierarchyTypePropertyDict> dicts = hierarchyTypePropertyDictService.list(
            Wrappers.<HierarchyTypePropertyDict>lambdaQuery()
                .in(HierarchyTypePropertyDict::getId, dictIds)
                .eq(HierarchyTypePropertyDict::getDataType, DataTypeEnum.HIERARCHY.getCode())
        );

        Set<Long> validDictIds = dicts.stream()
            .map(HierarchyTypePropertyDict::getId)
            .collect(Collectors.toSet());

        // 统计每个目标层级的关联数量
        Map<Long, Long> targetCountMap = new HashMap<>();

        for (HierarchyProperty property : relatedProperties) {
            HierarchyTypeProperty typeProperty = typePropertyMap.get(property.getTypePropertyId());

            if (typeProperty != null && validDictIds.contains(typeProperty.getPropertyDictId())) {
                // 符合条件：来自statisticsTypeId类型层级 且 data_type=1001
                Long targetHierarchyId = Long.valueOf(property.getPropertyValue());
                targetCountMap.put(targetHierarchyId, targetCountMap.getOrDefault(targetHierarchyId, 0L) + 1);
            }
        }

        // 查询目标类型的 sort 和 icon 属性值
        Map<Long, String> sortMap = getSortValuesForHierarchies(targetHierarchyIds);
        Map<Long, String> iconMap = getIconValuesForHierarchies(targetHierarchyIds);

        // 构造返回结果 - 返回所有目标层级,包括count为0的
        for (Hierarchy targetHierarchy : targetHierarchies) {
            Long count = targetCountMap.getOrDefault(targetHierarchy.getId(), 0L);
            String sort = sortMap.get(targetHierarchy.getId());
            String icon = iconMap.get(targetHierarchy.getId());
            result.add(buildResultItem(targetHierarchy.getName(), count, sort, icon));
        }

        // 按count降序排列
        sortResultByCountDesc(result);
        return result;
    }

    @Override
    public List<HierarchyVo> getNextHierarchyList(Long hierarchyId, Long targetTypeId) {

        // 递归获取包含目标类型的所有子孙层级，找到目标类型就停止递归
        List<Long> matchedIds = new ArrayList<>();
        findMatchingDescendants(hierarchyId, targetTypeId, matchedIds);

        if (matchedIds.isEmpty()) {
            // 如果没有找到关联的目标类型层级，直接返回数据库中所有targetTypeId类型的层级
            log.info("从hierarchyId={}未找到targetTypeId={}的子孙层级，返回全部该类型层级", hierarchyId, targetTypeId);
            List<Hierarchy> allTargetTypeHierarchies = hierarchyService.lambdaQuery()
                    .eq(Hierarchy::getTypeId, targetTypeId)
                    .list();

            if (allTargetTypeHierarchies.isEmpty()) {
                return new ArrayList<>();
            }

            List<Long> allTargetIds = allTargetTypeHierarchies.stream()
                    .map(Hierarchy::getId)
                    .collect(Collectors.toList());
            return hierarchyService.selectByIds(allTargetIds, true);
        }
        return hierarchyService.selectByIds(matchedIds, true);
    }

    @Override
    public Map<String, Object> alarm(Long hierarchyId, Long targetTypeId, Integer statisticalType) {
        if (statisticalType == null) {
            statisticalType = 2; // 默认为被监测设备统计
        }

        // 通用前置逻辑：获取传感器类型
        HierarchyType sensorHierarchyType = getHierarchyTypeByKey("sensor");
        if (sensorHierarchyType == null) {
            return createEmptyResult(targetTypeId, statisticalType);
        }

        // 判断hierarchyId的类型，决定使用哪种统计策略
        Hierarchy hierarchy = hierarchyService.getById(hierarchyId);
        HierarchyType hierarchyType = hierarchyTypeService.getById(hierarchy.getTypeId());

        // 先检查 targetTypeId 是否在层级树下（适用于所有类型）
        List<Long> matchedIds = new ArrayList<>();
        findMatchingDescendants(hierarchyId, targetTypeId, matchedIds);
        
        if (matchedIds.isEmpty()) {
            // targetTypeId 不在层级树下，使用属性维度统计
            log.info("hierarchyId={} 下未找到 targetTypeId={} 的层级，使用属性维度统计", hierarchyId, targetTypeId);
            return alarmByPropertyDimension(hierarchyId, targetTypeId, statisticalType, sensorHierarchyType);
        }

        // targetTypeId 在层级树下，根据 hierarchyId 类型选择统计策略
        if (isDeviceType(hierarchyType.getTypeKey())) {
            // 设备类型：使用反向统计逻辑
            return alarmByReverseStatistics(hierarchyId, targetTypeId, statisticalType, sensorHierarchyType, hierarchyType);
        } else {
            // 非设备类型：使用正向统计逻辑
            List<Hierarchy> targetHierarchies = getTargetHierarchies(hierarchyId, targetTypeId);
            if (targetHierarchies.isEmpty()) {
                return createEmptyResult(targetTypeId, statisticalType);
            }

            log.info("找到 {} 个目标层级进行统计", targetHierarchies.size());

            if (statisticalType == 1) {
                // 统计维度：按设备关联的传感器统计
                return alarmByDeviceLevel(targetHierarchies, sensorHierarchyType, targetTypeId);
            } else {
                // 被监测设备统计：按目标层级下属传感器统计
                return alarmByMonitoredDevice(targetHierarchies, sensorHierarchyType, targetTypeId);
            }
        }
    }

    /**
     * 设备类型反向统计：从设备出发，通过绑定的传感器反向统计到目标层级
     */
    private Map<String, Object> alarmByReverseStatistics(Long hierarchyId, Long targetTypeId, Integer statisticalType,
            HierarchyType sensorHierarchyType, HierarchyType hierarchyType) {
        log.info("执行设备反向统计 - hierarchyId: {}, targetTypeId: {}, statisticalType: {}",
                hierarchyId, targetTypeId, statisticalType);

        // 获取hierarchyId及其级联子类型的所有typeIds
        List<Long> typeIds;

        // 对于statisticalType=1，只统计device和device_point
        if (statisticalType == 1) {
            typeIds = getTypeIdsByKeys(List.of("device", "device_point"));
        } else {
            // 获取级联子类型
            typeIds = getCascadeChildTypeIds(hierarchyType.getId());
        }

        // 获取传感器ID列表
        List<Long> sensorIds = getSensorIdsByDeviceBinding(hierarchyId, typeIds);
        if (sensorIds.isEmpty()) {
            log.warn("未找到有效的传感器");
            return createEmptyResult(targetTypeId, statisticalType);
        }

        log.info("找到 {} 个传感器进行反向统计", sensorIds.size());

        // 先找到hierarchyId向上查找到的type=7的层级
        Long type7HierarchyId = findType7Upward(hierarchyId);
        if (type7HierarchyId == null) {
            log.warn("从hierarchyId={}向上未找到type=7的层级", hierarchyId);
            return createEmptyResult(targetTypeId, statisticalType);
        }

        // 从type=7层级向下查找所有targetTypeId类型的三级设备
        List<Hierarchy> targetHierarchies = findTargetHierarchiesUnderType7(type7HierarchyId, targetTypeId);

        if (targetHierarchies.isEmpty()) {
            log.warn("在type=7层级{}下未找到targetTypeId={}的目标层级", type7HierarchyId, targetTypeId);
            return createEmptyResult(targetTypeId, statisticalType);
        }

        log.info("在type=7层级{}下找到{}个目标层级", type7HierarchyId, targetHierarchies.size());

        // 反向查找传感器所属的目标层级（向上查找三级系统）
        Map<Long, Long> sensorToTargetMap = new HashMap<>();
        if (!sensorIds.isEmpty()) {
            sensorToTargetMap = findSensorBelongingToTargetType(sensorIds, targetTypeId);
        }

        if (statisticalType == 1) {
            // 统计维度：按device_point报警统计
            // 获取传感器到device_point的映射
            Map<Long, Hierarchy> sensorToDevicePointMap = getSensorToDevicePointMapping(hierarchyId, typeIds);
            return alarmByReverseDeviceLevel(targetHierarchies, sensorToTargetMap, sensorToDevicePointMap, sensorHierarchyType, targetTypeId);
        } else {
            // 被监测设备统计：按传感器统计
            return alarmByReverseSensorLevel(targetHierarchies, sensorToTargetMap, sensorHierarchyType, targetTypeId);
        }
    }

    /**
     * 反向统计：按device_point报警统计
     */
    private Map<String, Object> alarmByReverseDeviceLevel(List<Hierarchy> targetHierarchies, Map<Long, Long> sensorToTargetMap,
            Map<Long, Hierarchy> sensorToDevicePointMap, HierarchyType sensorHierarchyType, Long targetTypeId) {
        log.info("执行反向device_point报警统计");

        // 获取所有传感器的报警级别
        Map<Long, Integer> sensorAlarmLevels = getSensorAlarmLevels(sensorHierarchyType.getId());

        // 建立device_point到传感器的映射
        Map<Long, List<Long>> devicePointToSensorMap = new HashMap<>();
        for (Map.Entry<Long, Hierarchy> entry : sensorToDevicePointMap.entrySet()) {
            Long sensorId = entry.getKey();
            Hierarchy devicePoint = entry.getValue();
            devicePointToSensorMap.computeIfAbsent(devicePoint.getId(), k -> new ArrayList<>()).add(sensorId);
        }

        // 建立device_point到目标层级的映射（通过传感器）
        Map<Long, Long> devicePointToTargetMap = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : devicePointToSensorMap.entrySet()) {
            Long devicePointId = entry.getKey();
            List<Long> sensorIds = entry.getValue();

            // 找到这个device_point的任一传感器所属的目标层级
            for (Long sensorId : sensorIds) {
                Long targetId = sensorToTargetMap.get(sensorId);
                if (targetId != null) {
                    devicePointToTargetMap.put(devicePointId, targetId);
                    break;
                }
            }
        }

        // 按目标层级分组device_point
        Map<Long, List<Long>> targetToDevicePointMap = new HashMap<>();
        for (Map.Entry<Long, Long> entry : devicePointToTargetMap.entrySet()) {
            Long devicePointId = entry.getKey();
            Long targetId = entry.getValue();
            targetToDevicePointMap.computeIfAbsent(targetId, k -> new ArrayList<>()).add(devicePointId);
        }

        // 统计每个目标层级的报警情况
        List<Map<String, Object>> statistics = new ArrayList<>();
        int totalDeviceCount = 0;
        int totalAlarmDeviceCount = 0;
        int totalGeneralCount = 0;
        int totalSeriousCount = 0;

        // 统计每个目标层级
        for (Hierarchy targetHierarchy : targetHierarchies) {
            List<Long> devicePointIdsUnderTarget = targetToDevicePointMap.getOrDefault(targetHierarchy.getId(), new ArrayList<>());

            // 统计该层级下的报警device_point
            List<Map<String, Object>> alarmDevices = new ArrayList<>();
            int hierarchyAlarmDeviceCount = 0;
            int hierarchyGeneralCount = 0;
            int hierarchySeriousCount = 0;

            for (Long devicePointId : devicePointIdsUnderTarget) {
                List<Long> sensorIds = devicePointToSensorMap.get(devicePointId);

                // 计算该device_point的最高报警级别
                int maxAlarmLevel = 0;
                for (Long sensorId : sensorIds) {
                    Integer alarmLevel = sensorAlarmLevels.get(sensorId);
                    if (alarmLevel != null && alarmLevel > maxAlarmLevel) {
                        maxAlarmLevel = alarmLevel;
                    }
                }

                // 统计计数
                if (maxAlarmLevel > 0) {
                    hierarchyAlarmDeviceCount++;
                    if (maxAlarmLevel == 1) {
                        hierarchyGeneralCount++;
                    } else if (maxAlarmLevel == 2 || maxAlarmLevel == 3) {
                        hierarchySeriousCount++;
                    }

                    // 获取device_point信息
                    Hierarchy devicePoint = sensorToDevicePointMap.values().stream()
                            .filter(dp -> dp.getId().equals(devicePointId))
                            .findFirst()
                            .orElse(null);

                    if (devicePoint != null) {
                        Map<String, Object> alarmDevice = new HashMap<>();
                        alarmDevice.put("id", devicePoint.getId());
                        alarmDevice.put("name", devicePoint.getName());
                        alarmDevice.put("typeId", devicePoint.getTypeId());
                        alarmDevice.put("report_st", maxAlarmLevel);
                        alarmDevices.add(alarmDevice);
                    }
                }
            }

            // 创建统计记录
            Map<String, Object> hierarchyStat = new HashMap<>();
            hierarchyStat.put("targetHierarchyName", targetHierarchy.getName());
            hierarchyStat.put("totalDeviceCount", devicePointIdsUnderTarget.size());
            hierarchyStat.put("alarmDeviceCount", hierarchyAlarmDeviceCount);
            hierarchyStat.put("generalCount", hierarchyGeneralCount);
            hierarchyStat.put("seriousCount", hierarchySeriousCount);
            hierarchyStat.put("alarmDevices", alarmDevices);

            statistics.add(hierarchyStat);

            totalDeviceCount += devicePointIdsUnderTarget.size();
            totalAlarmDeviceCount += hierarchyAlarmDeviceCount;
            totalGeneralCount += hierarchyGeneralCount;
            totalSeriousCount += hierarchySeriousCount;
        }

        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("statisticalType", 1);
        result.put("targetTypeId", targetTypeId);
        result.put("totalDeviceCount", totalDeviceCount);
        result.put("totalAlarmDeviceCount", totalAlarmDeviceCount);
        result.put("totalOfflineDeviceCount", 0); // 反向统计暂不支持离线设备统计
        result.put("totalGeneralCount", totalGeneralCount);
        result.put("totalSeriousCount", totalSeriousCount);
        result.put("statistics", statistics);

        log.info("反向device_point统计完成：共{}个device_point，{}个报警device_point（一般{}个，严重{}个）",
                totalDeviceCount, totalAlarmDeviceCount, totalGeneralCount, totalSeriousCount);

        return result;
    }

    /**
     * 反向统计：按传感器报警统计
     */
    private Map<String, Object> alarmByReverseSensorLevel(List<Hierarchy> targetHierarchies, Map<Long, Long> sensorToTargetMap,
            HierarchyType sensorHierarchyType, Long targetTypeId) {
        log.info("执行反向传感器报警统计");

        // 获取报警和离线传感器ID列表
        List<Long> alarmSensorIds = getReportStHierarchyIds(sensorHierarchyType.getId());
        List<Long> offlineSensorIds = getOfflineFlagHierarchyIds(sensorHierarchyType.getId());

        // 按目标层级分组统计传感器
        Map<Long, List<Long>> targetToSensorMap = new HashMap<>();
        for (Map.Entry<Long, Long> entry : sensorToTargetMap.entrySet()) {
            Long sensorId = entry.getKey();
            Long targetId = entry.getValue();
            targetToSensorMap.computeIfAbsent(targetId, k -> new ArrayList<>()).add(sensorId);
        }

        // 统计每个目标层级的传感器报警情况
        List<Map<String, Object>> statistics = new ArrayList<>();
        int totalAlarmSensorCount = 0;
        int totalOfflineSensorCount = 0;
        int totalSensorCount = 0;

        // 统计每个目标层级（确保所有目标层级都被统计，即使没有传感器）
        for (Hierarchy targetHierarchy : targetHierarchies) {
            List<Long> sensorsUnderTarget = targetToSensorMap.getOrDefault(targetHierarchy.getId(), new ArrayList<>());

            // 获取传感器详细信息
            List<Hierarchy> allSensors = new ArrayList<>();
            List<Hierarchy> alarmSensors = new ArrayList<>();
            List<Hierarchy> offlineSensors = new ArrayList<>();

            if (!sensorsUnderTarget.isEmpty()) {
                // 批量查询传感器详细信息
                allSensors = hierarchyService.lambdaQuery()
                        .in(Hierarchy::getId, sensorsUnderTarget)
                        .list();

                // 分类统计报警和离线传感器
                for (Hierarchy sensor : allSensors) {
                    if (alarmSensorIds.contains(sensor.getId())) {
                        alarmSensors.add(sensor);
                    }
                    if (offlineSensorIds.contains(sensor.getId())) {
                        offlineSensors.add(sensor);
                    }
                }
            }

            totalSensorCount += allSensors.size();
            totalAlarmSensorCount += alarmSensors.size();
            totalOfflineSensorCount += offlineSensors.size();

            // 创建统计记录（无论是否有传感器都要创建记录）
            Map<String, Object> hierarchyStat = new HashMap<>();
            hierarchyStat.put("targetHierarchyName", targetHierarchy.getName());
            hierarchyStat.put("totalSensorCount", allSensors.size());
            hierarchyStat.put("alarmSensorCount", alarmSensors.size());
            hierarchyStat.put("alarmSensors", alarmSensors.stream()
                    .map(h -> Map.of("id", h.getId(), "name", h.getName(), "typeId", h.getTypeId()))
                    .collect(Collectors.toList()));
            hierarchyStat.put("offlineSensorCount", offlineSensors.size());
            hierarchyStat.put("offlineSensors", offlineSensors.stream()
                    .map(h -> Map.of("id", h.getId(), "name", h.getName(), "typeId", h.getTypeId()))
                    .collect(Collectors.toList()));

            statistics.add(hierarchyStat);
        }

        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("statisticalType", 2);
        result.put("targetTypeId", targetTypeId);
        result.put("totalSensorCount", totalSensorCount);
        result.put("totalAlarmSensorCount", totalAlarmSensorCount);
        result.put("totalOfflineSensorCount", totalOfflineSensorCount);
        result.put("statistics", statistics);

        return result;
    }

    /**
     * 根据hierarchyId类型获取传感器列表
     * 设备类型：通过属性绑定查找传感器
     * 其他类型：递归查找传感器
     */
    private List<Hierarchy> getSensorsForHierarchy(Long hierarchyId, Long sensorTypeId) {
        // 判断hierarchyId的类型
        List<String> devices = List.of("device_group", "device", "device_point");
        Hierarchy hierarchy = hierarchyService.getById(hierarchyId);
        HierarchyType hierarchyType = hierarchyTypeService.getById(hierarchy.getTypeId());

        if (devices.contains(hierarchyType.getTypeKey())) {
            // 设备类型：通过属性绑定查找传感器
            return getSensorsByPropertyBinding(hierarchyId, hierarchyType, sensorTypeId);
        } else {
            // 其他类型：递归查找传感器
            return findAllSensorsUnderTarget(hierarchyId, sensorTypeId);
        }
    }

    /**
     * 设备类型通过属性绑定查找传感器
     */
    private List<Hierarchy> getSensorsByPropertyBinding(Long hierarchyId, HierarchyType hierarchyType, Long sensorTypeId) {
        log.info("设备类型{}通过属性绑定查找传感器", hierarchyType.getTypeKey());

        // 获取hierarchyId及其级联子类型的所有typeIds
        List<Long> typeIds = new ArrayList<>();
        typeIds.add(hierarchyType.getId());

        // 获取级联子类型
        List<HierarchyType> allChildTypes = hierarchyTypeService.list();
        Map<Long, List<HierarchyType>> parentToChildMap = allChildTypes.stream()
                .filter(ht -> ht.getCascadeParentId() != null)
                .collect(Collectors.groupingBy(HierarchyType::getCascadeParentId));

        Queue<Long> queue = new LinkedList<>();
        queue.offer(hierarchyType.getId());

        while (!queue.isEmpty()) {
            Long currentTypeId = queue.poll();
            List<HierarchyType> children = parentToChildMap.get(currentTypeId);
            if (children != null) {
                for (HierarchyType child : children) {
                    if (!typeIds.contains(child.getId())) {
                        typeIds.add(child.getId());
                        queue.offer(child.getId());
                    }
                }
            }
        }

        // 查找sensors字典属性
        HierarchyTypePropertyDict sensorsDict = hierarchyTypePropertyDictService.lambdaQuery()
                .eq(HierarchyTypePropertyDict::getDictKey, "sensors").one();

        if (sensorsDict == null) {
            log.warn("未找到sensors字典属性");
            return new ArrayList<>();
        }

        List<HierarchyTypeProperty> list = hierarchyTypePropertyService.lambdaQuery()
                .eq(HierarchyTypeProperty::getPropertyDictId, sensorsDict.getId())
                .in(HierarchyTypeProperty::getTypeId, typeIds).list();

        List<Long> typePropertyIds = list.stream().map(HierarchyTypeProperty::getId).collect(Collectors.toList());

        if (typePropertyIds.isEmpty()) {
            log.warn("未找到sensors类型属性");
            return new ArrayList<>();
        }

        // 获取hierarchyId范围内符合typeIds的设备层级
        Set<Long> hierarchyDescendants = getAllDescendantIds(hierarchyId);
        List<Hierarchy> deviceHierarchies = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, hierarchyDescendants)
                .in(Hierarchy::getTypeId, typeIds)
                .list();

        if (deviceHierarchies.isEmpty()) {
            log.warn("在hierarchyId={}范围内未找到符合类型的设备", hierarchyId);
            return new ArrayList<>();
        }

        List<Long> deviceHierarchyIds = deviceHierarchies.stream()
                .map(Hierarchy::getId)
                .collect(Collectors.toList());

        // 查找这些设备的传感器绑定属性
        List<HierarchyProperty> properties = hierarchyPropertyService.lambdaQuery()
                .ne(HierarchyProperty::getPropertyValue, "")
                .in(HierarchyProperty::getTypePropertyId, typePropertyIds)
                .in(HierarchyProperty::getHierarchyId, deviceHierarchyIds)
                .list();

        // 解析传感器ID列表
        List<Long> sensorIds = new ArrayList<>();
        for (HierarchyProperty property : properties) {
            String propertyValue = property.getPropertyValue();
            String[] split = propertyValue.split("\\,");
            for (String s : split) {
                try {
                    sensorIds.add(Long.parseLong(s.trim()));
                } catch (NumberFormatException e) {
                    log.warn("解析传感器ID失败: {}", s);
                }
            }
        }

        if (sensorIds.isEmpty()) {
            log.warn("未找到有效的传感器ID");
            return new ArrayList<>();
        }

        // 查询传感器详细信息
        List<Hierarchy> sensors = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, sensorIds)
                .eq(Hierarchy::getTypeId, sensorTypeId)
                .list();

        log.info("通过属性绑定找到 {} 个传感器", sensors.size());
        return sensors;
    }

    /**
     * 统计维度：按目标层级统计设备报警情况
     * 每个目标层级下统计device_point，device_point的report_st等于该设备及其递归子集关联的所有传感器中的最高报警等级
     */
    private Map<String, Object> alarmByDeviceLevel(List<Hierarchy> targetHierarchies, HierarchyType sensorHierarchyType,
            Long targetTypeId) {
        log.info("执行按目标层级设备点报警统计 - 目标层级数: {}", targetHierarchies.size());

        // 获取设备点类型（typeKey = "device_point"）
        HierarchyType devicePointHierarchyType = getHierarchyTypeByKey("device_point");
        if (devicePointHierarchyType == null) {
            log.warn("未找到device_point类型");
            return createEmptyResult(targetTypeId, 1);
        }

        // 获取所有传感器的报警状态（report_st值）
        Map<Long, Integer> sensorAlarmLevels = getSensorAlarmLevels(sensorHierarchyType.getId());

        // 统计每个目标层级下的设备点报警情况
        List<Map<String, Object>> statistics = new ArrayList<>();
        int totalDeviceCount = 0;
        int totalAlarmDeviceCount = 0;
        int totalGeneralCount = 0; // 全局一般报警设备点数量
        int totalSeriousCount = 0; // 全局严重报警设备点数量

        for (Hierarchy targetHierarchy : targetHierarchies) {
            // 查找该目标层级下的所有device_point类型的层级
            List<Hierarchy> devicesUnderTarget = findDevicesUnderTarget(targetHierarchy.getId(),
                    devicePointHierarchyType.getId());

            // 统计该层级下的报警设备点
            List<Map<String, Object>> alarmDevices = new ArrayList<>();
            int hierarchyAlarmDeviceCount = 0;
            int hierarchyGeneralCount = 0; // 该层级一般报警设备点数量
            int hierarchySeriousCount = 0; // 该层级严重报警设备点数量

            for (Hierarchy device : devicesUnderTarget) {
                // 计算该设备点及其递归子集中关联的所有传感器的最高报警级别
                int maxAlarmLevel = getDeviceMaxAlarmLevel(device.getId(), sensorAlarmLevels);

                if (maxAlarmLevel > 0) {
                    hierarchyAlarmDeviceCount++;
                    totalAlarmDeviceCount++;

                    // 根据报警级别分类统计
                    if (maxAlarmLevel == 1) {
                        hierarchyGeneralCount++;
                        totalGeneralCount++;
                    } else if (maxAlarmLevel == 2 || maxAlarmLevel == 3) {
                        hierarchySeriousCount++;
                        totalSeriousCount++;
                    }

                    Map<String, Object> alarmDevice = new HashMap<>();
                    alarmDevice.put("id", device.getId());
                    alarmDevice.put("name", device.getName());
                    alarmDevice.put("typeId", device.getTypeId());
                    alarmDevice.put("report_st", maxAlarmLevel);

                    alarmDevices.add(alarmDevice);
                }
            }

            // 创建该目标层级的统计记录
            Map<String, Object> hierarchyStat = new HashMap<>();
            hierarchyStat.put("targetHierarchyName", targetHierarchy.getName());
            hierarchyStat.put("totalDeviceCount", devicesUnderTarget.size());
            hierarchyStat.put("alarmDeviceCount", hierarchyAlarmDeviceCount);
            hierarchyStat.put("generalCount", hierarchyGeneralCount);
            hierarchyStat.put("seriousCount", hierarchySeriousCount);
            hierarchyStat.put("alarmDevices", alarmDevices);

            statistics.add(hierarchyStat);
            totalDeviceCount += devicesUnderTarget.size();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalDeviceCount", totalDeviceCount);
        result.put("totalAlarmDeviceCount", totalAlarmDeviceCount);
        result.put("totalGeneralCount", totalGeneralCount);
        result.put("totalSeriousCount", totalSeriousCount);
        result.put("statistics", statistics);

        log.info("按目标层级设备点统计完成：共{}个层级，{}个设备点，{}个报警设备点（一般{}个，严重{}个）",
                targetHierarchies.size(), totalDeviceCount, totalAlarmDeviceCount, totalGeneralCount,
                totalSeriousCount);

        return result;
    }

    /**
     * 被监测设备统计（原有逻辑）
     */
    private Map<String, Object> alarmByMonitoredDevice(List<Hierarchy> targetHierarchies,
            HierarchyType sensorHierarchyType, Long targetTypeId) {
        log.info("执行被监测设备报警统计 - 目标层级数: {}", targetHierarchies.size());

        // 获取所有报警的传感器ID列表
        List<Long> alarmSensorIds = getReportStHierarchyIds(sensorHierarchyType.getId());

        // 获取所有离线的传感器ID列表
        List<Long> offlineSensorIds = getOfflineFlagHierarchyIds(sensorHierarchyType.getId());

        // 统计每个目标层级下的报警、离线和总传感器数量
        List<Map<String, Object>> statistics = new ArrayList<>();
        int totalAlarmSensorCount = 0;
        int totalOfflineSensorCount = 0;
        int totalSensorCount = 0;

        for (Hierarchy targetHierarchy : targetHierarchies) {
            // 查找该目标层级下的所有传感器
            List<Hierarchy> allSensorsUnderTarget = findAllSensorsUnderTarget(
                    targetHierarchy.getId(), sensorHierarchyType.getId());

            // 查找该目标层级下的所有报警传感器
            List<Hierarchy> alarmSensorsUnderTarget = findAlarmSensorsUnderTarget(
                    targetHierarchy.getId(), sensorHierarchyType.getId(), alarmSensorIds);

            // 查找该目标层级下的所有离线传感器
            List<Hierarchy> offlineSensorsUnderTarget = findAlarmSensorsUnderTarget(
                    targetHierarchy.getId(), sensorHierarchyType.getId(), offlineSensorIds);

            Map<String, Object> stat = new HashMap<>();
            stat.put("targetHierarchyName", targetHierarchy.getName());
            stat.put("totalSensorCount", allSensorsUnderTarget.size());
            stat.put("alarmSensorCount", alarmSensorsUnderTarget.size());
            stat.put("alarmSensors", alarmSensorsUnderTarget.stream()
                    .map(h -> Map.of("id", h.getId(), "name", h.getName(), "typeId", h.getTypeId()))
                    .toList());
            stat.put("offlineSensorCount", offlineSensorsUnderTarget.size());
            stat.put("offlineSensors", offlineSensorsUnderTarget.stream()
                    .map(h -> Map.of("id", h.getId(), "name", h.getName(), "typeId", h.getTypeId()))
                    .toList());

            statistics.add(stat);
            totalSensorCount += allSensorsUnderTarget.size();
            totalAlarmSensorCount += alarmSensorsUnderTarget.size();
            totalOfflineSensorCount += offlineSensorsUnderTarget.size();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalSensorCount", totalSensorCount);
        result.put("totalAlarmSensorCount", totalAlarmSensorCount);
        result.put("totalOfflineSensorCount", totalOfflineSensorCount);
        result.put("statistics", statistics);

        log.info("被监测设备统计完成：targetTypeId={}, 共找到{}个目标层级，包含{}个传感器（{}个报警，{}个离线）",
                targetTypeId, targetHierarchies.size(), totalSensorCount, totalAlarmSensorCount,
                totalOfflineSensorCount);

        return result;
    }

    /**
     * 按属性维度统计报警：当targetTypeId不在hierarchyId层级树下时使用
     * 通过传感器的属性值进行分组统计
     *
     * @param hierarchyId 层级ID
     * @param targetTypeId 目标类型ID（作为统计维度）
     * @param statisticalType 统计类型（1=设备点维度，2=传感器维度）
     * @param sensorHierarchyType 传感器类型
     * @return 统计结果
     */
    private Map<String, Object> alarmByPropertyDimension(Long hierarchyId, Long targetTypeId, 
            Integer statisticalType, HierarchyType sensorHierarchyType) {
        log.info("执行属性维度报警统计 - hierarchyId: {}, targetTypeId: {}, statisticalType: {}", 
                hierarchyId, targetTypeId, statisticalType);

        if (statisticalType == 1) {
            // 按设备点维度统计
            return alarmByPropertyDimensionDeviceLevel(hierarchyId, targetTypeId, sensorHierarchyType);
        } else {
            // 按传感器维度统计
            return alarmByPropertyDimensionSensorLevel(hierarchyId, targetTypeId, sensorHierarchyType);
        }
    }

    /**
     * 按属性维度 + 设备点维度统计报警
     */
    private Map<String, Object> alarmByPropertyDimensionDeviceLevel(Long hierarchyId, Long targetTypeId, 
            HierarchyType sensorHierarchyType) {
        log.info("执行属性维度设备点统计 - hierarchyId: {}, targetTypeId: {}", hierarchyId, targetTypeId);

        // 1. 查找对应的属性字典
        HierarchyTypePropertyDict propertyDict = findPropertyDictForTargetType(targetTypeId);
        if (propertyDict == null) {
            log.warn("targetTypeId={} 没有对应的属性字典，无法进行属性维度统计", targetTypeId);
            return createEmptyResult(targetTypeId, 1);
        }

        log.info("找到属性字典：{} (key={})", propertyDict.getDictName(), propertyDict.getDictKey());

        // 2. 获取 device 和 device_point 类型ID
        List<Long> deviceTypeIds = getTypeIdsByKeys(List.of("device", "device_point"));
        if (deviceTypeIds.isEmpty()) {
            log.warn("未找到设备类型");
            return createEmptyResult(targetTypeId, 1);
        }

        // 3. 查找 hierarchyId 下的所有设备点
        List<Hierarchy> allDevicePoints = findAllHierarchiesUnderTarget(hierarchyId, deviceTypeIds);
        if (allDevicePoints.isEmpty()) {
            log.warn("hierarchyId={} 下没有找到设备点", hierarchyId);
            return createEmptyResult(targetTypeId, 1);
        }

        log.info("找到 {} 个设备点", allDevicePoints.size());

        // 4. 获取所有报警的传感器ID
        Map<Long, Integer> sensorAlarmLevels = getSensorAlarmLevels(sensorHierarchyType.getId());

        // 5. 查询所有设备点绑定的传感器
        Map<Long, List<Long>> devicePointToSensorsMap = getDevicePointSensorBindings(
                allDevicePoints.stream().map(Hierarchy::getId).collect(Collectors.toList()));

        log.info("查询到 {} 个设备点的传感器绑定关系", devicePointToSensorsMap.size());

        // 6. 查询传感器的属性值，建立传感器到目标维度的映射
        List<Long> allSensorIds = devicePointToSensorsMap.values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Long> sensorToTargetMap = getSensorPropertyMapping(
                allSensorIds, propertyDict.getId(), sensorHierarchyType.getId());

        log.info("建立了 {} 个传感器的属性映射", sensorToTargetMap.size());

        // 7. 查询所有目标类型的层级（作为统计维度标签）
        List<Hierarchy> targetDimensions = hierarchyService.list(
                Wrappers.<Hierarchy>lambdaQuery().eq(Hierarchy::getTypeId, targetTypeId));

        log.info("找到 {} 个统计维度", targetDimensions.size());

        // 8. 按维度分组统计设备点
        List<Map<String, Object>> statistics = new ArrayList<>();
        int totalDeviceCount = 0;
        int totalAlarmDeviceCount = 0;
        int totalGeneralCount = 0;
        int totalSeriousCount = 0;

        for (Hierarchy targetDimension : targetDimensions) {
            // 找到属于该维度的设备点（通过其绑定的传感器判断）
            List<Hierarchy> devicePointsInDimension = allDevicePoints.stream()
                    .filter(devicePoint -> {
                        List<Long> sensorIds = devicePointToSensorsMap.get(devicePoint.getId());
                        if (sensorIds == null || sensorIds.isEmpty()) {
                            return false;
                        }
                        // 只要有一个传感器属于该维度，就算该设备点属于该维度
                        return sensorIds.stream()
                                .anyMatch(sensorId -> targetDimension.getId().equals(sensorToTargetMap.get(sensorId)));
                    })
                    .collect(Collectors.toList());

            // 统计报警设备点
            int generalCount = 0;
            int seriousCount = 0;
            List<Map<String, Object>> alarmDevices = new ArrayList<>();

            for (Hierarchy devicePoint : devicePointsInDimension) {
                List<Long> sensorIds = devicePointToSensorsMap.get(devicePoint.getId());
                if (sensorIds == null || sensorIds.isEmpty()) {
                    continue;
                }

                // 筛选出属于当前维度的传感器
                List<Long> dimensionSensorIds = sensorIds.stream()
                        .filter(sensorId -> targetDimension.getId().equals(sensorToTargetMap.get(sensorId)))
                        .collect(Collectors.toList());

                if (dimensionSensorIds.isEmpty()) {
                    continue;
                }

                // 计算该设备点的最高报警级别
                int maxAlarmLevel = dimensionSensorIds.stream()
                        .map(sensorId -> sensorAlarmLevels.getOrDefault(sensorId, 0))
                        .max(Integer::compareTo)
                        .orElse(0);

                if (maxAlarmLevel > 0) {
                    if (maxAlarmLevel >= 3) {
                        seriousCount++;
                    } else {
                        generalCount++;
                    }

                    Map<String, Object> alarmDevice = new HashMap<>();
                    alarmDevice.put("id", devicePoint.getId());
                    alarmDevice.put("name", devicePoint.getName());
                    alarmDevice.put("typeId", devicePoint.getTypeId());
                    alarmDevice.put("alarmLevel", maxAlarmLevel);
                    alarmDevices.add(alarmDevice);
                }
            }

            Map<String, Object> stat = new HashMap<>();
            stat.put("targetHierarchyName", targetDimension.getName());
            stat.put("targetHierarchyId", targetDimension.getId());
            stat.put("totalDeviceCount", devicePointsInDimension.size());
            stat.put("alarmDeviceCount", generalCount + seriousCount);
            stat.put("generalCount", generalCount);
            stat.put("seriousCount", seriousCount);
            stat.put("alarmDevices", alarmDevices);

            statistics.add(stat);
            totalDeviceCount += devicePointsInDimension.size();
            totalAlarmDeviceCount += (generalCount + seriousCount);
            totalGeneralCount += generalCount;
            totalSeriousCount += seriousCount;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalDeviceCount", totalDeviceCount);
        result.put("totalAlarmDeviceCount", totalAlarmDeviceCount);
        result.put("totalGeneralCount", totalGeneralCount);
        result.put("totalSeriousCount", totalSeriousCount);
        result.put("statistics", statistics);

        log.info("属性维度设备点统计完成：共 {} 个维度，包含 {} 个设备点（{} 个报警）",
                targetDimensions.size(), totalDeviceCount, totalAlarmDeviceCount);

        return result;
    }

    /**
     * 按属性维度 + 传感器维度统计报警
     */
    private Map<String, Object> alarmByPropertyDimensionSensorLevel(Long hierarchyId, Long targetTypeId, 
            HierarchyType sensorHierarchyType) {
        log.info("执行属性维度传感器统计 - hierarchyId: {}, targetTypeId: {}", hierarchyId, targetTypeId);

        // 1. 查找对应的属性字典
        HierarchyTypePropertyDict propertyDict = findPropertyDictForTargetType(targetTypeId);
        if (propertyDict == null) {
            log.warn("targetTypeId={} 没有对应的属性字典，无法进行属性维度统计", targetTypeId);
            return createEmptyResult(targetTypeId, 2);
        }

        log.info("找到属性字典：{} (key={})", propertyDict.getDictName(), propertyDict.getDictKey());

        // 2. 查找 hierarchyId 下的所有传感器
        // 特殊处理：如果是 device_group 类型，会通过绑定属性收集传感器
        List<Hierarchy> allSensors = getSensorsForHierarchy(hierarchyId, sensorHierarchyType.getId());
        if (allSensors.isEmpty()) {
            log.warn("hierarchyId={} 下没有找到传感器", hierarchyId);
            return createEmptyResult(targetTypeId, 2);
        }

        log.info("找到 {} 个传感器", allSensors.size());

        // 3. 获取所有报警和离线的传感器ID
        Map<Long, Integer> sensorAlarmLevels = getSensorAlarmLevels(sensorHierarchyType.getId());
        List<Long> offlineSensorIds = getOfflineFlagHierarchyIds(sensorHierarchyType.getId());

        // 4. 查询传感器的属性值，建立传感器到目标维度的映射
        Map<Long, Long> sensorToTargetMap = getSensorPropertyMapping(
                allSensors.stream().map(Hierarchy::getId).collect(Collectors.toList()), 
                propertyDict.getId(), 
                sensorHierarchyType.getId());

        log.info("建立了 {} 个传感器的属性映射", sensorToTargetMap.size());

        // 5. 查询所有目标类型的层级（作为统计维度标签）
        List<Hierarchy> targetDimensions = hierarchyService.list(
                Wrappers.<Hierarchy>lambdaQuery().eq(Hierarchy::getTypeId, targetTypeId));

        log.info("找到 {} 个统计维度", targetDimensions.size());

        // 6. 按维度分组统计
        List<Map<String, Object>> statistics = new ArrayList<>();
        int totalSensorCount = 0;
        int totalAlarmSensorCount = 0;
        int totalOfflineSensorCount = 0;

        for (Hierarchy targetDimension : targetDimensions) {
            // 找到属于该维度的传感器
            List<Hierarchy> sensorsInDimension = allSensors.stream()
                    .filter(sensor -> targetDimension.getId().equals(sensorToTargetMap.get(sensor.getId())))
                    .collect(Collectors.toList());

            // 统计报警传感器
            List<Hierarchy> alarmSensors = sensorsInDimension.stream()
                    .filter(sensor -> {
                        Integer alarmLevel = sensorAlarmLevels.get(sensor.getId());
                        return alarmLevel != null && alarmLevel > 0;
                    })
                    .collect(Collectors.toList());

            // 统计离线传感器
            List<Hierarchy> offlineSensors = sensorsInDimension.stream()
                    .filter(sensor -> offlineSensorIds.contains(sensor.getId()))
                    .collect(Collectors.toList());

            Map<String, Object> stat = new HashMap<>();
            stat.put("targetHierarchyName", targetDimension.getName());
            stat.put("targetHierarchyId", targetDimension.getId());
            stat.put("totalSensorCount", sensorsInDimension.size());
            stat.put("alarmSensorCount", alarmSensors.size());
            stat.put("alarmSensors", alarmSensors.stream()
                    .map(h -> Map.of("id", h.getId(), "name", h.getName(), "typeId", h.getTypeId()))
                    .toList());
            stat.put("offlineSensorCount", offlineSensors.size());
            stat.put("offlineSensors", offlineSensors.stream()
                    .map(h -> Map.of("id", h.getId(), "name", h.getName(), "typeId", h.getTypeId()))
                    .toList());

            statistics.add(stat);
            totalSensorCount += sensorsInDimension.size();
            totalAlarmSensorCount += alarmSensors.size();
            totalOfflineSensorCount += offlineSensors.size();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalSensorCount", totalSensorCount);
        result.put("totalAlarmSensorCount", totalAlarmSensorCount);
        result.put("totalOfflineSensorCount", totalOfflineSensorCount);
        result.put("statistics", statistics);

        log.info("属性维度传感器统计完成：共 {} 个维度，包含 {} 个传感器（{} 个报警，{} 个离线）",
                targetDimensions.size(), totalSensorCount, totalAlarmSensorCount, totalOfflineSensorCount);

        return result;
    }

    /**
     * 查找目标类型对应的属性字典
     * 比如 targetTypeId=17（传感器类型） 对应 sensor_type 属性字典
     */
    private HierarchyTypePropertyDict findPropertyDictForTargetType(Long targetTypeId) {
        // 查找 dict_values 包含 targetTypeId 的属性字典
        List<HierarchyTypePropertyDict> dicts = hierarchyTypePropertyDictService.list();
        for (HierarchyTypePropertyDict dict : dicts) {
            if (dict.getDictValues() != null && dict.getDictValues().equals(String.valueOf(targetTypeId))) {
                return dict;
            }
        }
        return null;
    }

    /**
     * 获取传感器的属性值映射（传感器ID -> 目标维度ID）
     */
    private Map<Long, Long> getSensorPropertyMapping(List<Long> sensorIds, Long propertyDictId, Long sensorTypeId) {
        Map<Long, Long> mapping = new HashMap<>();

        if (sensorIds.isEmpty()) {
            return mapping;
        }

        // 查找该属性字典对应的类型属性
        HierarchyTypeProperty typeProperty = hierarchyTypePropertyService.getOne(
                Wrappers.<HierarchyTypeProperty>lambdaQuery()
                        .eq(HierarchyTypeProperty::getPropertyDictId, propertyDictId)
                        .eq(HierarchyTypeProperty::getTypeId, sensorTypeId));

        if (typeProperty == null) {
            log.warn("传感器类型未配置该属性，propertyDictId={}", propertyDictId);
            return mapping;
        }

        // 查询所有传感器的属性值
        List<HierarchyProperty> properties = hierarchyPropertyService.list(
                Wrappers.<HierarchyProperty>lambdaQuery()
                        .eq(HierarchyProperty::getTypePropertyId, typeProperty.getId())
                        .in(HierarchyProperty::getHierarchyId, sensorIds));

        for (HierarchyProperty property : properties) {
            if (property.getPropertyValue() != null && !property.getPropertyValue().trim().isEmpty()) {
                try {
                    Long targetId = Long.parseLong(property.getPropertyValue());
                    mapping.put(property.getHierarchyId(), targetId);
                } catch (NumberFormatException e) {
                    log.warn("属性值格式错误：hierarchyId={}, value={}", 
                            property.getHierarchyId(), property.getPropertyValue());
                }
            }
        }

        return mapping;
    }

    /**
     * 批量查找传感器所属的目标类型层级，优化性能避免N+1查询和递归查询
     *
     * @param sensorHierarchyIds 传感器层级ID列表
     * @param targetTypeId 目标类型ID
     * @return 传感器层级ID到目标层级ID的映射
     */
    private Map<Long, Long> findBelongingTargetHierarchiesBatch(List<Long> sensorHierarchyIds, Long targetTypeId) {
        Map<Long, Long> resultMap = new HashMap<>();

        if (sensorHierarchyIds.isEmpty()) {
            return resultMap;
        }

        // 批量查询所有传感器层级的基本信息
        List<Hierarchy> allHierarchies = hierarchyService.lambdaQuery()
            .in(Hierarchy::getId, sensorHierarchyIds)
            .list();

        Map<Long, Hierarchy> hierarchyMap = allHierarchies.stream()
            .collect(Collectors.toMap(Hierarchy::getId, h -> h));

        // 直接检查是否已经是目标类型
        for (Long sensorHierarchyId : sensorHierarchyIds) {
            Hierarchy hierarchy = hierarchyMap.get(sensorHierarchyId);
            if (hierarchy != null && targetTypeId.equals(hierarchy.getTypeId())) {
                resultMap.put(sensorHierarchyId, sensorHierarchyId);
            }
        }

        // 批量查询所有相关层级的属性
        List<HierarchyProperty> allProperties = hierarchyPropertyService.list(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .in(HierarchyProperty::getHierarchyId, sensorHierarchyIds)
        );

        if (allProperties.isEmpty()) {
            return resultMap;
        }

        // 批量查询类型属性
        Set<Long> typePropertyIds = allProperties.stream()
            .map(HierarchyProperty::getTypePropertyId)
            .collect(Collectors.toSet());

        List<HierarchyTypeProperty> typeProperties = hierarchyTypePropertyService.lambdaQuery()
            .in(HierarchyTypeProperty::getId, typePropertyIds)
            .list();

        Map<Long, HierarchyTypeProperty> typePropertyMap = typeProperties.stream()
            .collect(Collectors.toMap(HierarchyTypeProperty::getId, tp -> tp));

        // 批量查询属性字典
        Set<Long> dictIds = typeProperties.stream()
            .map(HierarchyTypeProperty::getPropertyDictId)
            .collect(Collectors.toSet());

        List<HierarchyTypePropertyDict> dicts = hierarchyTypePropertyDictService.lambdaQuery()
            .in(HierarchyTypePropertyDict::getId, dictIds)
            .list();

        Map<Long, HierarchyTypePropertyDict> dictMap = dicts.stream()
            .collect(Collectors.toMap(HierarchyTypePropertyDict::getId, d -> d));

        // 收集所有关联的层级ID
        Set<Long> relatedHierarchyIds = new HashSet<>();
        Map<Long, List<Long>> hierarchyToRelatedMap = new HashMap<>();

        for (HierarchyProperty property : allProperties) {
            HierarchyTypeProperty typeProperty = typePropertyMap.get(property.getTypePropertyId());
            if (typeProperty != null) {
                HierarchyTypePropertyDict dict = dictMap.get(typeProperty.getPropertyDictId());
                if (dict != null && dict.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
                    try {
                        Long relatedHierarchyId = Long.valueOf(property.getPropertyValue());
                        relatedHierarchyIds.add(relatedHierarchyId);

                        hierarchyToRelatedMap
                            .computeIfAbsent(property.getHierarchyId(), k -> new ArrayList<>())
                            .add(relatedHierarchyId);
                    } catch (NumberFormatException e) {
                        // 忽略无效的层级ID
                    }
                }
            }
        }

        // 批量查询所有关联层级的信息
        if (!relatedHierarchyIds.isEmpty()) {
            List<Hierarchy> relatedHierarchies = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, relatedHierarchyIds)
                .list();

            Map<Long, Hierarchy> relatedHierarchyMap = relatedHierarchies.stream()
                .collect(Collectors.toMap(Hierarchy::getId, h -> h));

            // 使用广度优先搜索找到目标类型层级
            for (Long sensorHierarchyId : sensorHierarchyIds) {
                if (resultMap.containsKey(sensorHierarchyId)) {
                    continue; // 已经找到目标类型
                }

                Long targetHierarchyId = findTargetHierarchyBFS(
                    sensorHierarchyId,
                    targetTypeId,
                    hierarchyToRelatedMap,
                    relatedHierarchyMap
                );

                if (targetHierarchyId != null) {
                    resultMap.put(sensorHierarchyId, targetHierarchyId);
                }
            }
        }

        return resultMap;
    }

    /**
     * 使用广度优先搜索查找目标类型层级
     */
    private Long findTargetHierarchyBFS(Long startHierarchyId, Long targetTypeId,
                                       Map<Long, List<Long>> hierarchyToRelatedMap,
                                       Map<Long, Hierarchy> hierarchyMap) {
        Queue<Long> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();

        queue.offer(startHierarchyId);
        visited.add(startHierarchyId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();

            Hierarchy current = hierarchyMap.get(currentId);
            if (current != null && targetTypeId.equals(current.getTypeId())) {
                return currentId;
            }

            List<Long> relatedIds = hierarchyToRelatedMap.get(currentId);
            if (relatedIds != null) {
                for (Long relatedId : relatedIds) {
                    if (!visited.contains(relatedId)) {
                        queue.offer(relatedId);
                        visited.add(relatedId);
                    }
                }
            }
        }

        return null;
    }


    /**
     * 从hierarchyId向上查找type=7的层级
     *
     * @param hierarchyId 起始层级ID
     * @return type=7的层级ID，如果没找到返回null
     */
    private Long findType7Upward(Long hierarchyId) {
        Hierarchy current = hierarchyService.getById(hierarchyId);
        int maxDepth = 20; // 防止无限循环
        int depth = 0;

        while (current != null && depth < maxDepth) {
            // 检查当前层级的类型是否为7
            if (Long.valueOf(7).equals(current.getTypeId())) {
                log.info("从hierarchyId={}向上找到type=7的层级: {}", hierarchyId, current.getId());
                return current.getId();
            }

            // 向上查找父级
            if (current.getParentId() != null) {
                current = hierarchyService.getById(current.getParentId());
                depth++;
            } else {
                break;
            }
        }

        log.warn("从hierarchyId={}向上未找到type=7的层级", hierarchyId);
        return null;
    }

    /**
     * 从type=7层级向下查找所有targetTypeId类型的层级
     *
     * @param type7HierarchyId type=7的层级ID
     * @param targetTypeId 目标类型ID
     * @return 目标类型的层级列表
     */
    private List<Hierarchy> findTargetHierarchiesUnderType7(Long type7HierarchyId, Long targetTypeId) {
        // 获取type7层级下的所有子孙层级ID
        Set<Long> allDescendants = getAllDescendantIds(type7HierarchyId);

        if (allDescendants.isEmpty()) {
            return new ArrayList<>();
        }

        // 在子孙层级中查找targetTypeId类型的层级
        List<Hierarchy> targetHierarchies = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, allDescendants)
                .eq(Hierarchy::getTypeId, targetTypeId)
                .list();

        log.info("在type=7层级{}下找到{}个targetTypeId={}的层级",
                type7HierarchyId, targetHierarchies.size(), targetTypeId);

        return targetHierarchies;
    }

    /**
     * 查找传感器所属的目标类型层级（向上查找三级系统）
     *
     * @param sensorIds 传感器ID列表
     * @param targetTypeId 目标类型ID（如三级系统类型）
     * @return 传感器ID到目标层级ID的映射
     */
    private Map<Long, Long> findSensorBelongingToTargetType(List<Long> sensorIds, Long targetTypeId) {
        Map<Long, Long> sensorToTargetMap = new HashMap<>();

        if (sensorIds.isEmpty()) {
            return sensorToTargetMap;
        }

        log.info("查找 {} 个传感器所属的targetTypeId={}的上级层级", sensorIds.size(), targetTypeId);

        // 批量查询所有传感器的层级信息
        List<Hierarchy> sensorHierarchies = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, sensorIds)
                .list();

        // 批量查询所有层级，建立层级关系映射
        List<Hierarchy> allHierarchies = hierarchyService.list();
        Map<Long, Hierarchy> hierarchyMap = allHierarchies.stream()
                .collect(Collectors.toMap(Hierarchy::getId, h -> h));

        // 对每个传感器向上查找目标类型的层级
        for (Hierarchy sensor : sensorHierarchies) {
            Long targetHierarchyId = findTargetTypeUpward(sensor, targetTypeId, hierarchyMap);
            if (targetHierarchyId != null) {
                sensorToTargetMap.put(sensor.getId(), targetHierarchyId);
            }
        }

        log.info("找到 {} 个传感器与目标层级的关联关系", sensorToTargetMap.size());
        return sensorToTargetMap;
    }

    /**
     * 从传感器向上查找指定类型的层级
     *
     * @param sensor 传感器层级
     * @param targetTypeId 目标类型ID
     * @param hierarchyMap 层级映射
     * @return 找到的目标层级ID，如果没找到返回null
     */
    private Long findTargetTypeUpward(Hierarchy sensor, Long targetTypeId, Map<Long, Hierarchy> hierarchyMap) {
        // 如果没有提供层级映射，重新构建
        if (hierarchyMap == null) {
            List<Hierarchy> allHierarchies = hierarchyService.list();
            hierarchyMap = allHierarchies.stream()
                .collect(Collectors.toMap(Hierarchy::getId, h -> h));
        }

        Hierarchy current = sensor;
        Set<Long> visited = new HashSet<>(); // 防止循环引用

        while (current != null && !visited.contains(current.getId())) {
            visited.add(current.getId());

            // 检查当前层级是否是目标类型
            if (targetTypeId.equals(current.getTypeId())) {
                return current.getId();
            }

            // 向上查找父层级
            if (current.getParentId() != null) {
                current = hierarchyMap.get(current.getParentId());
            } else {
                break;
            }
        }

        return null; // 没有找到目标类型的上级层级
    }

    /**
     * 从hierarchyId开始，只在其子集范围内查找指定类型的层级（限定在子集范围内）
     *
     * @param hierarchyId 起始层级ID
     * @param targetTypeId 目标类型ID
     * @return 指定类型的层级ID列表
     */
    private List<Long> findHierarchiesByType(Long hierarchyId, Long targetTypeId) {
        List<Long> resultIds = new ArrayList<>();

        // 首先获取hierarchyId的所有子集层级（只通过parentId关系）
        Set<Long> allDescendantIds = getAllDescendantIds(hierarchyId);

        if (allDescendantIds.isEmpty()) {
            return resultIds;
        }

        // 在子集范围内查找目标类型的层级
        List<Hierarchy> targetHierarchies = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, allDescendantIds)
                .eq(Hierarchy::getTypeId, targetTypeId)
                .list();

        return targetHierarchies.stream()
                .map(Hierarchy::getId)
                .collect(Collectors.toList());
    }

    /**
     * 查找与hierarchyId关联的指定类型的层级（包括通过属性关系）
     * 用于解决某些层级不在parentId树中，而是通过隐藏属性关联的情况
     *
     * @param hierarchyId 起始层级ID
     * @param targetTypeId 目标类型ID
     * @return 目标类型的层级ID列表
     */
    private List<Long> findHierarchiesByTypeIncludingPropertyRelation(Long hierarchyId, Long targetTypeId) {
        Set<Long> resultIds = new HashSet<>();

        // 1. 首先通过parentId关系查找
        List<Long> descendantIds = findHierarchiesByType(hierarchyId, targetTypeId);
        resultIds.addAll(descendantIds);

        // 2. 通过属性关系查找：查询所有包含hierarchyId作为属性值的层级
        // 这些层级虽然不在parentId树中，但通过隐藏属性关联到hierarchyId
        List<HierarchyProperty> relatedProperties = hierarchyPropertyService.list(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .eq(HierarchyProperty::getPropertyValue, hierarchyId.toString())
        );

        if (!relatedProperties.isEmpty()) {
            // 获取这些属性关联的层级ID
            Set<Long> relatedHierarchyIds = relatedProperties.stream()
                .map(HierarchyProperty::getHierarchyId)
                .collect(Collectors.toSet());

            // 筛选出目标类型的层级
            List<Hierarchy> relatedHierarchies = hierarchyService.lambdaQuery()
                .in(Hierarchy::getId, relatedHierarchyIds)
                .eq(Hierarchy::getTypeId, targetTypeId)
                .list();

            resultIds.addAll(relatedHierarchies.stream()
                .map(Hierarchy::getId)
                .collect(Collectors.toList()));

            // 递归查找：从这些关联层级继续向下查找
            for (Hierarchy relatedHierarchy : relatedHierarchies) {
                // 通过parentId关系继续向下查找
                Set<Long> childDescendantIds = getAllDescendantIds(relatedHierarchy.getId());
                if (!childDescendantIds.isEmpty()) {
                    List<Hierarchy> childTargetHierarchies = hierarchyService.lambdaQuery()
                        .in(Hierarchy::getId, childDescendantIds)
                        .eq(Hierarchy::getTypeId, targetTypeId)
                        .list();

                    resultIds.addAll(childTargetHierarchies.stream()
                        .map(Hierarchy::getId)
                        .collect(Collectors.toList()));
                }
            }
        }

        return new ArrayList<>(resultIds);
    }

    /**
     * 批量查询层级的 sort 属性值
     *
     * @param hierarchyIds 层级ID列表
     * @return 层级ID到sort值的映射
     */
    private Map<Long, String> getSortValuesForHierarchies(List<Long> hierarchyIds) {
        Map<Long, String> sortMap = new HashMap<>();

        if (hierarchyIds == null || hierarchyIds.isEmpty()) {
            return sortMap;
        }

        // 查询 dict_key='sort' 的字典
        HierarchyTypePropertyDict sortDict = hierarchyTypePropertyDictService.getOne(
            Wrappers.<HierarchyTypePropertyDict>lambdaQuery()
                .eq(HierarchyTypePropertyDict::getDictKey, "sort")
        );

        if (sortDict == null) {
            return sortMap;
        }

        // 查询该字典对应的类型属性
        List<HierarchyTypeProperty> sortTypeProperties = hierarchyTypePropertyService.list(
            Wrappers.<HierarchyTypeProperty>lambdaQuery()
                .eq(HierarchyTypeProperty::getPropertyDictId, sortDict.getId())
        );

        if (sortTypeProperties.isEmpty()) {
            return sortMap;
        }

        Set<Long> sortTypePropertyIds = sortTypeProperties.stream()
            .map(HierarchyTypeProperty::getId)
            .collect(Collectors.toSet());

        // 批量查询层级的 sort 属性值
        List<HierarchyProperty> sortProperties = hierarchyPropertyService.list(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .in(HierarchyProperty::getHierarchyId, hierarchyIds)
                .in(HierarchyProperty::getTypePropertyId, sortTypePropertyIds)
        );

        // 构建映射
        for (HierarchyProperty property : sortProperties) {
            sortMap.put(property.getHierarchyId(), property.getPropertyValue());
        }

        return sortMap;
    }

    /**
     * 批量获取层级的 icon 属性值
     *
     * @param hierarchyIds 层级ID列表
     * @return hierarchyId -> icon 的映射
     */
    private Map<Long, String> getIconValuesForHierarchies(List<Long> hierarchyIds) {
        Map<Long, String> iconMap = new HashMap<>();

        if (hierarchyIds == null || hierarchyIds.isEmpty()) {
            return iconMap;
        }

        // 查询 dict_key='icon' 的字典
        HierarchyTypePropertyDict iconDict = hierarchyTypePropertyDictService.getOne(
            Wrappers.<HierarchyTypePropertyDict>lambdaQuery()
                .eq(HierarchyTypePropertyDict::getDictKey, "icon")
        );

        if (iconDict == null) {
            return iconMap;
        }

        // 查询该字典对应的类型属性
        List<HierarchyTypeProperty> iconTypeProperties = hierarchyTypePropertyService.list(
            Wrappers.<HierarchyTypeProperty>lambdaQuery()
                .eq(HierarchyTypeProperty::getPropertyDictId, iconDict.getId())
        );

        if (iconTypeProperties.isEmpty()) {
            return iconMap;
        }

        Set<Long> iconTypePropertyIds = iconTypeProperties.stream()
            .map(HierarchyTypeProperty::getId)
            .collect(Collectors.toSet());

        // 批量查询层级的 icon 属性值
        List<HierarchyProperty> iconProperties = hierarchyPropertyService.list(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .in(HierarchyProperty::getHierarchyId, hierarchyIds)
                .in(HierarchyProperty::getTypePropertyId, iconTypePropertyIds)
        );

        // 构建映射
        for (HierarchyProperty property : iconProperties) {
            iconMap.put(property.getHierarchyId(), property.getPropertyValue());
        }

        return iconMap;
    }

    /**
     * 获取hierarchyId的所有子孙层级ID（只通过parentId关系，确保在子集范围内）
     *
     * @param hierarchyId 起始层级ID
     * @return 所有子孙层级ID的集合（包括起始层级）
     */
    private Set<Long> getAllDescendantIds(Long hierarchyId) {
        Set<Long> allIds = new HashSet<>();
        Set<Long> toProcess = new HashSet<>();

        toProcess.add(hierarchyId);
        allIds.add(hierarchyId);

        int maxIterations = 20; // 防止无限循环
        int iteration = 0;

        while (!toProcess.isEmpty() && iteration < maxIterations) {
            iteration++;
            Set<Long> currentBatch = new HashSet<>(toProcess);
            toProcess.clear();

            if (!currentBatch.isEmpty()) {
                // 批量查询当前批次的直接子级
                List<Hierarchy> children = hierarchyService.lambdaQuery()
                        .in(Hierarchy::getParentId, currentBatch)
                        .list();

                for (Hierarchy child : children) {
                    if (!allIds.contains(child.getId())) {
                        allIds.add(child.getId());
                        toProcess.add(child.getId());
                    }
                }
            }
        }

        return allIds;
    }

    /**
     * 递归查找匹配指定类型的子孙层级，找到匹配类型就停止递归
     *
     * @param hierarchyId  当前层级ID
     * @param targetTypeId 目标类型ID
     * @param matchedIds   匹配的层级ID集合
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

    /**
     * 获取报警状态层级ID列表（sys:st属性值大于0）
     *
     * @param typeId 类型ID
     * @return 报警状态的层级ID列表
     */
    private List<Long> getReportStHierarchyIds(Long typeId) {
        return getHierarchyIdsByDictAndType("sys:st", typeId);
    }

    /**
     * 获取离线标志层级ID列表（sys:cs属性值为"1"）
     *
     * @param typeId 类型ID
     * @return 离线标志的层级ID列表
     */
    private List<Long> getOfflineFlagHierarchyIds(Long typeId) {
        return getHierarchyIdsByDictAndValue("sys:cs", typeId, "1");
    }

    /**
     * 获取指定字典key、类型和属性值的层级ID列表
     *
     * @param dictKey 字典key
     * @param typeId 类型ID
     * @param expectedValue 期望的属性值
     * @return 层级ID列表
     */
    private List<Long> getHierarchyIdsByDictAndValue(String dictKey, Long typeId, String expectedValue) {
        HierarchyTypePropertyDict dict = getPropertyDictByKey(dictKey);
        if (dict == null) {
            log.warn("未找到{}字典属性", dictKey);
            return new ArrayList<>();
        }

        HierarchyTypeProperty typeProperty = getTypePropertyByDictAndType(dict.getId(), typeId);
        if (typeProperty == null) {
            log.warn("类型{}未配置{}属性", typeId, dictKey);
            return new ArrayList<>();
        }

        List<HierarchyProperty> properties = hierarchyPropertyService.lambdaQuery()
                .eq(HierarchyProperty::getTypePropertyId, typeProperty.getId())
                .list();

        return properties.stream()
                .filter(property -> expectedValue.equals(property.getPropertyValue()))
                .map(HierarchyProperty::getHierarchyId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 检查属性值是否大于0
     *
     * @param property 属性对象
     * @return 是否大于0
     */
    private boolean isValidPropertyValue(HierarchyProperty property) {
        String value = property.getPropertyValue();
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            // 尝试将值转换为数字，大于0的都是true
            double numericValue = Double.parseDouble(value.trim());
            return numericValue > 0;
        } catch (NumberFormatException e) {
            // 如果不是数字，则返回false
            return false;
        }
    }

    /**
     * 从目标层级出发，查找其下所有的报警传感器
     *
     * @param targetHierarchyId 目标层级ID
     * @param sensorTypeId      传感器类型ID
     * @param alarmSensorIds    报警传感器ID列表
     * @return 该目标层级下的所有报警传感器列表
     */
    private List<Hierarchy> findAlarmSensorsUnderTarget(Long targetHierarchyId,
            Long sensorTypeId,
            List<Long> alarmSensorIds) {
        List<Hierarchy> alarmSensors = new ArrayList<>();
        findAlarmSensorsRecursive(targetHierarchyId, sensorTypeId, alarmSensorIds, alarmSensors);
        return alarmSensors;
    }

    /**
     * 递归查找目标层级下的报警传感器
     *
     * @param hierarchyId    当前层级ID
     * @param sensorTypeId   传感器类型ID
     * @param alarmSensorIds 报警传感器ID列表
     * @param alarmSensors   找到的报警传感器集合
     */
    private void findAlarmSensorsRecursive(Long hierarchyId,
            Long sensorTypeId,
            List<Long> alarmSensorIds,
            List<Hierarchy> alarmSensors) {
        // 获取直接子级
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Hierarchy::getParentId, hierarchyId);
        List<Hierarchy> children = hierarchyService.list(wrapper);

        // 检查每个子级
        for (Hierarchy child : children) {
            // 如果是传感器类型且在报警传感器列表中，添加到结果中
            if (Objects.equals(sensorTypeId, child.getTypeId()) && alarmSensorIds.contains(child.getId())) {
                alarmSensors.add(child);
                log.debug("找到报警传感器: id={}, name={}, typeId={}",
                        child.getId(), child.getName(), child.getTypeId());
            } else {
                // 递归查找子级的子级（继续查找非传感器类型的子级）
                findAlarmSensorsRecursive(child.getId(), sensorTypeId, alarmSensorIds, alarmSensors);
            }
        }
    }

    /**
     * 从目标层级出发，查找其下所有的传感器（不区分报警或离线状态）
     *
     * @param targetHierarchyId 目标层级ID
     * @param sensorTypeId      传感器类型ID
     * @return 该目标层级下的所有传感器列表
     */
    private List<Hierarchy> findAllSensorsUnderTarget(Long targetHierarchyId, Long sensorTypeId) {
        List<Hierarchy> allSensors = new ArrayList<>();
        findAllSensorsRecursive(targetHierarchyId, sensorTypeId, allSensors);
        return allSensors;
    }

    /**
     * 递归查找目标层级下的所有传感器
     *
     * @param hierarchyId  当前层级ID
     * @param sensorTypeId 传感器类型ID
     * @param allSensors   找到的所有传感器集合
     */
    private void findAllSensorsRecursive(Long hierarchyId,
            Long sensorTypeId,
            List<Hierarchy> allSensors) {
        // 获取直接子级
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Hierarchy::getParentId, hierarchyId);
        List<Hierarchy> children = hierarchyService.list(wrapper);

        // 检查每个子级
        for (Hierarchy child : children) {
            // 如果是传感器类型，添加到结果中
            if (Objects.equals(sensorTypeId, child.getTypeId())) {
                allSensors.add(child);
                log.debug("找到传感器: id={}, name={}, typeId={}",
                        child.getId(), child.getName(), child.getTypeId());
            } else {
                // 递归查找子级的子级（继续查找非传感器类型的子级）
                findAllSensorsRecursive(child.getId(), sensorTypeId, allSensors);
            }
        }
    }

    @Override
    public Map<String, Object> alarmList(Long hierarchyId) {
        long methodStartTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 计算时间范围：当前时间-1天到现在
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(30);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'+08:00'");
            String fromTime = yesterday.format(formatter);
            String toTime = now.format(formatter);

            log.info("查询时间范围: {} 到 {}", fromTime, toTime);

            // 2. 获取传感器类型
            HierarchyType sensorHierarchyType = getHierarchyTypeByKey("sensor");
            if (sensorHierarchyType == null) {
                log.warn("未找到传感器类型");
                result.put("error", "未找到传感器类型");
                return result;
            }

            // 3. 获取hierarchyId下所有传感器
            long step3Start = System.currentTimeMillis();
            Long sensorTypeId = sensorHierarchyType.getId();
            List<String> devices = List.of("device_group", "device", "device_point");
            Hierarchy hierarchy = hierarchyService.getById(hierarchyId);
            HierarchyType hierarchyType = hierarchyTypeService.getById(hierarchy.getTypeId());

            List<Hierarchy> sensors;

            if (devices.contains(hierarchyType.getTypeKey())) {
                // 设备类型：通过属性绑定查找传感器
                sensors = getSensorsByPropertyBinding(hierarchyId, hierarchyType, sensorTypeId);
            } else {
                // 其他类型：递归查找传感器
                sensors = findAllSensorsUnderTarget(hierarchyId, sensorTypeId);
            }
            log.info("步骤3-查找传感器耗时: {}ms, 找到{}个传感器", System.currentTimeMillis() - step3Start, sensors.size());

            if (sensors.isEmpty()) {
                result.put("totalEvents", 0);
                result.put("events", new ArrayList<>());
                result.put("message", "未找到传感器");
                return result;
            }

            // 4. 通过SD400MPUtils.testpointFind将code转换为id，并建立testpointId到sensor的映射（并行优化）
            long step4Start = System.currentTimeMillis();
            List<Long> testpointIds = Collections.synchronizedList(new ArrayList<>());
            Map<Long, Hierarchy> testpointIdToSensorMap = new ConcurrentHashMap<>();

            // 使用并行流优化外部接口调用
            sensors.parallelStream()
                .filter(sensor -> sensor.getFullCode() != null && !sensor.getFullCode().trim().isEmpty())
                .forEach(sensor -> {
                    try {
                        JSONObject response = SD400MPUtils.testpointFind(sensor.getFullCode());
                        if (response != null && response.getInt("code") == 200) {
                            JSONObject data = response.getJSONObject("data");
                            if (data != null && data.getStr("id") != null) {
                                Long testpointId = Long.valueOf(data.getStr("id"));
                                testpointIds.add(testpointId);
                                testpointIdToSensorMap.put(testpointId, sensor);
                            }
                        }
                    } catch (Exception e) {
                        log.error("转换异常 - 传感器 {} (code: {}) 时发生异常", sensor.getName(), sensor.getFullCode(), e);
                    }
                });
            log.info("步骤4-testpointFind并行调用耗时: {}ms, 转换成功{}个", System.currentTimeMillis() - step4Start, testpointIds.size());

            // 5. 批量查询所有sensor的branch和power_plant属性（一次性批量查询优化）
            long step5Start = System.currentTimeMillis();
            List<Long> sensorIds = new ArrayList<>(testpointIdToSensorMap.values().stream()
                .map(Hierarchy::getId)
                .collect(Collectors.toSet()));
            Map<Long, Map<String, String>> sensorPropertiesMap = new HashMap<>(); // sensorId -> {dictKey -> hierarchyName}

            if (!sensorIds.isEmpty()) {
                // 一次性批量查询所有sensor的属性（使用MyBatis-Plus的in查询）
                List<HierarchyPropertyVo> allSensorProperties = batchQueryPropertiesByDictKeys(sensorIds, List.of("branch", "power_plant","sensor_type"));
                log.info("步骤5.1-批量查询{}个sensor的属性耗时: {}ms, 查到{}条属性",
                    sensorIds.size(), System.currentTimeMillis() - step5Start, allSensorProperties.size());

                // 收集所有需要查询的层级ID
                long step5_2Start = System.currentTimeMillis();
                Set<Long> hierarchyIdsToQuery = allSensorProperties.stream()
                    .filter(property -> property.getPropertyValue() != null)
                    .map(property -> {
                        try {
                            return Long.parseLong(property.getPropertyValue());
                        } catch (NumberFormatException e) {
                            log.warn("无效的属性值: {}", property.getPropertyValue());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

                // 批量查询层级信息
                Map<Long, String> hierarchyNameMap = new HashMap<>();
                if (!hierarchyIdsToQuery.isEmpty()) {
                    List<Hierarchy> hierarchies = hierarchyService.listByIds(hierarchyIdsToQuery);
                    hierarchyNameMap = hierarchies.stream()
                        .collect(Collectors.toMap(Hierarchy::getId, Hierarchy::getName));
                }
                log.info("步骤5.2-批量查询层级名称耗时: {}ms, 查到{}个层级",
                    System.currentTimeMillis() - step5_2Start, hierarchyNameMap.size());

                // 构建sensorId到属性的映射
                Map<Long, String> finalHierarchyNameMap = hierarchyNameMap;
                allSensorProperties.forEach(property -> {
                    Long sensorId = property.getHierarchyId();
                    String dictKey = property.getTypeProperty().getDict().getDictKey();
                    String propertyValue = property.getPropertyValue();

                    if (propertyValue != null) {
                        try {
                            Long propHierarchyId = Long.parseLong(propertyValue);
                            String hierarchyName = finalHierarchyNameMap.get(propHierarchyId);
                            if (hierarchyName != null) {
                                sensorPropertiesMap.computeIfAbsent(sensorId, k -> new HashMap<>())
                                    .put(dictKey, hierarchyName);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("无效的属性值: {}", propertyValue);
                        }
                    }
                });
            }
            log.info("步骤5-批量查询属性总耗时: {}ms", System.currentTimeMillis() - step5Start);

            if (testpointIds.isEmpty()) {
                result.put("totalEvents", 0);
                result.put("events", new ArrayList<>());
                result.put("message", "未找到有效的测点ID");
                return result;
            }

            // 6. 创建MPIDMultipleJson对象
            MPIDMultipleJson mpidMultipleJson = MPIDMultipleJson.create(testpointIds);

            // 7. 调用SD400MPUtils.events获取事件数据 (idEquipment=1)
            long step7Start = System.currentTimeMillis();
            JSONObject events = SD400MPUtils.events("1", fromTime, toTime, mpidMultipleJson, true);
            log.info("步骤7-调用SD400MPUtils.events耗时: {}ms", System.currentTimeMillis() - step7Start);

            // 8. 解析events数据
            if (events != null && events.getInt("code") == 200) {
                long step8Start = System.currentTimeMillis();
                MPEventList eventList = eventParserService.parseEvents(events);
                log.info("步骤8-解析events数据耗时: {}ms", System.currentTimeMillis() - step8Start);
                if (eventList != null) {

//                    // 8. 构建返回结果，避免循环引用
//                    // 统计总事件数
//                     int totalEvents = eventList.getGroups().values().stream()
//                     .mapToInt(group -> group.getEvents().size())
//                     .sum();
//                     result.put("totalEvents", totalEvents);
//
//                    // 统计各状态事件数量
//                     Map<Integer, Long> stateStatistics = new HashMap<>();
//                     eventList.getGroups().values().forEach(group -> {
//                     group.getEvents().forEach(event -> {
//                     stateStatistics.merge(event.getState(), 1L, Long::sum);
//                     });
//                     });
//                     result.put("stateStatistics", stateStatistics);

                    // 9. 不再分组，将所有事件合并到一个列表中，添加分组key作为事件属性
                    long step9Start = System.currentTimeMillis();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    List<Map<String, Object>> allEvents = new ArrayList<>();

                    eventList.getGroups().forEach((key, group) -> {
                        String tagTitle = group.getTag() != null ? group.getTag().getTitle() : null;
                        // 只保留这两个key
                        if (!key.equals("sys:st") && !key.equals("sys:mont/pd/dia/st/sum")) {
                            return;
                        }

                        // 处理该分组中的所有事件
                        group.getEvents().forEach(event -> {
                            Map<String, Object> eventInfo = new HashMap<>();
                            eventInfo.put("groupKey", key); // 添加分组key作为事件属性
                            eventInfo.put("tagTitle", tagTitle); // 添加标签标题
                            eventInfo.put("state", event.getState());
                            if(event.getState()==null || event.getState()==0){
                                return;
                            }

                            // 根据testpointId找到对应的sensor，然后使用该sensor的属性
                            Long testpointId = event.getTestpointId();
                            if (testpointId != null && testpointIdToSensorMap.containsKey(testpointId)) {
                                Hierarchy sensor = testpointIdToSensorMap.get(testpointId);
                                Long sensorId = sensor.getId();

                                // 添加传感器的full_code字段
                                eventInfo.put("full_code", sensor.getFullCode());

                                // 从预先查询好的sensorPropertiesMap中获取该sensor的属性
                                if (sensorPropertiesMap.containsKey(sensorId)) {
                                    Map<String, String> properties = sensorPropertiesMap.get(sensorId);
                                    // 将branch和power_plant属性放入eventInfo
                                    properties.forEach((dictKey, hierarchyName) -> {
                                        eventInfo.put(dictKey, hierarchyName);
                                    });
                                }
                            }

                            // 处理开始时间
                            String startTime = null;
                            if (event.getStart() != null) {
                                try {
                                    // 检查是否是有效的时间（大于1970年）
                                    if (event.getStart().getTime() > 0) {
                                        startTime = dateFormat.format(event.getStart());
                                    }
                                } catch (Exception e) {
                                    log.warn("格式化开始时间失败: {}", event.getStart(), e);
                                }
                            }
                            eventInfo.put("start", startTime);
                            eventInfo.put("mag" , "-3.14dbm");
                            eventInfo.put("startTimestamp", event.getStart() != null ? event.getStart().getTime() : 0); // 用于排序

                            // 处理结束时间
                            String endTime = null;
                            if (event.getEnd() != null) {
                                try {
                                    // 检查是否是有效的时间（大于1970年）
                                    if (event.getEnd().getTime() > 0) {
                                        endTime = dateFormat.format(event.getEnd());
                                    }
                                } catch (Exception e) {
                                    log.warn("格式化结束时间失败: {}", event.getEnd(), e);
                                }
                            }
                            // if(endTime==null){
                            // return;
                            // }
                            eventInfo.put("end", endTime);
                            //eventInfo.put("satelliteValue", event.getSatelliteValue());

                            // 添加设备和测点名称
                            String equipmentName = eventList.getNamesEq().get(event.getEquipmentId());
                            String testpointName = eventList.getNamesTp().get(event.getTestpointId());
                            eventInfo.put("equipmentName", equipmentName != null ? equipmentName : "未知设备");
                            eventInfo.put("testpointName", testpointName != null ? testpointName : "未知测点");

                            allEvents.add(eventInfo);
                        });
                    });

                    // 按开始时间倒序排列（最新的在前面）
                    allEvents.sort((e1, e2) -> {
                        Long timestamp1 = (Long) e1.get("startTimestamp");
                        Long timestamp2 = (Long) e2.get("startTimestamp");
                        if (timestamp1 == null)
                            timestamp1 = 0L;
                        if (timestamp2 == null)
                            timestamp2 = 0L;
                        return timestamp2.compareTo(timestamp1); // 倒序
                    });

                    // 移除排序用的时间戳字段，保持数据清洁
                    allEvents.forEach(event -> event.remove("startTimestamp"));
                    log.info("步骤9-处理和组装事件数据耗时: {}ms, 处理了{}个事件",
                        System.currentTimeMillis() - step9Start, allEvents.size());

                    result.put("events", allEvents);
                    result.put("totalEvents", allEvents.size());
                }
            }

        } catch (Exception e) {
            log.error("实时报警列表统计异常", e);
            result.put("error", "系统异常: " + e.getMessage());
        }

        log.info("alarmList接口总耗时: {}ms", System.currentTimeMillis() - methodStartTime);
        return result;
    }

    @Override
    public List<HierarchyVo> sensorList(Long hierarchyId, boolean showAllFlag) {
        return hierarchyService.getSensorListByDeviceId(hierarchyId, showAllFlag);
    }

    @Override
    public Map<String, List<HierarchyVo>> sensorListGroupByThreeSystem(Long hierarchyId, boolean showAllFlag) {
        // 获取所有传感器列表
        List<HierarchyVo> sensorList = hierarchyService.getSensorListByDeviceId(hierarchyId, showAllFlag);

        if (sensorList == null || sensorList.isEmpty()) {
            return new HashMap<>();
        }

        // 获取三级系统类型ID
        HierarchyType threeSystemType = getThreeSystemType();
        if (threeSystemType == null) {
            Map<String, List<HierarchyVo>> result = new HashMap<>();
            result.put("未分类", sensorList);
            return result;
        }

        // 查找每个传感器所属的三级系统
        Map<String, List<HierarchyVo>> groupedSensors = new HashMap<>();

        for (HierarchyVo sensor : sensorList) {
            String threeSystemName = findThreeSystemForSensor(sensor.getId(), threeSystemType.getId());
            groupedSensors.computeIfAbsent(threeSystemName, k -> new ArrayList<>()).add(sensor);
        }

        return groupedSensors;
    }

    /**
     * 获取三级系统类型
     * @return 三级系统类型，如果未找到返回null
     */
    private HierarchyType getThreeSystemType() {

        return hierarchyTypeService.lambdaQuery()
        .eq(HierarchyType::getTypeKey, "three_system")
        .one();
    }

    /**
     * 查找传感器所属的三级系统
     * @param sensorId 传感器ID
     * @param threeSystemTypeId 三级系统类型ID
     * @return 三级系统名称
     */
    private String findThreeSystemForSensor(Long sensorId, Long threeSystemTypeId) {
        try {
            // 查询传感器层级
            Hierarchy sensor = hierarchyService.getById(sensorId);
            if (sensor == null) {
                return "未分类";
            }

            // 向上查找三级系统
            Long threeSystemId = findTargetTypeUpward(sensor, threeSystemTypeId, null);
            if (threeSystemId != null) {
                Hierarchy threeSystem = hierarchyService.getById(threeSystemId);
                if (threeSystem != null) {
                    return threeSystem.getName();
                }
            }

            return "未分类";
        } catch (Exception e) {
            log.error("查找传感器{}所属三级系统失败", sensorId, e);
            return "未分类";
        }
    }


    /**
     * 查找指定层级下的所有指定类型的层级
     * 
     * @param hierarchyId 父层级ID
     * @param typeIds 目标类型ID列表
     * @return 符合条件的层级列表
     */
    private List<Hierarchy> findAllHierarchiesUnderTarget(Long hierarchyId, List<Long> typeIds) {
        if (typeIds == null || typeIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 递归查找所有子层级
        List<Hierarchy> allChildren = new ArrayList<>();
        Queue<Long> queue = new LinkedList<>();
        queue.offer(hierarchyId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            List<Hierarchy> children = hierarchyService.list(
                    Wrappers.<Hierarchy>lambdaQuery().eq(Hierarchy::getParentId, currentId));

            for (Hierarchy child : children) {
                if (typeIds.contains(child.getTypeId())) {
                    allChildren.add(child);
                }
                queue.offer(child.getId());
            }
        }

        return allChildren;
    }

    /**
     * 获取设备点绑定的传感器映射
     * 
     * @param devicePointIds 设备点ID列表
     * @return 设备点ID到传感器ID列表的映射
     */
    private Map<Long, List<Long>> getDevicePointSensorBindings(List<Long> devicePointIds) {
        Map<Long, List<Long>> bindingMap = new HashMap<>();

        if (devicePointIds == null || devicePointIds.isEmpty()) {
            return bindingMap;
        }

        // 查询设备点绑定的传感器属性
        HierarchyTypePropertyDict bindingDict = hierarchyTypePropertyDictService.getOne(
                Wrappers.<HierarchyTypePropertyDict>lambdaQuery()
                        .eq(HierarchyTypePropertyDict::getDictKey, "sensors"));

        if (bindingDict == null) {
            log.warn("未找到 sensors 属性字典");
            return bindingMap;
        }

        // 查询所有设备点的绑定属性
        List<HierarchyTypeProperty> typeProperties = hierarchyTypePropertyService.list(
                Wrappers.<HierarchyTypeProperty>lambdaQuery()
                        .eq(HierarchyTypeProperty::getPropertyDictId, bindingDict.getId()));

        if (typeProperties.isEmpty()) {
            log.warn("未找到设备点类型的 sensors 属性配置");
            return bindingMap;
        }

        List<Long> typePropertyIds = typeProperties.stream()
                .map(HierarchyTypeProperty::getId)
                .collect(Collectors.toList());

        List<HierarchyProperty> properties = hierarchyPropertyService.list(
                Wrappers.<HierarchyProperty>lambdaQuery()
                        .in(HierarchyProperty::getTypePropertyId, typePropertyIds)
                        .in(HierarchyProperty::getHierarchyId, devicePointIds));

        // 解析属性值（可能是逗号分隔的传感器ID列表）
        for (HierarchyProperty property : properties) {
            if (property.getPropertyValue() != null && !property.getPropertyValue().trim().isEmpty()) {
                try {
                    String[] sensorIdStrs = property.getPropertyValue().split(",");
                    List<Long> sensorIds = new ArrayList<>();
                    for (String idStr : sensorIdStrs) {
                        sensorIds.add(Long.parseLong(idStr.trim()));
                    }
                    bindingMap.put(property.getHierarchyId(), sensorIds);
                } catch (NumberFormatException e) {
                    log.warn("设备点 {} 的传感器绑定属性值格式错误: {}", 
                            property.getHierarchyId(), property.getPropertyValue());
                }
            }
        }

        return bindingMap;
    }

    /**
     * 创建空结果
     */
    private Map<String, Object> createEmptyResult(Long targetTypeId, Integer statisticalType) {
        Map<String, Object> result = new HashMap<>();
        result.put("statisticalType", statisticalType);
        result.put("targetTypeId", targetTypeId);
        result.put("statistics", new ArrayList<>());

        if (statisticalType == 1) {
            // 设备级别统计
            result.put("totalDeviceCount", 0);
            result.put("totalAlarmDeviceCount", 0);
            result.put("totalOfflineDeviceCount", 0);
        } else {
            // 传感器级别统计
            result.put("totalSensorCount", 0);
            result.put("totalAlarmSensorCount", 0);
            result.put("totalOfflineSensorCount", 0);
        }

        return result;
    }

    /**
     * 获取目标层级列表
     */
    private List<Hierarchy> getTargetHierarchies(Long hierarchyId, Long targetTypeId) {
        List<Long> matchedIds = new ArrayList<>();
        findMatchingDescendants(hierarchyId, targetTypeId, matchedIds);

        LambdaQueryWrapper<Hierarchy> targetTypeQuery = new LambdaQueryWrapper<Hierarchy>()
                .eq(Hierarchy::getTypeId, targetTypeId);

        if (!matchedIds.isEmpty()) {
            HierarchyTypeVo hierarchyTypeVo = hierarchyTypeService.queryById(targetTypeId);
            if (hierarchyTypeVo.getCascadeFlag() && hierarchyTypeVo.getCascadeParentId() != null) {
                targetTypeQuery.in(Hierarchy::getId, matchedIds);
            }
        }

        return hierarchyService.list(targetTypeQuery);
    }

    /**
     * 获取所有传感器的报警级别（report_st值）
     */
    private Map<Long, Integer> getSensorAlarmLevels(Long sensorTypeId) {
        Map<Long, Integer> alarmLevels = new HashMap<>();

        // 获取sys:st字典
        HierarchyTypePropertyDict dict = hierarchyTypePropertyDictService.getOne(
                new LambdaQueryWrapper<HierarchyTypePropertyDict>()
                        .eq(HierarchyTypePropertyDict::getDictKey, "sys:st"));

        if (dict == null) {
            log.warn("未找到sys:st属性字典");
            return alarmLevels;
        }

        HierarchyTypeProperty typeProperty = hierarchyTypePropertyService.getOne(
                Wrappers.<HierarchyTypeProperty>lambdaQuery()
                        .eq(HierarchyTypeProperty::getPropertyDictId, dict.getId())
                        .eq(HierarchyTypeProperty::getTypeId, sensorTypeId));

        if (typeProperty == null) {
            log.warn("传感器类型未配置sys:st属性");
            return alarmLevels;
        }

        // 查找所有report_st属性
        List<HierarchyProperty> properties = hierarchyPropertyService.list(
                new LambdaQueryWrapper<HierarchyProperty>()
                        .eq(HierarchyProperty::getTypePropertyId, typeProperty.getId()));

        for (HierarchyProperty property : properties) {
            if (property.getPropertyValue() != null && !property.getPropertyValue().trim().isEmpty()) {
                try {
                    int alarmLevel = Integer.parseInt(property.getPropertyValue().trim());
                    alarmLevels.put(property.getHierarchyId(), alarmLevel);
                } catch (NumberFormatException e) {
                    log.warn("传感器 {} 的report_st值无效: {}", property.getHierarchyId(), property.getPropertyValue());
                }
            }
        }

        log.info("获取到 {} 个传感器的报警级别", alarmLevels.size());
        return alarmLevels;
    }

    /**
     * 查找目标层级下的所有指定类型设备
     */
    private List<Hierarchy> findDevicesUnderTarget(Long targetHierarchyId, Long targetTypeId) {
        List<Hierarchy> devices = new ArrayList<>();
        findDevicesRecursive(targetHierarchyId, targetTypeId, devices);
        return devices;
    }

    /**
     * 递归查找指定类型的设备
     */
    private void findDevicesRecursive(Long hierarchyId, Long targetTypeId, List<Hierarchy> devices) {
        // 获取直接子级
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Hierarchy::getParentId, hierarchyId);
        List<Hierarchy> children = hierarchyService.list(wrapper);

        for (Hierarchy child : children) {
            // 如果是目标类型设备，添加到结果中
            if (Objects.equals(targetTypeId, child.getTypeId())) {
                devices.add(child);
            }
            // 继续递归查找子级
            findDevicesRecursive(child.getId(), targetTypeId, devices);
        }
    }

    /**
     * 计算设备及其递归子集中关联的所有传感器的最高报警级别
     */
    private int getDeviceMaxAlarmLevel(Long deviceId, Map<Long, Integer> sensorAlarmLevels) {
        Integer maxAlarmLevel = 0;

        // 获取该设备及其递归子集中的所有传感器ID
        List<Long> allSensorIds = getAllSensorIdsUnderDevice(deviceId);

        // 找出最高报警级别
        for (Long sensorId : allSensorIds) {
            Integer alarmLevel = sensorAlarmLevels.get(sensorId);
            if (alarmLevel != null && alarmLevel > maxAlarmLevel) {
                maxAlarmLevel = alarmLevel;
            }
        }

        return maxAlarmLevel;
    }

    /**
     * 获取设备及其递归子集中关联的所有传感器ID
     */
    private List<Long> getAllSensorIdsUnderDevice(Long deviceId) {
        List<Long> allSensorIds = new ArrayList<>();

        // 获取当前设备直接关联的传感器
        List<Long> directSensorIds = getAssociatedSensorIds(deviceId);
        allSensorIds.addAll(directSensorIds);

        // 递归获取子设备关联的传感器
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Hierarchy::getParentId, deviceId);
        List<Hierarchy> children = hierarchyService.list(wrapper);

        for (Hierarchy child : children) {
            List<Long> childSensorIds = getAllSensorIdsUnderDevice(child.getId());
            allSensorIds.addAll(childSensorIds);
        }

        return allSensorIds.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 获取设备关联的传感器ID列表
     * 从设备的属性中读取逗号分隔的传感器ID字符串（dictKey=sensors的字典）
     */
    private List<Long> getAssociatedSensorIds(Long deviceId) {
        List<Long> sensorIds = new ArrayList<>();

        // 获取dictKey=sensors的属性字典
        HierarchyTypePropertyDict dict = hierarchyTypePropertyDictService.getOne(
                new LambdaQueryWrapper<HierarchyTypePropertyDict>()
                        .eq(HierarchyTypePropertyDict::getDictKey, "sensors"));

        if (dict == null) {
            log.debug("未找到dictKey=sensors的属性字典");
            return sensorIds;
        }

        // 先找到该设备类型对应的HierarchyTypeProperty
        Hierarchy device = hierarchyService.getById(deviceId);
        if (device == null) {
            log.debug("设备 {} 不存在", deviceId);
            return sensorIds;
        }

        HierarchyTypeProperty typeProperty = hierarchyTypePropertyService.getOne(
                new LambdaQueryWrapper<HierarchyTypeProperty>()
                        .eq(HierarchyTypeProperty::getTypeId, device.getTypeId())
                        .eq(HierarchyTypeProperty::getPropertyDictId, dict.getId()));

        if (typeProperty == null) {
            log.debug("设备类型 {} 未配置sensors属性", device.getTypeId());
            return sensorIds;
        }

        // 查找该设备的sensors属性值
        List<HierarchyProperty> properties = hierarchyPropertyService.list(
                new LambdaQueryWrapper<HierarchyProperty>()
                        .eq(HierarchyProperty::getHierarchyId, deviceId)
                        .eq(HierarchyProperty::getTypePropertyId, typeProperty.getId()));

        for (HierarchyProperty property : properties) {
            if (property.getPropertyValue() != null && !property.getPropertyValue().trim().isEmpty()) {
                // 解析逗号分隔的传感器ID
                String[] idStrings = property.getPropertyValue().split(",");
                for (String idString : idStrings) {
                    try {
                        Long sensorId = Long.valueOf(idString.trim());
                        sensorIds.add(sensorId);
                    } catch (NumberFormatException e) {
                        log.warn("设备 {} 的sensors属性包含无效的传感器ID: {}", deviceId, idString);
                    }
                }
            }
        }

        log.debug("设备 {} 关联了 {} 个传感器(sensors): {}", deviceId, sensorIds.size(), sensorIds);
        return sensorIds;
    }

    /**
     * 批量查询层级属性（根据dictKey过滤）
     *
     * @param hierarchyIds 层级ID列表
     * @param dictKeys 字典key列表
     * @return 属性列表
     */
    private List<HierarchyPropertyVo> batchQueryPropertiesByDictKeys(List<Long> hierarchyIds, List<String> dictKeys) {
        if (hierarchyIds == null || hierarchyIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询所有属性
        List<HierarchyProperty> properties = hierarchyPropertyService.lambdaQuery()
            .in(HierarchyProperty::getHierarchyId, hierarchyIds)
            .list();

        if (properties.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询类型属性和字典信息
        Set<Long> typePropertyIds = properties.stream()
            .map(HierarchyProperty::getTypePropertyId)
            .collect(Collectors.toSet());

        Map<Long, HierarchyTypeProperty> typePropertyMap = hierarchyTypePropertyService.lambdaQuery()
            .in(HierarchyTypeProperty::getId, typePropertyIds)
            .list()
            .stream()
            .collect(Collectors.toMap(HierarchyTypeProperty::getId, tp -> tp));

        // 批量查询字典
        Set<Long> dictIds = typePropertyMap.values().stream()
            .map(HierarchyTypeProperty::getPropertyDictId)
            .collect(Collectors.toSet());

        Map<Long, HierarchyTypePropertyDict> dictMap = hierarchyTypePropertyDictService.lambdaQuery()
            .in(HierarchyTypePropertyDict::getId, dictIds)
            .list()
            .stream()
            .collect(Collectors.toMap(HierarchyTypePropertyDict::getId, dict -> dict));

        // 组装VO并过滤
        List<HierarchyPropertyVo> result = new ArrayList<>();
        Set<String> dictKeySet = new HashSet<>(dictKeys);

        for (HierarchyProperty property : properties) {
            HierarchyTypeProperty typeProperty = typePropertyMap.get(property.getTypePropertyId());
            if (typeProperty != null) {
                HierarchyTypePropertyDict dict = dictMap.get(typeProperty.getPropertyDictId());
                if (dict != null && dictKeySet.contains(dict.getDictKey())) {
                    HierarchyPropertyVo vo = BeanUtil.toBean(property, HierarchyPropertyVo.class);
                    HierarchyTypePropertyVo typePropertyVo = BeanUtil.toBean(typeProperty, HierarchyTypePropertyVo.class);
                    HierarchyTypePropertyDictVo dictVo = BeanUtil.toBean(dict, HierarchyTypePropertyDictVo.class);
                    typePropertyVo.setDict(dictVo);
                    vo.setTypeProperty(typePropertyVo);
                    result.add(vo);
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> statisticsByDeviceCategory(Long hierarchyId) {
        Map<String, Object> result = new HashMap<>();

        log.info("开始执行按设备类别统计 - hierarchyId: {}", hierarchyId);

        // 获取所有设备类型 (typeKey = /ledger/deviceType)
        List<HierarchyType> deviceTypes = hierarchyTypeService.lambdaQuery()
            .eq(HierarchyType::getTypeKey, "device_category")
            .list();

        log.info("找到 {} 个设备类型(typeKey=/ledger/deviceType)", deviceTypes.size());

        // 构建每个设备类型下的树形结构
        List<Map<String, Object>> deviceTypeTreeList = new ArrayList<>();

        for (HierarchyType deviceType : deviceTypes) {
            log.info("处理设备类型: id={}, name={}, typeKey={}",
                deviceType.getId(), deviceType.getName(), deviceType.getTypeKey());
            Map<String, Object> deviceTypeNode = new HashMap<>();
            deviceTypeNode.put("id", deviceType.getId());
            deviceTypeNode.put("name", deviceType.getName());
            deviceTypeNode.put("typeKey", deviceType.getTypeKey());
            deviceTypeNode.put("level", "deviceType");

            // 查询该设备类型下的所有设备区域
            List<Hierarchy> deviceAreas = hierarchyService.lambdaQuery()
                .eq(Hierarchy::getTypeId, deviceType.getId())
                .isNull(Hierarchy::getParentId)
                .list();

            List<Map<String, Object>> deviceAreaList = new ArrayList<>();
            for (Hierarchy deviceArea : deviceAreas) {
                Map<String, Object> deviceAreaNode = buildDeviceAreaTree(deviceArea);
                deviceAreaList.add(deviceAreaNode);
            }

            deviceTypeNode.put("children", deviceAreaList);
            deviceTypeTreeList.add(deviceTypeNode);
        }

        result.put("deviceTypeTree", deviceTypeTreeList);
        return result;
    }

    /**
     * 构建设备区域树（包含设备和设备点）
     */
    private Map<String, Object> buildDeviceAreaTree(Hierarchy deviceArea) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", deviceArea.getId());
        node.put("name", deviceArea.getName());

        // 获取类型信息
        HierarchyType type = hierarchyTypeService.getById(deviceArea.getTypeId());
        node.put("typeKey", type != null ? type.getTypeKey() : null);
        node.put("level", "deviceArea");

        // 查询该设备区域下的所有设备
        List<Hierarchy> devices = hierarchyService.lambdaQuery()
            .eq(Hierarchy::getParentId, deviceArea.getId())
            .list();

        // 过滤出设备节点（根据typeKey包含device）
        List<Map<String, Object>> deviceList = new ArrayList<>();
        for (Hierarchy device : devices) {
            HierarchyType deviceHierarchyType = hierarchyTypeService.getById(device.getTypeId());
            if (deviceHierarchyType != null && deviceHierarchyType.getTypeKey() != null
                && deviceHierarchyType.getTypeKey().contains("/device")) {
                Map<String, Object> deviceNode = buildDeviceTree(device);
                deviceList.add(deviceNode);
            }
        }

        node.put("children", deviceList);
        return node;
    }

    /**
     * 构建设备树（包含设备点）
     */
    private Map<String, Object> buildDeviceTree(Hierarchy device) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", device.getId());
        node.put("name", device.getName());

        // 获取类型信息
        HierarchyType type = hierarchyTypeService.getById(device.getTypeId());
        node.put("typeKey", type != null ? type.getTypeKey() : null);
        node.put("level", "device");

        // 查询该设备下的所有设备点
        List<Hierarchy> devicePoints = hierarchyService.lambdaQuery()
            .eq(Hierarchy::getParentId, device.getId())
            .list();

        // 过滤出设备点节点（根据typeKey包含devicePoint）
        List<Map<String, Object>> devicePointList = new ArrayList<>();
        for (Hierarchy devicePoint : devicePoints) {
            HierarchyType devicePointType = hierarchyTypeService.getById(devicePoint.getTypeId());
            if (devicePointType != null && devicePointType.getTypeKey() != null
                && devicePointType.getTypeKey().contains("/devicePoint")) {
                Map<String, Object> devicePointNode = new HashMap<>();
                devicePointNode.put("id", devicePoint.getId());
                devicePointNode.put("name", devicePoint.getName());
                devicePointNode.put("typeKey", devicePointType.getTypeKey());
                devicePointNode.put("level", "devicePoint");
                devicePointList.add(devicePointNode);
            }
        }

        node.put("children", devicePointList);
        return node;
    }

}
