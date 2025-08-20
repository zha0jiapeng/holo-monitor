package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.constant.Tag;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.bo.EquipmentBo;
import org.dromara.hm.domain.vo.EquipmentVo;
import org.dromara.hm.enums.EquipmentDutEnum;
import org.dromara.hm.mapper.EquipmentMapper;
import org.dromara.hm.mapper.HierarchyMapper;
import org.dromara.hm.service.IEquipmentService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 设备Service业务层处理
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class EquipmentServiceImpl implements IEquipmentService {

    private final EquipmentMapper baseMapper;
    private final HierarchyMapper hierarchyMapper;

    @Override
    public EquipmentVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<EquipmentVo> queryPageList(EquipmentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Equipment> lqw = buildQueryWrapper(bo);
        Page<EquipmentVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<EquipmentVo> customPageList(EquipmentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Equipment> lqw = buildQueryWrapper(bo);
        Page<EquipmentVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<EquipmentVo> queryList(EquipmentBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<Equipment> buildQueryWrapper(EquipmentBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<Equipment> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getUniqueKey()), Equipment::getUniqueKey, bo.getUniqueKey());
        lqw.eq(bo.getHierarchyId() != null, Equipment::getHierarchyId, bo.getHierarchyId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), Equipment::getName, bo.getName());
        lqw.like(StringUtils.isNotBlank(bo.getDesc()), Equipment::getDesc, bo.getDesc());
        lqw.eq(bo.getLat() != null, Equipment::getLat, bo.getLat());
        lqw.eq(bo.getLng() != null, Equipment::getLng, bo.getLng());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            Equipment::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByAsc(Equipment::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(EquipmentBo bo) {
        Equipment add = MapstructUtils.convert(bo, Equipment.class);
        if (add != null) {
            validEntityBeforeSave(add);
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            if (add != null) {
                bo.setId(add.getId());
            }
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(EquipmentBo bo) {
        Equipment update = MapstructUtils.convert(bo, Equipment.class);
        if (update != null) {
            validEntityBeforeSave(update);
        }
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(Equipment entity) {
        // 校验唯一键不能重复
        if (StringUtils.isNotBlank(entity.getUniqueKey())) {
            LambdaQueryWrapper<Equipment> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(Equipment::getUniqueKey, entity.getUniqueKey());
            if (entity.getId() != null) {
                wrapper.ne(Equipment::getId, entity.getId());
            }
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("设备唯一键已存在");
            }
        }

        // 校验层级是否存在
        if (entity.getHierarchyId() != null) {
            // 这里应该检查hierarchy表中是否存在该层级
            // 暂时注释掉，实际使用时需要注入HierarchyService进行验证
            // Hierarchy hierarchy = hierarchyService.queryById(entity.getHierarchyId());
            // if (hierarchy == null) {
            //     throw new ServiceException("层级不存在");
            // }
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 检查是否有关联到该设备的其他设备，如果有则不能删除
            // 注意：现在设备关联的是层级而不是其他设备，所以这个检查可能不再需要
            // 如果需要，应该检查是否有设备关联到要删除设备所在的层级

            // 校验删除权限
            List<Equipment> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("您没有删除权限!");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<Equipment> list) {
        return baseMapper.insertBatch(list);
    }

    @Override
    public Boolean importFromJson(String jsonData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null || !dataNode.isArray()) {
                throw new ServiceException("JSON格式错误：缺少data数组");
            }

            // 获取当前数据库中的所有设备，按id建立映射
            List<Equipment> existingEquipments = baseMapper.selectList(null);
            Map<Long, Equipment> existingIdMap = existingEquipments.stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(Equipment::getId, e -> e, (e1, e2) -> e1));

            // 解析SD400MP数据
            List<Equipment> sdEquipmentList = new ArrayList<>();
            Map<Long, Long> sdParentIdMap = new HashMap<>(); // 存储SD400MP中的父子关系

            // 第一轮：解析所有设备数据
            for (JsonNode equipmentNode : dataNode) {
                Equipment equipment = parseEquipmentFromJson(equipmentNode);
                if (equipment != null && equipment.getId() != null) {
                    sdEquipmentList.add(equipment);
                    // 记录原始的父级关系（SD400MP ID）
                    if (equipmentNode.has("idParent")) {
                        sdParentIdMap.put(equipment.getId(), equipmentNode.get("idParent").asLong());
                    }
                }
            }

            // 第二轮：处理所有设备的新增和更新（暂时不处理父级关系）
            for (Equipment equipment : sdEquipmentList) {
                Equipment existing = existingIdMap.get(equipment.getId());
                if (existing != null) {
                    // 更新现有设备 - 使用update wrapper避免Sa-Token上下文问题
                    updateEquipmentBySql(equipment.getId(), equipment);
                } else {
                    // 新增设备（暂时不设置层级）
                    equipment.setHierarchyId(null);
                    baseMapper.insert(equipment);
                }
            }

            // 第三轮：更新父级关系
            for (Equipment equipment : sdEquipmentList) {
                Long sdParentId = sdParentIdMap.get(equipment.getId());
                if (sdParentId != null) {
                    // 检查父级设备是否存在
                    Equipment parentEquipment = existingIdMap.get(sdParentId);
                    if (parentEquipment != null || sdEquipmentList.stream().anyMatch(e -> e.getId().equals(sdParentId))) {
                        // 更新父级关系
                        Equipment dbEquipment = baseMapper.selectById(equipment.getId());
                        if (dbEquipment != null && !sdParentId.equals(dbEquipment.getHierarchyId())) {
                            updateParentId(equipment.getId(), sdParentId);
                        }
                    }
                }
            }

            // 第四轮：删除SD400MP中已不存在的设备
            List<Long> sdIds = sdEquipmentList.stream()
                .map(Equipment::getId)
                .toList();

            List<Long> toDeleteIds = existingEquipments.stream()
                .filter(e -> e.getId() != null && !sdIds.contains(e.getId()))
                .map(Equipment::getId)
                .toList();

            if (!toDeleteIds.isEmpty()) {
                // 检查要删除的设备是否有子设备，如果有则一并删除
                List<Long> allDeleteIds = new ArrayList<>(toDeleteIds);
                findAndAddChildIds(allDeleteIds, existingEquipments);
                baseMapper.deleteByIds(allDeleteIds);
            }

            return true;
        } catch (Exception e) {
            throw new ServiceException("设备同步失败：" + e.getMessage());
        }
    }

    /**
     * 使用SQL更新设备信息
     */
    private void updateEquipmentBySql(Long equipmentId, Equipment newData) {
        LambdaUpdateWrapper<Equipment> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(Equipment::getId, equipmentId)
            .set(Equipment::getUniqueKey, newData.getUniqueKey())
            .set(Equipment::getName, newData.getName())
            .set(Equipment::getDesc, newData.getDesc())
            .set(Equipment::getSettings, newData.getSettings())
            .set(Equipment::getLat, newData.getLat())
            .set(Equipment::getLng, newData.getLng())
            .set(Equipment::getDut, newData.getDut())
            .set(Equipment::getDutMajor, newData.getDutMajor());

        baseMapper.update(null, updateWrapper);
    }

    /**
     * 更新父级ID
     */
    private void updateParentId(Long equipmentId, Long parentId) {
        LambdaUpdateWrapper<Equipment> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(Equipment::getId, equipmentId)
            .set(Equipment::getHierarchyId, parentId);

        baseMapper.update(null, updateWrapper);
    }

    /**
     * 查找并添加子设备ID（递归删除）
     */
    private void findAndAddChildIds(List<Long> deleteIds, List<Equipment> allEquipments) {
        List<Long> currentIds = new ArrayList<>(deleteIds);
        for (Long parentId : currentIds) {
            List<Long> childIds = allEquipments.stream()
                .filter(e -> parentId.equals(e.getHierarchyId()) && !deleteIds.contains(e.getId()))
                .map(Equipment::getId)
                .toList();

            if (!childIds.isEmpty()) {
                deleteIds.addAll(childIds);
                findAndAddChildIds(deleteIds, allEquipments); // 递归查找子设备
            }
        }
    }

    /**
     * 从JSON节点解析设备对象
     */
    private Equipment parseEquipmentFromJson(JsonNode equipmentNode) {
                try {
            Equipment equipment = new Equipment();

            // 直接使用SD400MP的ID作为主键
            if (equipmentNode.has("id") && !equipmentNode.get("id").isNull()) {
                equipment.setId(equipmentNode.get("id").asLong());
            } else {
                // 如果没有ID，跳过此设备
                return null;
            }

            // 唯一键
            if (equipmentNode.has("key") && !equipmentNode.get("key").isNull()) {
                equipment.setUniqueKey(equipmentNode.get("key").asText());
            }

            // 设备名称 - 必须字段
            if (equipmentNode.has("name") && !equipmentNode.get("name").isNull()) {
                equipment.setName(equipmentNode.get("name").asText());
            } else {
                equipment.setName("未命名设备");
            }

            // 描述
            if (equipmentNode.has("desc") && !equipmentNode.get("desc").isNull()) {
                equipment.setDesc(equipmentNode.get("desc").asText());
            }

            // 设置（blob类型）- 正确处理JSON对象或字符串
            if (equipmentNode.has("settings") && !equipmentNode.get("settings").isNull()) {
                JsonNode settingsNode = equipmentNode.get("settings");
                try {
                    String settingsStr;
                    if (settingsNode.isTextual()) {
                        // 如果是字符串，直接使用
                        settingsStr = settingsNode.asText();
                    } else {
                        // 如果是JSON对象，转换为字符串
                        settingsStr = settingsNode.toString();
                    }
                    // 转换为字节数组存储
                    equipment.setSettings(settingsStr.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    // 如果转换失败，存储原始字符串
                    equipment.setSettings(settingsNode.toString().getBytes(StandardCharsets.UTF_8));
                }
            }

            // 经纬度
            if (equipmentNode.has("lat") && !equipmentNode.get("lat").isNull()) {
                double lat = equipmentNode.get("lat").asDouble();
                equipment.setLat(lat != 0 ? lat : null);
            }

            if (equipmentNode.has("lng") && !equipmentNode.get("lng").isNull()) {
                double lng = equipmentNode.get("lng").asDouble();
                equipment.setLng(lng != 0 ? lng : null);
            }

            if (equipmentNode.has("settings") && !equipmentNode.get("settings").isNull()) {
                JsonNode settings = equipmentNode.get("settings");
                if (settings.has("params") && !settings.get("params").isNull()) {
                    JsonNode params = settings.get("params");
                    for (JsonNode param : params) {
                        String key = param.get("key").asText();
                        if (Tag.DUT.equals(key)) {
                            String val = param.get("val").asText();
                            equipment.setDut(Integer.parseInt(val));
                            equipment.setDutMajor(EquipmentDutEnum.getByDutValue(equipment.getDut()).getCode());
                            break;
                        }
                    }
                }
            }

            // 注意：idParent在这里不设置，在调用方法中单独处理

            return equipment;
        } catch (Exception e) {
            throw new ServiceException("解析设备数据失败：" + e.getMessage());
        }
    }

    @Override
    public Boolean importEquipmentsFromJson(String jsonData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null || !dataNode.isArray()) {
                throw new ServiceException("JSON格式错误：缺少data数组");
            }

            // 获取当前数据库中的所有设备，按id建立映射
            List<Equipment> existingEquipments = baseMapper.selectList(null);
            Map<Long, Equipment> existingIdMap = existingEquipments.stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(Equipment::getId, e -> e, (e1, e2) -> e1));

            // 解析设备数据（此时数据已经过滤，只包含设备）
            List<Equipment> newEquipments = new ArrayList<>();
            Map<Long, Long> hierarchyRelations = new HashMap<>();

            // 第一轮：解析设备数据
            for (JsonNode itemNode : dataNode) {
                Equipment equipment = parseEquipmentFromJsonNew(itemNode);
                if (equipment != null && equipment.getId() != null) {
                    newEquipments.add(equipment);
                    // 记录层级关系
                    if (itemNode.has("idParent") && !itemNode.get("idParent").isNull()) {
                        hierarchyRelations.put(equipment.getId(), itemNode.get("idParent").asLong());
                    }
                }
            }

            // 第二轮：新增和更新设备（直接设置层级关系）
            for (Equipment equipment : newEquipments) {
                // 设置层级关系 - type=2的设备一定有层级
                equipment.setHierarchyId(hierarchyRelations.get(equipment.getId()));

                Equipment existing = existingIdMap.get(equipment.getId());
                if (existing != null) {
                    // 更新现有设备
                    equipment.setCreateBy(existing.getCreateBy());
                    equipment.setCreateTime(existing.getCreateTime());
                    equipment.setCreateDept(existing.getCreateDept());
                    equipment.setTenantId(existing.getTenantId());
                    updateEquipmentBySqlNew(equipment.getId(), equipment);
                } else {
                    // 新增设备
                    baseMapper.insert(equipment);
                }
            }

            // 第四轮：删除不存在的设备
            List<Long> newIds = newEquipments.stream().map(Equipment::getId).toList();
            List<Long> toDeleteIds = existingEquipments.stream()
                .filter(e -> e.getId() != null && !newIds.contains(e.getId()))
                .map(Equipment::getId)
                .toList();

            if (!toDeleteIds.isEmpty()) {
                baseMapper.deleteByIds(toDeleteIds);
            }

            return true;
        } catch (Exception e) {
            throw new ServiceException("设备同步失败：" + e.getMessage());
        }
    }



    /**
     * 从JSON节点解析设备对象（新版本，使用hierarchyId）
     */
    private Equipment parseEquipmentFromJsonNew(JsonNode equipmentNode) {
        try {
            Equipment equipment = new Equipment();

            // 直接使用SD400MP的ID作为主键
            if (equipmentNode.has("id") && !equipmentNode.get("id").isNull()) {
                equipment.setId(equipmentNode.get("id").asLong());
            } else {
                return null;
            }

            // 唯一键
            if (equipmentNode.has("key") && !equipmentNode.get("key").isNull()) {
                equipment.setUniqueKey(equipmentNode.get("key").asText());
            }

            // 设备名称 - 必须字段
            if (equipmentNode.has("name") && !equipmentNode.get("name").isNull()) {
                equipment.setName(equipmentNode.get("name").asText());
            } else {
                equipment.setName("未命名设备");
            }

            // 描述
            if (equipmentNode.has("desc") && !equipmentNode.get("desc").isNull()) {
                equipment.setDesc(equipmentNode.get("desc").asText());
            }

            // 设置（blob类型）
            if (equipmentNode.has("settings") && !equipmentNode.get("settings").isNull()) {
                JsonNode settingsNode = equipmentNode.get("settings");
                try {
                    String settingsStr;
                    if (settingsNode.isTextual()) {
                        settingsStr = settingsNode.asText();
                    } else {
                        settingsStr = settingsNode.toString();
                    }
                    equipment.setSettings(settingsStr.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    equipment.setSettings(settingsNode.toString().getBytes(StandardCharsets.UTF_8));
                }
            }

            // 经纬度
            if (equipmentNode.has("lat") && !equipmentNode.get("lat").isNull()) {
                double lat = equipmentNode.get("lat").asDouble();
                equipment.setLat(lat != 0 ? lat : null);
            }

            if (equipmentNode.has("lng") && !equipmentNode.get("lng").isNull()) {
                double lng = equipmentNode.get("lng").asDouble();
                equipment.setLng(lng != 0 ? lng : null);
            }

            // 处理DUT信息
            if (equipmentNode.has("settings") && !equipmentNode.get("settings").isNull()) {
                JsonNode settings = equipmentNode.get("settings");
                if (settings.has("params") && !settings.get("params").isNull()) {
                    JsonNode params = settings.get("params");
                    for (JsonNode param : params) {
                        String key = param.get("key").asText();
                        if (Tag.DUT.equals(key)) {
                            String val = param.get("val").asText();
                            equipment.setDut(Integer.parseInt(val));
                            equipment.setDutMajor(EquipmentDutEnum.getByDutValue(equipment.getDut()).getCode());
                            break;
                        }
                    }
                }
            }

            return equipment;
        } catch (Exception e) {
            throw new ServiceException("解析设备数据失败：" + e.getMessage());
        }
    }

    /**
     * 使用SQL更新设备信息（新版本，使用hierarchyId）
     */
    private void updateEquipmentBySqlNew(Long equipmentId, Equipment newData) {
        LambdaUpdateWrapper<Equipment> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(Equipment::getId, equipmentId)
            .set(Equipment::getUniqueKey, newData.getUniqueKey())
            .set(Equipment::getName, newData.getName())
            .set(Equipment::getDesc, newData.getDesc())
            .set(Equipment::getSettings, newData.getSettings())
            .set(Equipment::getLat, newData.getLat())
            .set(Equipment::getLng, newData.getLng())
            .set(Equipment::getDut, newData.getDut())
            .set(Equipment::getDutMajor, newData.getDutMajor())
            .set(Equipment::getVoltageLevel, newData.getVoltageLevel());
        baseMapper.update(updateWrapper);
    }

    @Override
    public List<Equipment> getEquipmentsByType(Integer type) {
        LambdaQueryWrapper<Equipment> wrapper = Wrappers.lambdaQuery();
        wrapper.orderByAsc(Equipment::getId);
        return baseMapper.selectList(wrapper);
    }

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
     * 获取设备大类统计
     *
     * @param hierarchyId 层级ID
     * @return 设备大类统计列表
     */
    private List<Map<String, Object>> getDutMajorStatistics(Long hierarchyId) {
        Map<Integer, Long> dutMajorStats = new HashMap<>();

        // 获取层级及其所有子层级的ID列表
        List<Long> hierarchyIds = getAllHierarchyIds(hierarchyId);

        if (hierarchyIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建设备查询条件
        LambdaQueryWrapper<Equipment> wrapper = Wrappers.lambdaQuery();
        wrapper.select(Equipment::getDutMajor);
        wrapper.in(Equipment::getHierarchyId, hierarchyIds);
        wrapper.isNotNull(Equipment::getDutMajor);

        List<Equipment> equipments = baseMapper.selectList(wrapper);

        // 统计每个设备大类的数量
        for (Equipment equipment : equipments) {
            Integer dutMajor = equipment.getDutMajor();
            if (dutMajor != null) {
                dutMajorStats.put(dutMajor, dutMajorStats.getOrDefault(dutMajor, 0L) + 1);
            }
        }

        // 转换为期望的数据格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : dutMajorStats.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", EquipmentDutEnum.getByCode(entry.getKey()).getName()); // 可以添加设备大类名称映射
            item.put("count", entry.getValue());
            item.put("code", entry.getKey());
            result.add(item);
        }

        return result;
    }

    /**
     * 获取电压等级电厂统计
     *
     * @param hierarchyId 层级ID
     * @return 按电压等级分组的电厂统计
     */
    private List<Map<String, Object>> getVoltageLevelPlantStatistics(Long hierarchyId,List<Hierarchy> childHierarchies) {


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
            item.put("value", entry.getValue());
            result.add(item);
        }

        return result;
    }

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

        List<Equipment> equipments = baseMapper.selectList(wrapper);

        if (equipments.isEmpty()) {
            return null;
        }

        // 找出最高电压等级
        String maxVoltageLevel = null;
        Double maxVoltageValue = 0.0;

        for (Equipment equipment : equipments) {
            String voltageLevel = equipment.getVoltageLevel();
            if (StringUtils.isNotBlank(voltageLevel)) {
                Double voltageValue = parseVoltageLevel(voltageLevel);
                if (voltageValue != null && voltageValue > maxVoltageValue) {
                    maxVoltageValue = voltageValue;
                    maxVoltageLevel = voltageLevel;
                }
            }
        }

        return maxVoltageLevel;
    }

    /**
     * 解析电压等级数值
     *
     * @param voltageLevel 电压等级字符串，如 "220kV", "110kV"
     * @return 电压数值
     */
    private Double parseVoltageLevel(String voltageLevel) {
        if (StringUtils.isBlank(voltageLevel)) {
            return null;
        }

        try {
            // 移除单位和空格，提取数字
            String numStr = voltageLevel.replaceAll("[^0-9.]", "");
            if (StringUtils.isNotBlank(numStr)) {
                return Double.parseDouble(numStr);
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析电压等级: {}", voltageLevel);
        }

        return null;
    }
}
