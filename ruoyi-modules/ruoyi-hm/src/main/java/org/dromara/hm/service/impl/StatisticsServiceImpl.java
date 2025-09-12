package org.dromara.hm.service.impl;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.MPIDMultipleJson;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.domain.*;
import org.dromara.hm.domain.sd400mp.MPEventList;
import org.dromara.hm.domain.vo.HierarchyTypeVo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.service.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
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

    private final EventParserService eventParserService;

    @Override
    public List<Map<String, Object>> getTargetTypeList(Long hierarchyId, Long targetTypeId) {
        List<Map<String, Object>> put = new ArrayList<>();
        List<Map<String,Long>> longIntegerMap = hierarchyService.selectTargetTypeHierarchyList(hierarchyService.selectChildHierarchyIds(hierarchyId), targetTypeId);
        for (Map<String, Long> stringIntegerMap : longIntegerMap) {
            Map<String, Object> nn = new  HashMap<>();
            Long count = stringIntegerMap.get("count");
            Hierarchy hierarchy = hierarchyService.getById( stringIntegerMap.get("hierarchy_id"));
            nn.put("name",hierarchy.getName());
            nn.put("count",count);
            put.add(nn);
        }
        return put;
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
        if (statisticalType == null) {
            statisticalType = 2; // 默认为被监测设备统计
        }

        // 通用前置逻辑：获取传感器类型和目标层级列表
        HierarchyType sensorHierarchyType = hierarchyTypeService.getOne(
            new LambdaQueryWrapper<HierarchyType>().eq(HierarchyType::getTypeKey, "sensor"));

        if (sensorHierarchyType == null) {
            return createEmptyResult(targetTypeId, statisticalType);
        }

        // 获取目标层级列表（都是从hierarchyId递归找targetTypeId的层级）
        List<Hierarchy> targetHierarchies = getTargetHierarchies(hierarchyId, targetTypeId);
        if (targetHierarchies.isEmpty()) {
            return createEmptyResult(targetTypeId, statisticalType);
        }

        log.info("找到 {} 个目标层级进行统计", targetHierarchies.size());

        if (statisticalType == 1) {
            // 统计维度：按设备关联的1002传感器统计
            return alarmByDeviceLevel(targetHierarchies, sensorHierarchyType, targetTypeId);
        } else {
            // 被监测设备统计：按目标层级下属传感器统计
            return alarmByMonitoredDevice(targetHierarchies, sensorHierarchyType, targetTypeId);
        }
    }

    /**
     * 统计维度：按目标层级统计设备报警情况
     * 每个目标层级下统计设备，设备的report_st等于该设备及其递归子集关联的所有传感器中的最高报警等级
     */
    private Map<String, Object> alarmByDeviceLevel(List<Hierarchy> targetHierarchies, HierarchyType sensorHierarchyType, Long targetTypeId) {
        log.info("执行按目标层级设备报警统计 - 目标层级数: {}", targetHierarchies.size());

        // 获取设备类型（typeKey = "device"）
        HierarchyType deviceHierarchyType = hierarchyTypeService.getOne(
            new LambdaQueryWrapper<HierarchyType>().eq(HierarchyType::getTypeKey, "device"));

        if (deviceHierarchyType == null) {
            log.warn("未找到device类型");
            return createEmptyResult(targetTypeId, 1);
        }

        // 获取所有传感器的报警状态（report_st值）
        Map<Long, Integer> sensorAlarmLevels = getSensorAlarmLevels(sensorHierarchyType.getId());

        // 统计每个目标层级下的设备报警情况
        List<Map<String, Object>> statistics = new ArrayList<>();
        int totalDeviceCount = 0;
        int totalAlarmDeviceCount = 0;
        int totalGeneralCount = 0;  // 全局一般报警设备数量
        int totalSeriousCount = 0;  // 全局严重报警设备数量

        for (Hierarchy targetHierarchy : targetHierarchies) {
            // 查找该目标层级下的所有device类型的层级
            List<Hierarchy> devicesUnderTarget = findDevicesUnderTarget(targetHierarchy.getId(), deviceHierarchyType.getId());

            // 统计该层级下的报警设备
            List<Map<String, Object>> alarmDevices = new ArrayList<>();
            int hierarchyAlarmDeviceCount = 0;
            int hierarchyGeneralCount = 0;   // 该层级一般报警设备数量
            int hierarchySeriousCount = 0;   // 该层级严重报警设备数量

            for (Hierarchy device : devicesUnderTarget) {
                // 计算该设备及其递归子集中关联的所有传感器的最高报警级别
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

        log.info("按目标层级设备统计完成：共{}个层级，{}个设备，{}个报警设备（一般{}个，严重{}个）",
                targetHierarchies.size(), totalDeviceCount, totalAlarmDeviceCount, totalGeneralCount, totalSeriousCount);

        return result;
    }

    /**
     * 被监测设备统计（原有逻辑）
     */
    private Map<String, Object> alarmByMonitoredDevice(List<Hierarchy> targetHierarchies, HierarchyType sensorHierarchyType, Long targetTypeId) {
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
            targetTypeId, targetHierarchies.size(), totalSensorCount, totalAlarmSensorCount, totalOfflineSensorCount);

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




    /**
     * 获取包含指定字典属性且属性值为 "1" 的层级ID列表
     * 根据用户数据，使用 type_property_id=70 的字典
     * @return 包含属性值为 "1" 的层级ID列表
     */
    private List<Long> getReportStHierarchyIds(Long typeId) {

        // 验证字典是否存在
        HierarchyTypePropertyDict dict = hierarchyTypePropertyDictService.getOne(new LambdaQueryWrapper<HierarchyTypePropertyDict>().eq(HierarchyTypePropertyDict::getDictKey,"sys:st"));

        HierarchyTypeProperty one = hierarchyTypePropertyService.getOne(Wrappers.<HierarchyTypeProperty>lambdaQuery()
            .eq(HierarchyTypeProperty::getPropertyDictId, dict.getId())
            .eq(HierarchyTypeProperty::getTypeId, typeId)
        );
        // 2. 查找包含指定字典属性的层级（所有记录）

        LambdaQueryWrapper<HierarchyProperty> propertyWrapper = Wrappers.lambdaQuery();
        propertyWrapper.eq(HierarchyProperty::getTypePropertyId, one.getId());
        List<HierarchyProperty> allReportStProperties = hierarchyPropertyService.list(propertyWrapper);

        log.info("找到 {} 个属性记录", allReportStProperties.size());

        // 调试：输出所有找到的属性记录
        if (!allReportStProperties.isEmpty()) {
            log.debug("属性记录详情 (前10条):");
            allReportStProperties.stream().limit(10).forEach(prop ->
                log.debug("属性: id={}, hierarchyId={}, typePropertyId={}, value='{}'",
                    prop.getId(), prop.getHierarchyId(), prop.getTypePropertyId(), prop.getPropertyValue())
            );
        }

        List<HierarchyProperty> validProperties = allReportStProperties.stream()
            .filter(this::isValidPropertyValue)
            .toList();

        // 4. 提取有效的层级ID
        List<Long> hierarchyIds = validProperties.stream()
            .map(HierarchyProperty::getHierarchyId)
            .distinct()
            .toList();

        log.info("找到 {} 个包含属性值不为为 0 的层级: {}", hierarchyIds.size(), hierarchyIds);
        return hierarchyIds;
    }

    /**
     * 获取包含offline_flag属性且属性值为 "1" 的层级ID列表
     * @param typeId 类型ID
     * @return 包含offline_flag属性值为 "1" 的层级ID列表
     */
    private List<Long> getOfflineFlagHierarchyIds(Long typeId) {
        // 验证字典是否存在
        HierarchyTypePropertyDict dict = hierarchyTypePropertyDictService.getOne(new LambdaQueryWrapper<HierarchyTypePropertyDict>().eq(HierarchyTypePropertyDict::getDictKey,"offline_flag"));

        HierarchyTypeProperty one = hierarchyTypePropertyService.getOne(Wrappers.<HierarchyTypeProperty>lambdaQuery()
            .eq(HierarchyTypeProperty::getPropertyDictId, dict.getId())
            .eq(HierarchyTypeProperty::getTypeId, typeId)
        );

        // 查找包含指定字典属性的层级（所有记录）
        LambdaQueryWrapper<HierarchyProperty> propertyWrapper = Wrappers.lambdaQuery();
        propertyWrapper.eq(HierarchyProperty::getTypePropertyId, one.getId());
        List<HierarchyProperty> allOfflineFlagProperties = hierarchyPropertyService.list(propertyWrapper);

        log.info("找到 {} 个offline_flag属性记录", allOfflineFlagProperties.size());

        // 调试：输出所有找到的属性记录
        if (!allOfflineFlagProperties.isEmpty()) {
            log.debug("offline_flag属性记录详情 (前10条):");
            allOfflineFlagProperties.stream().limit(10).forEach(prop ->
                log.debug("属性: id={}, hierarchyId={}, typePropertyId={}, value='{}'",
                    prop.getId(), prop.getHierarchyId(), prop.getTypePropertyId(), prop.getPropertyValue())
            );
        }

        List<HierarchyProperty> validProperties = allOfflineFlagProperties.stream()
            .filter(property -> "1".equals(property.getPropertyValue()))
            .toList();

        // 提取有效的层级ID
        List<Long> hierarchyIds = validProperties.stream()
            .map(HierarchyProperty::getHierarchyId)
            .distinct()
            .toList();

        log.info("找到 {} 个包含offline_flag值为 '1' 的层级: {}", hierarchyIds.size(), hierarchyIds);
        return hierarchyIds;
    }

    /**
     * 检查属性值是否大于0
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
     * @param targetHierarchyId 目标层级ID
     * @param sensorTypeId 传感器类型ID
     * @param alarmSensorIds 报警传感器ID列表
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
     * @param hierarchyId 当前层级ID
     * @param sensorTypeId 传感器类型ID
     * @param alarmSensorIds 报警传感器ID列表
     * @param alarmSensors 找到的报警传感器集合
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
     * @param targetHierarchyId 目标层级ID
     * @param sensorTypeId 传感器类型ID
     * @return 该目标层级下的所有传感器列表
     */
    private List<Hierarchy> findAllSensorsUnderTarget(Long targetHierarchyId, Long sensorTypeId) {
        List<Hierarchy> allSensors = new ArrayList<>();
        findAllSensorsRecursive(targetHierarchyId, sensorTypeId, allSensors);
        return allSensors;
    }

    /**
     * 递归查找目标层级下的所有传感器
     * @param hierarchyId 当前层级ID
     * @param sensorTypeId 传感器类型ID
     * @param allSensors 找到的所有传感器集合
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
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 计算时间范围：当前时间-1天到现在
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(7);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'+08:00'");
            String fromTime = yesterday.format(formatter);
            String toTime = now.format(formatter);

            log.info("查询时间范围: {} 到 {}", fromTime, toTime);

            // 2. 获取传感器类型
            HierarchyType sensorHierarchyType = hierarchyTypeService.getOne(
                new LambdaQueryWrapper<HierarchyType>().eq(HierarchyType::getTypeKey, "sensor"));

            if (sensorHierarchyType == null) {
                log.warn("未找到传感器类型");
                result.put("error", "未找到传感器类型");
                return result;
            }

            // 3. 递归获取hierarchyId下所有传感器的code
            List<Hierarchy> sensors = findAllSensorsUnderTarget(hierarchyId, sensorHierarchyType.getId());
            log.info("找到 {} 个传感器", sensors.size());

            if (sensors.isEmpty()) {
                result.put("totalEvents", 0);
                result.put("events", new ArrayList<>());
                result.put("message", "未找到传感器");
                return result;
            }

            // 4. 通过SD400MPUtils.testpointFind将code转换为id
            List<Long> testpointIds = new ArrayList<>();
            log.info("开始转换传感器code为测点ID，传感器详情：");
            for (Hierarchy sensor : sensors) {
                log.info("传感器: id={}, name={}, code={}", sensor.getId(), sensor.getName(), sensor.getCode());

                if (sensor.getCode() != null && !sensor.getCode().trim().isEmpty()) {
                    try {
                        JSONObject response = SD400MPUtils.testpointFind(sensor.getCode());
                        if (response != null && response.getInt("code") == 200) {
                            JSONObject data = response.getJSONObject("data");
                            if (data != null && data.getStr("id") != null) {
                                testpointIds.add(Long.valueOf(data.getStr("id")));
                                log.info("成功转换 - 传感器 {} (code: {}) → 测点ID: {}",
                                    sensor.getName(), sensor.getCode(), data.getStr("id"));
                            }
                        } else {
                            log.warn("转换失败 - 传感器 {} (code: {}) 未找到对应的测点ID，响应: {}",
                                sensor.getName(), sensor.getCode(), response);
                        }
                    } catch (Exception e) {
                        log.error("转换异常 - 传感器 {} (code: {}) 时发生异常", sensor.getName(), sensor.getCode(), e);
                    }
                }
            }


            if (testpointIds.isEmpty()) {
                result.put("totalEvents", 0);
                result.put("events", new ArrayList<>());
                result.put("message", "未找到有效的测点ID");
                return result;
            }

            // 5. 创建MPIDMultipleJson对象
            MPIDMultipleJson mpidMultipleJson = MPIDMultipleJson.create(testpointIds);

            // 6. 调用SD400MPUtils.events获取事件数据 (idEquipment=1)
            JSONObject events = SD400MPUtils.events("1", fromTime, toTime, mpidMultipleJson, true);

            // 7. 解析events数据
            if (events != null && events.getInt("code") == 200) {
                MPEventList eventList = eventParserService.parseEvents(events);
                if (eventList != null) {
                    log.info("成功解析events，共{}个分组，{}个设备名称，{}个测点名称",
                            eventList.getGroups().size(),
                            eventList.getNamesEq().size(),
                            eventList.getNamesTp().size());

                    // 8. 构建返回结果，避免循环引用
                    // 统计总事件数
//                    int totalEvents = eventList.getGroups().values().stream()
//                            .mapToInt(group -> group.getEvents().size())
//                            .sum();
//                    result.put("totalEvents", totalEvents);

                     //统计各状态事件数量
//                    Map<Integer, Long> stateStatistics = new HashMap<>();
//                    eventList.getGroups().values().forEach(group -> {
//                        group.getEvents().forEach(event -> {
//                            stateStatistics.merge(event.getState(), 1L, Long::sum);
//                        });
//                    });
//                    result.put("stateStatistics", stateStatistics);

                    // 不再分组，将所有事件合并到一个列表中，添加分组key作为事件属性
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    List<Map<String, Object>> allEvents = new ArrayList<>();

                    eventList.getGroups().forEach((key, group) -> {
                        String tagTitle = group.getTag() != null ? group.getTag().getTitle() : null;

                        // 处理该分组中的所有事件
                        group.getEvents().forEach(event -> {
                            Map<String, Object> eventInfo = new HashMap<>();
                            eventInfo.put("groupKey", key); // 添加分组key作为事件属性
                            eventInfo.put("tagTitle", tagTitle); // 添加标签标题
                            eventInfo.put("state", event.getState());

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
                            eventInfo.put("end", endTime);
                            eventInfo.put("satelliteValue", event.getSatelliteValue());

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
                        if (timestamp1 == null) timestamp1 = 0L;
                        if (timestamp2 == null) timestamp2 = 0L;
                        return timestamp2.compareTo(timestamp1); // 倒序
                    });

                    // 移除排序用的时间戳字段，保持数据清洁
                    allEvents.forEach(event -> event.remove("startTimestamp"));

                    result.put("events", allEvents);
                    result.put("totalEvents", allEvents.size());
                }
            }

        } catch (Exception e) {
            log.error("实时报警列表统计异常", e);
            result.put("error", "系统异常: " + e.getMessage());
        }

        return result;
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


}
