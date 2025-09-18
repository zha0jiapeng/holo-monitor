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
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.enums.DataTypeEnum;
import org.dromara.hm.mapper.HierarchyPropertyMapper;
import org.dromara.hm.service.IHierarchyPropertyService;
import org.dromara.hm.service.IHierarchyService;
import org.dromara.hm.service.IHierarchyTypePropertyDictService;
import org.dromara.hm.service.IHierarchyTypePropertyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    public Boolean updateByDictKey(HierarchyPropertyBo bo) {
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
        HierarchyTypePropertyVo hierarchyTypeProperty = hierarchyTypePropertyService.queryById(property.getTypePropertyId());
        if(hierarchyTypeProperty.getDict().getDataType().equals(DataTypeEnum.ASSOCIATION.getCode())){
            String propertyValue = property.getPropertyValue();
            String[] oldProperty = propertyValue.split("\\,"); // 库里现有的属性
            String[] newProperty = bo.getPropertyValue().split("\\,"); // 传来的新属性

            // 转换为Set便于比较
            Set<String> existingIds = new HashSet<>(Arrays.asList(oldProperty));
            Set<String> newIds = new HashSet<>(Arrays.asList(newProperty));

            // 找出要删除的关联（在现有中但不在新的中）
            Set<String> toDelete = new HashSet<>(existingIds);
            toDelete.removeAll(newIds);

            // 找出要新增的关联（在新的中但不在现有中）
            Set<String> toAdd = new HashSet<>(newIds);
            toAdd.removeAll(existingIds);

            // 获取sensor_device字典配置
            HierarchyTypePropertyDict sensorDevice = hierarchyTypePropertyDictService.getOne(
                new LambdaQueryWrapper<HierarchyTypePropertyDict>().eq(HierarchyTypePropertyDict::getDictKey, "sensor_device"));

            // 处理删除的关联
            for (String hierarchyIdStr : toDelete) {
                if (hierarchyIdStr.trim().isEmpty()) continue;

                try {
                    Long hierarchyId = Long.valueOf(hierarchyIdStr.trim());
                    HierarchyVo hierarchyVo = hierarchyService.queryById(hierarchyId, true);
                    if (hierarchyVo == null) continue;

                    Long typeId = hierarchyVo.getTypeId();
                    HierarchyTypeProperty typeProperty = hierarchyTypePropertyService.getOne(
                        new LambdaQueryWrapper<HierarchyTypeProperty>()
                            .eq(HierarchyTypeProperty::getTypeId, typeId)
                            .eq(HierarchyTypeProperty::getPropertyDictId, sensorDevice.getId())
                    );

                    if (typeProperty != null) {
                        // 删除对应的sensor_device关联
                        baseMapper.delete(new LambdaQueryWrapper<HierarchyProperty>()
                            .eq(HierarchyProperty::getHierarchyId, hierarchyId)
                            .eq(HierarchyProperty::getTypePropertyId, typeProperty.getId())
                            //.eq(HierarchyProperty::getPropertyValue, property.getHierarchyId() + "")
                        );
                        log.info("删除sensor_device关联: hierarchyId={}, 关联到={}", hierarchyId, property.getHierarchyId());
                    }
                } catch (NumberFormatException e) {
                    log.warn("删除关联时层级ID格式错误: {}", hierarchyIdStr, e);
                }
            }

            // 处理新增的关联
            for (String hierarchyIdStr : toAdd) {
                if (hierarchyIdStr.trim().isEmpty()) continue;

                try {
                    Long hierarchyId = Long.valueOf(hierarchyIdStr.trim());
                    HierarchyVo hierarchyVo = hierarchyService.queryById(hierarchyId, false);
                    if (hierarchyVo == null) continue;

                    // 检查是否已存在相同的关联
                    Long existingCount = baseMapper.selectCount(new LambdaQueryWrapper<HierarchyProperty>()
                        .eq(HierarchyProperty::getHierarchyId, hierarchyVo.getId())
                        .eq(HierarchyProperty::getPropertyValue, property.getHierarchyId() + "")
                    );
                    if (existingCount > 0) {
                        continue; // 已存在，跳过
                    }

                    Long typeId = hierarchyVo.getTypeId();
                    HierarchyTypeProperty typeProperty = hierarchyTypePropertyService.getOne(
                        new LambdaQueryWrapper<HierarchyTypeProperty>()
                            .eq(HierarchyTypeProperty::getTypeId, typeId)
                            .eq(HierarchyTypeProperty::getPropertyDictId, sensorDevice.getId())
                    );

                    if (typeProperty != null) {
                        // 新增sensor_device关联
                        HierarchyProperty hierarchyProperty = new HierarchyProperty();
                        hierarchyProperty.setScope(0);
                        hierarchyProperty.setTypePropertyId(typeProperty.getId());
                        hierarchyProperty.setHierarchyId(hierarchyId);
                        hierarchyProperty.setPropertyValue(property.getHierarchyId() + "");
                        baseMapper.insert(hierarchyProperty);
                        log.info("新增sensor_device关联: hierarchyId={}, 关联到={}", hierarchyId, property.getHierarchyId());
                    }
                } catch (NumberFormatException e) {
                    log.warn("新增关联时层级ID格式错误: {}", hierarchyIdStr, e);
                }
            }
        }

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

}
