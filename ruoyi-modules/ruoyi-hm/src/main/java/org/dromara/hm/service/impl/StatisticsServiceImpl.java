package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.Testpoint;
import org.dromara.hm.domain.TestpointOffline;
import org.dromara.hm.domain.TestpointData;
import org.dromara.hm.enums.EquipmentDutEnum;
import org.dromara.hm.enums.TestpointTypeEnum;
import org.dromara.hm.enums.AlarmTypeEnum;
import org.dromara.hm.enums.StatisticsCountTypeEnum;
import org.dromara.hm.mapper.EquipmentMapper;
import org.dromara.hm.mapper.HierarchyMapper;
import org.dromara.hm.mapper.TestpointMapper;
import org.dromara.hm.mapper.TestpointOfflineMapper;
import org.dromara.hm.mapper.TestpointDataMapper;
import org.dromara.hm.service.IStatisticsService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 统计服务实现类
 *
 * @author Mashir0
 * @date 2025-08-21
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements IStatisticsService {

    private final EquipmentMapper equipmentMapper;
    private final HierarchyMapper hierarchyMapper;
    private final TestpointMapper testpointMapper;
    private final TestpointOfflineMapper testpointOfflineMapper;
    private final TestpointDataMapper testpointDataMapper;

    @Override
    public Map<String, Object> getPowerPlantStatistics(Long hierarchyId) {
        if(hierarchyId == null){
            LambdaQueryWrapper<Hierarchy> rootWrapper = Wrappers.lambdaQuery();
            rootWrapper.eq(Hierarchy::getType, 0);
            Hierarchy rootHierarchies = hierarchyMapper.selectOne(rootWrapper,false);
            if (rootHierarchies ==null) {
                return null;
            }
            hierarchyId = rootHierarchies.getId();
        }
        Map<String, Object> result = new HashMap<>();

        // 1. 获取直接子层级列表（电厂列表）
        List<Hierarchy> childHierarchies = getChildHierarchies(hierarchyId);

        if (childHierarchies.isEmpty()) {
            childHierarchies = new ArrayList<>();
        }
        result.put("branchFactoryCount", childHierarchies.size());

        List<Map<String, Object>> voltageLevelPlantStats = getVoltageLevelPlantStatistics(hierarchyId,childHierarchies);
        result.put("voltageLevelPlantStats", voltageLevelPlantStats);

        return result;
    }

    @Override
    public Map<String, Object> getEquipmentDetailStatistics(Long hierarchyId) {
        // 如果未传入层级ID，获取根目录层级
        if (hierarchyId == null) {
            LambdaQueryWrapper<Hierarchy> rootWrapper = Wrappers.lambdaQuery();
            rootWrapper.eq(Hierarchy::getType, 0);
            Hierarchy rootHierarchy = hierarchyMapper.selectOne(rootWrapper, false);
            if (rootHierarchy == null) {
                return Map.of("dutMajorStats", new ArrayList<>());
            }
            hierarchyId = rootHierarchy.getId();
        }

        Map<String, Object> result = new HashMap<>();

        // 统计设备大类分组数据
        List<Map<String, Object>> dutMajorStats = getDutMajorStatistics(hierarchyId);
        result.put("dutMajorStats", dutMajorStats);

        return result;
    }

    @Override
    public Map<String, Object> getTestpointDetailStatistics(Long hierarchyId) {
        // 如果未传入层级ID，获取根目录层级
        if (hierarchyId == null) {
            LambdaQueryWrapper<Hierarchy> rootWrapper = Wrappers.lambdaQuery();
            rootWrapper.eq(Hierarchy::getType, 0);
            Hierarchy rootHierarchy = hierarchyMapper.selectOne(rootWrapper, false);
            if (rootHierarchy == null) {
                return Map.of("testpointTypeStats", new ArrayList<>());
            }
            hierarchyId = rootHierarchy.getId();
        }

        Map<String, Object> result = new HashMap<>();

        // 统计测点类型分组数据
        List<Map<String, Object>> testpointTypeStats = getTestpointTypeStatistics(hierarchyId);
        result.put("testpointTypeStats", testpointTypeStats);

        return result;
    }

    @Override
    public Map<String, Object> getReportDetailStatistics(Long hierarchyId, StatisticsCountTypeEnum countType) {
        Map<String, Object> result = new HashMap<>();
        // 如果未传入层级ID，获取根目录层级
        if (hierarchyId == null) {
            LambdaQueryWrapper<Hierarchy> rootWrapper = Wrappers.lambdaQuery();
            rootWrapper.eq(Hierarchy::getType, 0);
            Hierarchy rootHierarchy = hierarchyMapper.selectOne(rootWrapper, false);
            if (rootHierarchy == null) {
                return Map.of("alarmTypeStats", new ArrayList<>());
            }
            hierarchyId = rootHierarchy.getId();
        }

        // 统计报警类型分组数据（按子层级统计最高报警等级）
        List<Map<String, Object>> alarmTypeStats = getHierarchyAlarmStatistics(hierarchyId, countType);
        result.put("alarmTypeStats", alarmTypeStats);

        return result;
    }

    @Override
    public Map<String, Object> getOfflineDetailStatistics(Long hierarchyId, StatisticsCountTypeEnum countType) {
        Map<String, Object> result = new HashMap<>();

        // 如果未传入层级ID，获取根目录层级
        if (hierarchyId == null) {
            LambdaQueryWrapper<Hierarchy> rootWrapper = Wrappers.lambdaQuery();
            rootWrapper.eq(Hierarchy::getType, 0);
            Hierarchy rootHierarchy = hierarchyMapper.selectOne(rootWrapper, false);
            if (rootHierarchy == null) {
                return Map.of("offlineStats", new ArrayList<>());
            }
            hierarchyId = rootHierarchy.getId();
        }

        // 统计离线情况分组数据（按子层级统计）
        List<Map<String, Object>> offlineStats = getHierarchyOfflineStatistics(hierarchyId, countType);
        result.put("offlineStats", offlineStats);

        return result;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 获取直接子层级列表
     *
     * @param hierarchyId 层级ID
     * @return 子层级列表
     */
    private List<Hierarchy> getChildHierarchies(Long hierarchyId) {
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Hierarchy::getIdParent, hierarchyId);
        wrapper.orderByAsc(Hierarchy::getId);
        return hierarchyMapper.selectList(wrapper);
    }

    /**
     * 获取指定层级及其所有子层级的ID列表（递归）
     *
     * @param hierarchyId 层级ID
     * @return 层级ID列表，包含当前层级和所有子层级
     */
    private List<Long> getAllHierarchyIds(Long hierarchyId) {
        List<Long> allIds = new ArrayList<>();

        // 添加当前层级ID
        allIds.add(hierarchyId);

        // 递归获取所有子层级ID
        getChildHierarchyIds(hierarchyId, allIds);

        return allIds;
    }

    /**
     * 递归获取子层级ID
     *
     * @param parentId 父层级ID
     * @param allIds 用于收集所有层级ID的列表
     */
    private void getChildHierarchyIds(Long parentId, List<Long> allIds) {
        // 查询直接子层级
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.select(Hierarchy::getId);
        wrapper.eq(Hierarchy::getIdParent, parentId);

        List<Hierarchy> children = hierarchyMapper.selectList(wrapper);

        for (Hierarchy child : children) {
            allIds.add(child.getId());
            // 递归查询子层级的子层级
            getChildHierarchyIds(child.getId(), allIds);
        }
    }

    /**
     * 获取电压等级电厂统计
     *
     * @param hierarchyId 层级ID
     * @return 按电压等级分组的电厂统计
     */
    private List<Map<String, Object>> getVoltageLevelPlantStatistics(Long hierarchyId, List<Hierarchy> childHierarchies) {
        // 2. 计算每个电厂的最高电压等级
        Map<String, Long> voltageLevelCount = new HashMap<>();

        for (Hierarchy child : childHierarchies) {
            String maxVoltageLevel = getMaxVoltageLevelForPlant(child.getId());
            if (StringUtils.isNotBlank(maxVoltageLevel)) {
                voltageLevelCount.put(maxVoltageLevel, voltageLevelCount.getOrDefault(maxVoltageLevel, 0L) + 1);
            }
        }

        // 3. 转换为期望的数据格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : voltageLevelCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }

        return result;
    }

    /**
     * 获取单个电厂的最高电压等级
     *
     * @param plantHierarchyId 电厂层级ID
     * @return 最高电压等级
     */
    private String getMaxVoltageLevelForPlant(Long plantHierarchyId) {
        // 获取该电厂及其所有子层级的ID列表
        List<Long> hierarchyIds = getAllHierarchyIds(plantHierarchyId);

        if (hierarchyIds.isEmpty()) {
            return null;
        }

        // 构建设备查询条件
        LambdaQueryWrapper<Equipment> wrapper = Wrappers.lambdaQuery();
        wrapper.select(Equipment::getVoltageLevel);
        wrapper.in(Equipment::getHierarchyId, hierarchyIds);
        wrapper.isNotNull(Equipment::getVoltageLevel);
        wrapper.ne(Equipment::getVoltageLevel, "");

        List<Equipment> equipments = equipmentMapper.selectList(wrapper);

        if (equipments.isEmpty()) {
            return null;
        }

        // 定义电压等级排序（越高级别越大）
        Map<String, Integer> voltageLevelRank = new HashMap<>();
        voltageLevelRank.put("0.4kV", 1);
        voltageLevelRank.put("6kV", 2);
        voltageLevelRank.put("10kV", 3);
        voltageLevelRank.put("35kV", 4);
        voltageLevelRank.put("110kV", 5);
        voltageLevelRank.put("220kV", 6);
        voltageLevelRank.put("500kV", 7);
        voltageLevelRank.put("800kV", 8);

        String maxVoltageLevel = null;
        int maxRank = 0;

        for (Equipment equipment : equipments) {
            String voltageLevel = equipment.getVoltageLevel();
            if (StringUtils.isNotBlank(voltageLevel)) {
                int rank = voltageLevelRank.getOrDefault(voltageLevel, 0);
                if (rank > maxRank) {
                    maxRank = rank;
                    maxVoltageLevel = voltageLevel;
                }
            }
        }

        return maxVoltageLevel;
    }

    /**
     * 获取设备大类统计
     *
     * @param hierarchyId 层级ID
     * @return 设备大类统计列表
     */
    private List<Map<String, Object>> getDutMajorStatistics(Long hierarchyId) {
        Map<Integer, Long> dutMajorStats = new HashMap<>();
        Map<Integer, List<Long>> dutMajorEquipmentIds = new HashMap<>();

        // 获取层级及其所有子层级的ID列表
        List<Long> hierarchyIds = getAllHierarchyIds(hierarchyId);

        if (hierarchyIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建设备查询条件，获取设备ID和设备类型
        LambdaQueryWrapper<Equipment> wrapper = Wrappers.lambdaQuery();
        wrapper.select(Equipment::getId, Equipment::getDutMajor);
        wrapper.in(Equipment::getHierarchyId, hierarchyIds);
        wrapper.isNotNull(Equipment::getDutMajor);

        List<Equipment> equipments = equipmentMapper.selectList(wrapper);

        if (equipments.isEmpty()) {
            return new ArrayList<>();
        }

        // 按设备类型分组设备ID，并统计数量
        for (Equipment equipment : equipments) {
            Integer dutMajor = equipment.getDutMajor();
            if (dutMajor != null) {
                dutMajorStats.put(dutMajor, dutMajorStats.getOrDefault(dutMajor, 0L) + 1);

                // 按设备类型收集设备ID
                if (!dutMajorEquipmentIds.containsKey(dutMajor)) {
                    dutMajorEquipmentIds.put(dutMajor, new ArrayList<>());
                }
                dutMajorEquipmentIds.get(dutMajor).add(equipment.getId());
            }
        }

        // 转换为期望的数据格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : dutMajorStats.entrySet()) {
            Integer dutMajor = entry.getKey();
            Map<String, Object> item = new HashMap<>();
            item.put("name", EquipmentDutEnum.getByCode(dutMajor).getName());
            item.put("count", entry.getValue());

            // 获取该设备类型下的报警统计
            List<Long> equipmentIds = dutMajorEquipmentIds.get(dutMajor);
            if (equipmentIds != null && !equipmentIds.isEmpty()) {
                List<Map<String, Object>> alarmStats = getAlarmStatsByEquipmentIds(equipmentIds);
                item.put("alarmStats", alarmStats);
            } else {
                item.put("alarmStats", new ArrayList<>());
            }

            result.add(item);
        }

        return result;
    }

    /**
     * 根据设备ID列表获取报警统计信息
     *
     * @param equipmentIds 设备ID列表
     * @return 报警统计列表
     */
    private List<Map<String, Object>> getAlarmStatsByEquipmentIds(List<Long> equipmentIds) {
        List<Map<String, Object>> alarmStats = new ArrayList<>();

        if (equipmentIds.isEmpty()) {
            return alarmStats;
        }

        // 1. 获取这些设备下的所有测点
        LambdaQueryWrapper<Testpoint> testpointWrapper = Wrappers.lambdaQuery();
        testpointWrapper.select(Testpoint::getId, Testpoint::getKksCode);
        testpointWrapper.in(Testpoint::getEquipmentId, equipmentIds);
        List<Testpoint> testpoints = testpointMapper.selectList(testpointWrapper);

        if (testpoints.isEmpty()) {
            return alarmStats;
        }

        List<String> kksCodes = testpoints.stream().map(Testpoint::getKksCode).toList();

        // 2. 查询这些测点的最新报警数据
        LambdaQueryWrapper<TestpointData> dataWrapper = Wrappers.lambdaQuery();
        dataWrapper.select(TestpointData::getKksCode, TestpointData::getAlarmType, TestpointData::getAcquisitionTime);
        dataWrapper.in(TestpointData::getKksCode, kksCodes);
        dataWrapper.isNotNull(TestpointData::getAlarmType);
        dataWrapper.ne(TestpointData::getAlarmType, 0); // 排除正常状态
        dataWrapper.orderByDesc(TestpointData::getAcquisitionTime);

        List<TestpointData> alarmDataList = testpointDataMapper.selectList(dataWrapper);

        // 3. 按报警等级统计数量
        Map<Integer, Long> alarmTypeCount = new HashMap<>();
        Map<String, TestpointData> latestAlarmByKks = new HashMap<>();

        // 获取每个KKS码的最新报警记录
        for (TestpointData alarmData : alarmDataList) {
            String kksCode = alarmData.getKksCode();
            if (!latestAlarmByKks.containsKey(kksCode) ||
                alarmData.getAcquisitionTime().isAfter(latestAlarmByKks.get(kksCode).getAcquisitionTime())) {
                latestAlarmByKks.put(kksCode, alarmData);
            }
        }

        // 统计最新报警的类型数量
        for (TestpointData alarmData : latestAlarmByKks.values()) {
            Integer alarmType = alarmData.getAlarmType();
            if (alarmType != null) {
                alarmTypeCount.put(alarmType, alarmTypeCount.getOrDefault(alarmType, 0L) + 1);
            }
        }

        // 4. 转换为期望的数据格式
        for (Map.Entry<Integer, Long> entry : alarmTypeCount.entrySet()) {
            Map<String, Object> alarmItem = new HashMap<>();
            alarmItem.put("alarmType", entry.getKey());
            alarmItem.put("count", entry.getValue());

            AlarmTypeEnum alarmTypeEnum = AlarmTypeEnum.getByCode(entry.getKey());
            String alarmTypeName = alarmTypeEnum != null ? alarmTypeEnum.getName() : "未知报警类型";
            alarmItem.put("alarmTypeName", alarmTypeName);

            alarmStats.add(alarmItem);
        }

        return alarmStats;
    }

    /**
     * 获取测点类型统计
     *
     * @param hierarchyId 层级ID
     * @return 测点类型统计列表
     */
    private List<Map<String, Object>> getTestpointTypeStatistics(Long hierarchyId) {
        Map<Integer, Long> typeStats = new HashMap<>();

        // 1. 获取层级及其所有子层级的ID列表
        List<Long> hierarchyIds = getAllHierarchyIds(hierarchyId);

        if (hierarchyIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询这些层级下的所有设备ID
        LambdaQueryWrapper<Equipment> equipmentWrapper = Wrappers.lambdaQuery();
        equipmentWrapper.select(Equipment::getId);
        equipmentWrapper.in(Equipment::getHierarchyId, hierarchyIds);
        List<Equipment> equipments = equipmentMapper.selectList(equipmentWrapper);

        if (equipments.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> equipmentIds = equipments.stream().map(Equipment::getId).toList();

        // 3. 查询这些设备下的测点
        LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
        wrapper.select(Testpoint::getType);
        wrapper.in(Testpoint::getEquipmentId, equipmentIds);
        wrapper.isNotNull(Testpoint::getType);

        List<Testpoint> testpoints = testpointMapper.selectList(wrapper);

        // 4. 统计每个测点类型的数量
        for (Testpoint testpoint : testpoints) {
            Integer type = testpoint.getType();
            if (type != null) {
                typeStats.put(type, typeStats.getOrDefault(type, 0L) + 1);
            }
        }

        // 5. 转换为期望的数据格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : typeStats.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            // 使用 TestpointTypeEnum 转换类型名称
            TestpointTypeEnum typeEnum = TestpointTypeEnum.getByCode(entry.getKey());
            String typeName = typeEnum != null ? typeEnum.getName() : "未知类型";
            item.put("name", typeName);
            item.put("count", entry.getValue());
            result.add(item);
        }

        return result;
    }

    /**
     * 获取层级报警统计（按子层级统计所有报警等级）
     *
     * @param hierarchyId 层级ID
     * @return 报警统计列表
     */
    private List<Map<String, Object>> getHierarchyAlarmStatistics(Long hierarchyId, StatisticsCountTypeEnum countType) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 获取直接子层级列表
        List<Hierarchy> childHierarchies = getChildHierarchies(hierarchyId);

        if (childHierarchies.isEmpty()) {
            return result;
        }

        // 2. 对每个子层级统计其所有报警等级
        for (Hierarchy childHierarchy : childHierarchies) {
            Map<Integer, Long> alarmStats = getAllAlarmLevelsForHierarchy(childHierarchy.getId());

            // 根据统计类型计算总数
            Long totalCount = getTotalCountForHierarchy(childHierarchy.getId(), countType);

            // 为该子层级创建一个item，包含name和alarmDetails列表
            Map<String, Object> hierarchyItem = new HashMap<>();
            // 优先使用show_name，如果为空则使用name
            String displayName = getDisplayName(childHierarchy.getShowName(), childHierarchy.getName());
            hierarchyItem.put("name", displayName);
            hierarchyItem.put("totalCount", totalCount);

            // 创建该子层级的报警详情列表
            List<Map<String, Object>> alarmDetails = new ArrayList<>();
            for (Map.Entry<Integer, Long> entry : alarmStats.entrySet()) {
                Map<String, Object> alarmItem = new HashMap<>();

                // 使用 AlarmTypeEnum 转换报警类型名称
                AlarmTypeEnum alarmTypeEnum = AlarmTypeEnum.getByCode(entry.getKey());
                String alarmTypeName = alarmTypeEnum != null ? alarmTypeEnum.getName() : "未知报警类型";
                alarmItem.put("alarmTypeName", alarmTypeName);
                alarmItem.put("alarmLevel", entry.getKey());
                alarmItem.put("count", entry.getValue());

                alarmDetails.add(alarmItem);
            }

            hierarchyItem.put("alarmDetails", alarmDetails);
            result.add(hierarchyItem);
        }

        return result;
    }

    /**
     * 获取指定层级下设备的所有报警等级统计
     *
     * @param hierarchyId 层级ID
     * @return 报警等级统计Map（key=报警等级，value=数量）
     */
    private Map<Integer, Long> getAllAlarmLevelsForHierarchy(Long hierarchyId) {
        Map<Integer, Long> alarmStats = new HashMap<>();

        // 1. 获取层级及其所有子层级的ID列表（递归）
        List<Long> hierarchyIds = getAllHierarchyIds(hierarchyId);

        if (hierarchyIds.isEmpty()) {
            return alarmStats;
        }

        // 2. 查询这些层级下的所有设备ID
        LambdaQueryWrapper<Equipment> equipmentWrapper = Wrappers.lambdaQuery();
        equipmentWrapper.select(Equipment::getId);
        equipmentWrapper.in(Equipment::getHierarchyId, hierarchyIds);
        List<Equipment> equipments = equipmentMapper.selectList(equipmentWrapper);

        if (equipments.isEmpty()) {
            return alarmStats;
        }

        List<Long> equipmentIds = equipments.stream().map(Equipment::getId).toList();

        // 3. 查询这些设备下的测点报警信息，统计每个报警等级的数量
        LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
        wrapper.select(Testpoint::getLastAlarmType);
        wrapper.in(Testpoint::getEquipmentId, equipmentIds);
        wrapper.isNotNull(Testpoint::getLastAlarmType);

        List<Testpoint> testpoints = testpointMapper.selectList(wrapper);

        // 4. 统计每个报警等级的数量
        for (Testpoint testpoint : testpoints) {
            Integer alarmLevel = testpoint.getLastAlarmType();
            if (alarmLevel != null) {
                alarmStats.put(alarmLevel, alarmStats.getOrDefault(alarmLevel, 0L) + 1);
            }
        }

        return alarmStats;
    }



    /**
     * 获取层级离线统计
     *
     * @param hierarchyId 层级ID
     * @return 离线统计列表
     */
    private List<Map<String, Object>> getHierarchyOfflineStatistics(Long hierarchyId, StatisticsCountTypeEnum countType) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 获取直接子层级列表
        List<Hierarchy> childHierarchies = getChildHierarchies(hierarchyId);

        if (childHierarchies.isEmpty()) {
            return result;
        }

        // 2. 对每个子层级统计离线情况
        for (Hierarchy childHierarchy : childHierarchies) {
            // 根据统计类型获取离线信息
            Map<String, Object> offlineInfo = getOfflineInfoForHierarchy(childHierarchy.getId(), countType);

            // 为该子层级创建一个item
            Map<String, Object> hierarchyItem = new HashMap<>();
            // 优先使用show_name，如果为空则使用name
            String displayName = getDisplayName(childHierarchy.getShowName(), childHierarchy.getName());
            hierarchyItem.put("name", displayName);
            hierarchyItem.put("totalCount", offlineInfo.get("totalCount"));
            hierarchyItem.put("offlineCount", offlineInfo.get("offlineCount"));

            result.add(hierarchyItem);
        }

        return result;
    }

    /**
     * 获取指定层级下设备的离线统计信息
     *
     * @param hierarchyId 层级ID
     * @param countType 统计数量类型
     * @return 包含总数量和离线数量的Map
     */
    private Map<String, Object> getOfflineInfoForHierarchy(Long hierarchyId, StatisticsCountTypeEnum countType) {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取层级及其所有子层级的ID列表（递归）
        List<Long> hierarchyIds = getAllHierarchyIds(hierarchyId);

        if (hierarchyIds.isEmpty()) {
            result.put("totalCount", 0L);
            result.put("offlineCount", 0L);
            return result;
        }

        if (countType == StatisticsCountTypeEnum.EQUIPMENT) {
            // 统计设备数量
            // 2. 查询这些层级下的所有设备ID
            LambdaQueryWrapper<Equipment> equipmentWrapper = Wrappers.lambdaQuery();
            equipmentWrapper.select(Equipment::getId);
            equipmentWrapper.in(Equipment::getHierarchyId, hierarchyIds);
            List<Equipment> equipments = equipmentMapper.selectList(equipmentWrapper);

            if (equipments.isEmpty()) {
                result.put("totalCount", 0L);
                result.put("offlineCount", 0L);
                return result;
            }

            List<Long> equipmentIds = equipments.stream().map(Equipment::getId).toList();
            Long totalEquipmentCount = (long) equipmentIds.size();

            // 3. 查询这些设备下的当前离线测点，统计离线设备数量
            LambdaQueryWrapper<TestpointOffline> offlineWrapper = Wrappers.lambdaQuery();
            offlineWrapper.select(TestpointOffline::getEquipmentId);
            offlineWrapper.in(TestpointOffline::getEquipmentId, equipmentIds);
            offlineWrapper.eq(TestpointOffline::getStatus, 1); // 1-离线中
            offlineWrapper.groupBy(TestpointOffline::getEquipmentId);

            List<TestpointOffline> offlineRecords = testpointOfflineMapper.selectList(offlineWrapper);
            Long offlineEquipmentCount = (long) offlineRecords.size();

            result.put("totalCount", totalEquipmentCount);
            result.put("offlineCount", offlineEquipmentCount);
        } else if (countType == StatisticsCountTypeEnum.TESTPOINT) {
            // 统计测点数量
            // 2. 查询这些层级下的所有设备ID
            LambdaQueryWrapper<Equipment> equipmentWrapper = Wrappers.lambdaQuery();
            equipmentWrapper.select(Equipment::getId);
            equipmentWrapper.in(Equipment::getHierarchyId, hierarchyIds);
            List<Equipment> equipments = equipmentMapper.selectList(equipmentWrapper);

            if (equipments.isEmpty()) {
                result.put("totalCount", 0L);
                result.put("offlineCount", 0L);
                return result;
            }

            List<Long> equipmentIds = equipments.stream().map(Equipment::getId).toList();

            // 3. 查询这些设备下的所有测点数量
            LambdaQueryWrapper<Testpoint> testpointWrapper = Wrappers.lambdaQuery();
            testpointWrapper.in(Testpoint::getEquipmentId, equipmentIds);
            Long totalTestpointCount = testpointMapper.selectCount(testpointWrapper);

            // 4. 查询这些设备下的当前离线测点数量
            LambdaQueryWrapper<TestpointOffline> offlineWrapper = Wrappers.lambdaQuery();
            offlineWrapper.select(TestpointOffline::getEquipmentId);
            offlineWrapper.in(TestpointOffline::getEquipmentId, equipmentIds);
            offlineWrapper.eq(TestpointOffline::getStatus, 1); // 1-离线中
            Long offlineTestpointCount = testpointOfflineMapper.selectCount(offlineWrapper);

            result.put("totalCount", totalTestpointCount);
            result.put("offlineCount", offlineTestpointCount);
        }

        return result;
    }

    /**
     * 根据统计类型获取指定层级的总数
     *
     * @param hierarchyId 层级ID
     * @param countType 统计数量类型
     * @return 总数
     */
    private Long getTotalCountForHierarchy(Long hierarchyId, StatisticsCountTypeEnum countType) {
        // 1. 获取层级及其所有子层级的ID列表（递归）
        List<Long> hierarchyIds = getAllHierarchyIds(hierarchyId);

        if (hierarchyIds.isEmpty()) {
            return 0L;
        }

        if (countType == StatisticsCountTypeEnum.EQUIPMENT) {
            // 统计设备数量
            LambdaQueryWrapper<Equipment> equipmentWrapper = Wrappers.lambdaQuery();
            equipmentWrapper.in(Equipment::getHierarchyId, hierarchyIds);
            return equipmentMapper.selectCount(equipmentWrapper);
        } else if (countType == StatisticsCountTypeEnum.TESTPOINT) {
            // 统计测点数量
            // 2. 查询这些层级下的所有设备ID
            LambdaQueryWrapper<Equipment> equipmentWrapper = Wrappers.lambdaQuery();
            equipmentWrapper.select(Equipment::getId);
            equipmentWrapper.in(Equipment::getHierarchyId, hierarchyIds);
            List<Equipment> equipments = equipmentMapper.selectList(equipmentWrapper);

            if (equipments.isEmpty()) {
                return 0L;
            }

            List<Long> equipmentIds = equipments.stream().map(Equipment::getId).toList();

            // 3. 统计这些设备下的测点数量
            LambdaQueryWrapper<Testpoint> testpointWrapper = Wrappers.lambdaQuery();
            testpointWrapper.in(Testpoint::getEquipmentId, equipmentIds);

            return testpointMapper.selectCount(testpointWrapper);
        }

        return 0L;
    }

    /**
     * 获取显示名称，优先使用 showName，如果为空则使用 name
     *
     * @param showName 显示名称
     * @param name 默认名称
     * @return 最终显示名称
     */
    private String getDisplayName(String showName, String name) {
        return StringUtils.isNotBlank(showName) ? showName : name;
    }

    @Override
    public Map<String, Object> getRealtimeAlarmList(Long hierarchyId, List<Integer> alarmTypes, Integer minutesAgo) {
        Map<String, Object> result = new HashMap<>();

        // 1. 参数验证和默认值设置
        if (hierarchyId == null) {
            LambdaQueryWrapper<Hierarchy> rootWrapper = Wrappers.lambdaQuery();
            rootWrapper.eq(Hierarchy::getType, 0);
            Hierarchy rootHierarchy = hierarchyMapper.selectOne(rootWrapper, false);
            if (rootHierarchy == null) {
                result.put("alarmList", new ArrayList<>());
                result.put("totalCount", 0);
                return result;
            }
            hierarchyId = rootHierarchy.getId();
        }

        if (minutesAgo == null || minutesAgo <= 0) {
            minutesAgo = 60; // 默认查询1小时内的数据
        }

        // 2. 获取层级及其所有子层级的ID列表
        List<Long> hierarchyIds = getAllHierarchyIds(hierarchyId);

        if (hierarchyIds.isEmpty()) {
            result.put("alarmList", new ArrayList<>());
            result.put("totalCount", 0);
            return result;
        }

        // 3. 查询这些层级下的所有设备ID
        LambdaQueryWrapper<Equipment> equipmentWrapper = Wrappers.lambdaQuery();
        equipmentWrapper.select(Equipment::getId, Equipment::getName, Equipment::getHierarchyId);
        equipmentWrapper.in(Equipment::getHierarchyId, hierarchyIds);
        List<Equipment> equipments = equipmentMapper.selectList(equipmentWrapper);

        if (equipments.isEmpty()) {
            result.put("alarmList", new ArrayList<>());
            result.put("totalCount", 0);
            return result;
        }

        List<Long> equipmentIds = equipments.stream().map(Equipment::getId).toList();

        // 4. 查询这些设备下的所有测点ID
        LambdaQueryWrapper<Testpoint> testpointWrapper = Wrappers.lambdaQuery();
        testpointWrapper.select(Testpoint::getId, Testpoint::getKksCode, Testpoint::getKksName, Testpoint::getEquipmentId);
        testpointWrapper.in(Testpoint::getEquipmentId, equipmentIds);
        List<Testpoint> testpoints = testpointMapper.selectList(testpointWrapper);

        if (testpoints.isEmpty()) {
            result.put("alarmList", new ArrayList<>());
            result.put("totalCount", 0);
            return result;
        }

        List<String> kksCodes = testpoints.stream().map(Testpoint::getKksCode).toList();

        // 5. 构建测点数据查询条件
        LambdaQueryWrapper<TestpointData> dataWrapper = Wrappers.lambdaQuery();
        dataWrapper.in(TestpointData::getKksCode, kksCodes);
        dataWrapper.ge(TestpointData::getAcquisitionTime, java.time.LocalDateTime.now().minusMinutes(minutesAgo));

        // 6. 报警类型过滤
        if (alarmTypes != null && !alarmTypes.isEmpty()) {
            dataWrapper.in(TestpointData::getAlarmType, alarmTypes);
        } else {
            // 默认查询所有非0的报警状态（0表示正常）
            dataWrapper.ne(TestpointData::getAlarmType, 0);
            dataWrapper.isNotNull(TestpointData::getAlarmType);
        }

        dataWrapper.orderByDesc(TestpointData::getAcquisitionTime);
        List<TestpointData> alarmDataList = testpointDataMapper.selectList(dataWrapper);

        // 7. 组装返回数据
        List<Map<String, Object>> alarmList = new ArrayList<>();

        // 创建设备和层级的映射
        Map<Long, Equipment> equipmentMap = equipments.stream()
            .collect(java.util.stream.Collectors.toMap(Equipment::getId, equipment -> equipment));

        Map<Long, Hierarchy> hierarchyMap = new HashMap<>();
        for (Long hid : hierarchyIds) {
            LambdaQueryWrapper<Hierarchy> hWrapper = Wrappers.lambdaQuery();
            hWrapper.eq(Hierarchy::getId, hid);
            Hierarchy hierarchy = hierarchyMapper.selectOne(hWrapper);
            if (hierarchy != null) {
                hierarchyMap.put(hid, hierarchy);
            }
        }

        Map<String, Testpoint> testpointMap = testpoints.stream()
            .collect(java.util.stream.Collectors.toMap(Testpoint::getKksCode, testpoint -> testpoint));

        for (TestpointData alarmData : alarmDataList) {
            Map<String, Object> alarmItem = new HashMap<>();

            // 获取测点信息
            Testpoint testpoint = testpointMap.get(alarmData.getKksCode());
            if (testpoint != null) {
                // 获取设备信息
                Equipment equipment = equipmentMap.get(testpoint.getEquipmentId());
                if (equipment != null) {
                    // 获取层级信息
                    Hierarchy hierarchy = hierarchyMap.get(equipment.getHierarchyId());
                    if (hierarchy != null) {
                        alarmItem.put("hierarchyName", getDisplayName(hierarchy.getShowName(), hierarchy.getName()));
                        alarmItem.put("hierarchyId", hierarchy.getId());
                    }

                    alarmItem.put("equipmentName", equipment.getName());
                    alarmItem.put("equipmentId", equipment.getId());
                }

                alarmItem.put("testpointName", getDisplayName(testpoint.getShowName(), testpoint.getKksName()));
                alarmItem.put("kksCode", testpoint.getKksCode());
                alarmItem.put("testpointId", testpoint.getId());
            }

            alarmItem.put("alarmType", alarmData.getAlarmType());
            alarmItem.put("alarmTime", alarmData.getAcquisitionTime());
            alarmItem.put("alarmTypeName", AlarmTypeEnum.getByCode(alarmData.getAlarmType()) != null ?
                AlarmTypeEnum.getByCode(alarmData.getAlarmType()).getName() : "未知报警");

            alarmList.add(alarmItem);
        }

        result.put("alarmList", alarmList);
        result.put("totalCount", alarmList.size());

        return result;
    }
}
