package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.HierarchyProperty;
import org.dromara.hm.domain.HierarchyTypeProperty;
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import org.dromara.hm.domain.bo.HierarchyPropertyBo;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.mapper.HierarchyPropertyMapper;
import org.dromara.hm.service.IHierarchyPropertyService;
import org.dromara.hm.service.IHierarchyService;
import org.dromara.hm.service.IHierarchyTypePropertyDictService;
import org.dromara.hm.service.IHierarchyTypePropertyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import org.dromara.common.core.utils.StringUtils;

/**
 * 层级属性Service业务层处理
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class HierarchyPropertyServiceImpl extends ServiceImpl<HierarchyPropertyMapper, HierarchyProperty>  implements IHierarchyPropertyService {

    private final HierarchyPropertyMapper baseMapper;
    private final IHierarchyTypePropertyService hierarchyTypePropertyService;
    private final IHierarchyTypePropertyDictService hierarchyTypePropertyDictService;
    private final IHierarchyService hierarchyService;

    @Override
    public HierarchyPropertyVo queryById(Long id) {
        HierarchyPropertyVo hierarchyPropertyVo = baseMapper.selectVoById(id);
        HierarchyTypePropertyVo hierarchyTypePropertyVo = hierarchyTypePropertyService.queryById(hierarchyPropertyVo.getTypePropertyId());
        hierarchyPropertyVo.setTypeProperty(hierarchyTypePropertyVo);
        return hierarchyPropertyVo;
    }

    @Override
    public TableDataInfo<HierarchyPropertyVo> queryPageList(HierarchyPropertyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyProperty> lqw = buildQueryWrapper(bo);
        Page<HierarchyPropertyVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        for (HierarchyPropertyVo record : result.getRecords()) {
            HierarchyTypePropertyVo hierarchyTypePropertyVo = hierarchyTypePropertyService.queryById(record.getTypePropertyId());
            record.setTypeProperty(hierarchyTypePropertyVo);
        }
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<HierarchyPropertyVo> customPageList(HierarchyPropertyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyProperty> lqw = buildQueryWrapper(bo);
        Page<HierarchyPropertyVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<HierarchyPropertyVo> queryList(HierarchyPropertyBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<HierarchyProperty> buildQueryWrapper(HierarchyPropertyBo bo) {
        LambdaQueryWrapper<HierarchyProperty> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getHierarchyId() != null, HierarchyProperty::getHierarchyId, bo.getHierarchyId());
        lqw.eq(bo.getTypePropertyId()!=null, HierarchyProperty::getTypePropertyId, bo.getTypePropertyId());
        lqw.orderByAsc(HierarchyProperty::getId);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(HierarchyPropertyBo bo) {
        HierarchyProperty add = MapstructUtils.convert(bo, HierarchyProperty.class);
        if (add != null) {
            validEntityBeforeSave(add);
        }
        boolean flag = baseMapper.insertOrUpdate(add);
        if (flag) {
            if (add != null) {
                bo.setId(add.getId());
            }
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean bindSensers(HierarchyPropertyBo bo) {
        HierarchyTypePropertyDict dict = hierarchyTypePropertyDictService.lambdaQuery().eq(HierarchyTypePropertyDict::getDictKey, bo.getDictKey()).one();
        if(dict==null) return false;
        Hierarchy hierarchy = hierarchyService.getById(bo.getHierarchyId());
        if(hierarchy==null) return false;
        HierarchyTypeProperty hierarchyTypeProperty = hierarchyTypePropertyService.lambdaQuery().eq(HierarchyTypeProperty::getTypeId, hierarchy.getTypeId()).eq(HierarchyTypeProperty::getPropertyDictId, dict.getId()).one();
        if(hierarchyTypeProperty==null) return false;
        HierarchyProperty property = lambdaQuery().eq(HierarchyProperty::getHierarchyId, hierarchy.getId())
            .eq(HierarchyProperty::getTypePropertyId, hierarchyTypeProperty.getId()).one();
        if(property==null) {
            property = new HierarchyProperty();
            property.setHierarchyId(hierarchy.getId());
            property.setTypePropertyId(hierarchyTypeProperty.getId());
        }
        property.setPropertyValue(bo.getPropertyValue());
        if("sensors".equals(bo.getDictKey())){
            // 1. 解析新的传感器ID列表
            Set<Long> newSensorIds = new HashSet<>();
            if (StringUtils.isNotBlank(bo.getPropertyValue())) {
                String[] split = bo.getPropertyValue().split(",");
                for (String idStr : split) {
                    if (StringUtils.isNotBlank(idStr)) {
                        newSensorIds.add(Long.parseLong(idStr.trim()));
                    }
                }
            }

            // 2. 获取 sensor_device 属性字典和类型属性定义
            HierarchyTypePropertyDict sensorDeviceDict = hierarchyTypePropertyDictService.lambdaQuery()
                .eq(HierarchyTypePropertyDict::getDictKey, "sensor_device").one();
            if (sensorDeviceDict == null) return false;

            // 3. 查询当前已绑定到此 hierarchyId 的所有传感器
            List<HierarchyProperty> existingBindings = lambdaQuery()
                .eq(HierarchyProperty::getPropertyValue, bo.getHierarchyId().toString())
                .exists("SELECT 1 FROM hm_hierarchy_type_property htp " +
                    "WHERE htp.id = hm_hierarchy_property.type_property_id " +
                    "AND htp.property_dict_id = {0}", sensorDeviceDict.getId())
                .list();

            Set<Long> oldSensorIds = existingBindings.stream()
                .map(HierarchyProperty::getHierarchyId)
                .collect(Collectors.toSet());

            // 4. 计算差异
            Set<Long> toAdd = new HashSet<>(newSensorIds);
            toAdd.removeAll(oldSensorIds); // 需要新增的传感器

            Set<Long> toRemove = new HashSet<>(oldSensorIds);
            toRemove.removeAll(newSensorIds); // 需要删除的传感器

            // 5. 处理新增的传感器绑定
            if (!toAdd.isEmpty()) {
                // 获取第一个传感器来确定类型（假设所有传感器类型相同）
                Long firstSensorId = toAdd.iterator().next();
                Hierarchy sensorHierarchy = hierarchyService.getById(firstSensorId);
                if (sensorHierarchy == null) return false;

                HierarchyTypeProperty sensorDeviceTypeProperty = hierarchyTypePropertyService
                    .lambdaQuery()
                    .eq(HierarchyTypeProperty::getTypeId, sensorHierarchy.getTypeId())
                    .eq(HierarchyTypeProperty::getPropertyDictId, sensorDeviceDict.getId())
                    .one();
                if (sensorDeviceTypeProperty == null) return false;

                // 为每个新增的传感器创建绑定关系
                for (Long sensorId : toAdd) {
                    // 先查询是否已存在该传感器的 sensor_device 属性
                    HierarchyProperty existingProperty = lambdaQuery()
                        .eq(HierarchyProperty::getHierarchyId, sensorId)
                        .eq(HierarchyProperty::getTypePropertyId, sensorDeviceTypeProperty.getId())
                        .one();

                    if (existingProperty != null) {
                        // 如果存在，更新属性值
                        existingProperty.setPropertyValue(bo.getHierarchyId().toString());
                        existingProperty.setScope(1);
                        updateById(existingProperty);
                    } else {
                        // 如果不存在，新增
                        HierarchyProperty newProperty = new HierarchyProperty();
                        newProperty.setHierarchyId(sensorId);
                        newProperty.setPropertyValue(bo.getHierarchyId().toString());
                        newProperty.setTypePropertyId(sensorDeviceTypeProperty.getId());
                        newProperty.setScope(1);
                        save(newProperty);
                    }
                }
            }

            // 6. 处理需要删除的传感器绑定
            if (!toRemove.isEmpty()) {
                // 删除这些传感器的 sensor_device 绑定关系
                for (HierarchyProperty binding : existingBindings) {
                    if (toRemove.contains(binding.getHierarchyId())) {
                        removeById(binding.getId());
                    }
                }
            }
        }

        return baseMapper.insertOrUpdate(property);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(HierarchyPropertyBo bo) {
        HierarchyProperty update = MapstructUtils.convert(bo, HierarchyProperty.class);
        if (update != null) {
            validEntityBeforeSave(update);
        }
        HierarchyProperty property = baseMapper.selectById(bo.getId());
        if(property==null) return false;
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(HierarchyProperty entity) {
        // 校验属性key在同一层级下不能重复
        if ( entity.getHierarchyId() != null) {
            LambdaQueryWrapper<HierarchyProperty> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(HierarchyProperty::getHierarchyId, entity.getHierarchyId());
            wrapper.eq(HierarchyProperty::getTypePropertyId, entity.getTypePropertyId());
            if (entity.getId() != null) {
                wrapper.ne(HierarchyProperty::getId, entity.getId());
            }
            HierarchyProperty hierarchyProperty = baseMapper.selectOne(wrapper);
            if (hierarchyProperty!=null) {
                entity.setId(hierarchyProperty.getId());
            }
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 可以在这里添加删除前校验逻辑，比如检查是否有其他地方引用了这些属性
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<HierarchyProperty> list) {
        return baseMapper.insertBatch(list);
    }

    @Override
    public List<HierarchyPropertyVo> getPropertiesByTypeId(Long typeId) {
        LambdaQueryWrapper<HierarchyProperty> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(HierarchyProperty::getHierarchyId, typeId)
               .orderByAsc(HierarchyProperty::getId);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public HierarchyPropertyVo getPropertyByTypeIdAndName(Long typeId, String propertyDictId) {
        LambdaQueryWrapper<HierarchyProperty> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(HierarchyProperty::getHierarchyId, typeId)
               .eq(HierarchyProperty::getTypePropertyId, propertyDictId);
        return baseMapper.selectVoOne(wrapper);
    }

    @Override
    public List<HierarchyPropertyVo> getPropertiesByHierarchyIdAndDictKeys(Long hierarchyId, List<String> dictKeys) {
        if (hierarchyId == null || dictKeys == null || dictKeys.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 根据字典键查询字典ID列表
        List<HierarchyTypePropertyDict> dicts = hierarchyTypePropertyDictService.lambdaQuery()
                .in(HierarchyTypePropertyDict::getDictKey, dictKeys)
                .list();

        if (dicts.isEmpty()) {
            log.warn("未找到字典键对应的字典: {}", dictKeys);
            return new ArrayList<>();
        }

        List<Long> dictIds = dicts.stream()
                .map(HierarchyTypePropertyDict::getId)
                .collect(Collectors.toList());

        // 2. 获取层级信息，获取其类型ID
        Hierarchy hierarchy = hierarchyService.getById(hierarchyId);
        if (hierarchy == null) {
            log.warn("未找到层级: {}", hierarchyId);
            return new ArrayList<>();
        }

        // 3. 根据类型ID和字典ID查询类型属性定义
        List<HierarchyTypeProperty> typeProperties = hierarchyTypePropertyService.lambdaQuery()
                //.eq(HierarchyTypeProperty::getTypeId, hierarchy.getTypeId())
                .in(HierarchyTypeProperty::getPropertyDictId, dictIds)
                .list();

        if (typeProperties.isEmpty()) {
            log.warn("未找到类型属性定义: typeId={}, dictKeys={}", hierarchy.getTypeId(), dictKeys);
            return new ArrayList<>();
        }

        List<Long> typePropertyIds = typeProperties.stream()
                .map(HierarchyTypeProperty::getId)
                .collect(Collectors.toList());

        // 4. 根据层级ID和类型属性ID查询属性列表
        LambdaQueryWrapper<HierarchyProperty> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(HierarchyProperty::getHierarchyId, hierarchyId)
               .in(HierarchyProperty::getTypePropertyId, typePropertyIds)
               .orderByAsc(HierarchyProperty::getId);

        List<HierarchyPropertyVo> result = baseMapper.selectVoList(wrapper);

        // 5. 填充类型属性信息
        for (HierarchyPropertyVo vo : result) {
            HierarchyTypePropertyVo typePropertyVo = hierarchyTypePropertyService.queryById(vo.getTypePropertyId());
            vo.setTypeProperty(typePropertyVo);
        }

        log.info("根据hierarchyId={} 和 dictKeys={} 查询到 {} 条属性", hierarchyId, dictKeys, result.size());
        return result;
    }

}
