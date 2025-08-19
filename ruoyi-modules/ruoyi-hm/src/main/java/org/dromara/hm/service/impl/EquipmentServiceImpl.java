package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.domain.bo.EquipmentBo;
import org.dromara.hm.domain.vo.EquipmentVo;
import org.dromara.hm.mapper.EquipmentMapper;
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
public class EquipmentServiceImpl implements IEquipmentService {

    private final EquipmentMapper baseMapper;

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
        lqw.eq(bo.getIdParent() != null, Equipment::getIdParent, bo.getIdParent());
        lqw.like(StringUtils.isNotBlank(bo.getName()), Equipment::getName, bo.getName());
        lqw.like(StringUtils.isNotBlank(bo.getDesc()), Equipment::getDesc, bo.getDesc());
        lqw.eq(bo.getType() != null, Equipment::getType, bo.getType());
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

        // 校验父级设备是否存在
        if (entity.getIdParent() != null) {
            Equipment parent = baseMapper.selectById(entity.getIdParent());
            if (parent == null) {
                throw new ServiceException("父级设备不存在");
            }
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 检查是否有子设备，如果有则不能删除
            for (Long id : ids) {
                LambdaQueryWrapper<Equipment> wrapper = Wrappers.lambdaQuery();
                wrapper.eq(Equipment::getIdParent, id);
                if (baseMapper.exists(wrapper)) {
                    throw new ServiceException("存在子设备，无法删除");
                }
            }

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
                    // 新增设备（暂时不设置父级）
                    equipment.setIdParent(null);
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
                        if (dbEquipment != null && !sdParentId.equals(dbEquipment.getIdParent())) {
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
            .set(Equipment::getType, newData.getType())
            .set(Equipment::getSettings, newData.getSettings())
            .set(Equipment::getLat, newData.getLat())
            .set(Equipment::getLng, newData.getLng());

        baseMapper.update(null, updateWrapper);
    }

    /**
     * 更新父级ID
     */
    private void updateParentId(Long equipmentId, Long parentId) {
        LambdaUpdateWrapper<Equipment> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(Equipment::getId, equipmentId)
            .set(Equipment::getIdParent, parentId);

        baseMapper.update(null, updateWrapper);
    }

    /**
     * 查找并添加子设备ID（递归删除）
     */
    private void findAndAddChildIds(List<Long> deleteIds, List<Equipment> allEquipments) {
        List<Long> currentIds = new ArrayList<>(deleteIds);
        for (Long parentId : currentIds) {
            List<Long> childIds = allEquipments.stream()
                .filter(e -> parentId.equals(e.getIdParent()) && !deleteIds.contains(e.getId()))
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

            // 设备类型
            if (equipmentNode.has("type") && !equipmentNode.get("type").isNull()) {
                equipment.setType(equipmentNode.get("type").asInt());
            } else {
                equipment.setType(0); // 默认类型
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

            // 注意：idParent在这里不设置，在调用方法中单独处理

            return equipment;
        } catch (Exception e) {
            throw new ServiceException("解析设备数据失败：" + e.getMessage());
        }
    }

    @Override
    public List<Equipment> getEquipmentsByType(Integer type) {
        LambdaQueryWrapper<Equipment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(type !=null,Equipment::getType, type);
        wrapper.orderByAsc(Equipment::getId);
        return baseMapper.selectList(wrapper);
    }
}
