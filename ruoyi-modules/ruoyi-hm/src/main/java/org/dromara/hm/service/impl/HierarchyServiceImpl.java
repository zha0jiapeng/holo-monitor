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
import org.dromara.hm.domain.*;
import org.dromara.hm.domain.bo.HierarchyBo;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.enums.DataTypeEnum;
import org.dromara.hm.mapper.*;
import org.dromara.hm.service.IHierarchyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 层级Service业务层处理
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class HierarchyServiceImpl implements IHierarchyService {

    private final HierarchyMapper baseMapper;
    private final HierarchyPropertyMapper hierarchyPropertyMapper;
    private final HierarchyTypeMapper hierarchyTypeMapper;
    private final HierarchyTypePropertyMapper hierarchyTypePropertyMapper;
    private final HierarchyTypePropertyDictMapper hierarchyTypePropertyDictMapper;
    private final TestpointMapper testpointMapper;

    @Override
    public HierarchyVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<HierarchyVo> queryPageList(HierarchyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Hierarchy> lqw = buildQueryWrapper(bo);
        Page<HierarchyVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);

        // 添加填充逻辑
        for (HierarchyVo vo : result.getRecords()) {
            List<HierarchyPropertyVo> properties = hierarchyPropertyMapper.selectVoList(
                Wrappers.<HierarchyProperty>lambdaQuery().eq(HierarchyProperty::getHierarchyId, vo.getId())
            );
            for (HierarchyPropertyVo prop : properties) {
                HierarchyTypePropertyVo typeProp = hierarchyTypePropertyMapper.selectVoById(prop.getTypePropertyId());
                if (typeProp != null) {
                    HierarchyTypePropertyDictVo dict = hierarchyTypePropertyDictMapper.selectVoById(typeProp.getPropertyDictId());
                    typeProp.setDict(dict);
                    prop.setTypeProperty(typeProp);
                }
            }
            vo.setProperties(properties);
        }

        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<HierarchyVo> customPageList(HierarchyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Hierarchy> lqw = buildQueryWrapper(bo);
        Page<HierarchyVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<HierarchyVo> queryList(HierarchyBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<Hierarchy> buildQueryWrapper(HierarchyBo bo) {
        LambdaQueryWrapper<Hierarchy> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getTypeId() != null, Hierarchy::getTypeId, bo.getTypeId());
        lqw.eq(bo.getParentId() != null, Hierarchy::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), Hierarchy::getName, bo.getName());
        lqw.orderByAsc(Hierarchy::getId);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(HierarchyBo bo) {
        Hierarchy add = MapstructUtils.convert(bo, Hierarchy.class);
        if (add != null) {
            validEntityBeforeSave(add);
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            if (add != null) {
                bo.setId(add.getId());
            }

            List<HierarchyProperty> extraProperties = new ArrayList<>();
            for (HierarchyProperty property : bo.getProperties()) {
                property.setHierarchyId(add != null ? add.getId() : null);
                HierarchyTypeProperty hierarchyTypeProperty = hierarchyTypePropertyMapper.selectById(property.getTypePropertyId());
                HierarchyTypePropertyDictVo hierarchyTypePropertyDictVo = hierarchyTypePropertyDictMapper.selectVoById(hierarchyTypeProperty.getPropertyDictId());
                if (hierarchyTypePropertyDictVo.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
                    HierarchyType hierarchyType = hierarchyTypeMapper.selectById(Long.valueOf(hierarchyTypePropertyDictVo.getDictValues()));
                    if (hierarchyType != null && hierarchyType.getCascadeFlag()) {
                        Hierarchy hierarchy = new Hierarchy();
                        hierarchy.setId(bo.getId());
                        hierarchy.setParentId(Long.valueOf(property.getPropertyValue()));
                        baseMapper.updateById(hierarchy);
                        if (hierarchyType.getCascadeParentId() != null) {
                            // 添加隐藏父级属性
                            Long currentBoundId = Long.valueOf(property.getPropertyValue());
                            HierarchyType currentType = hierarchyType;
                            while (currentType.getCascadeParentId() != null) {
                                Long parentTypeId = currentType.getCascadeParentId();
                                // 找到当前类型中指向父类型的typeProperty
                                HierarchyTypeProperty parentProperty = null;
                                List<HierarchyTypeProperty> props = hierarchyTypePropertyMapper.selectList(
                                    Wrappers.<HierarchyTypeProperty>lambdaQuery().eq(HierarchyTypeProperty::getTypeId, currentType.getId())
                                );
                                for (HierarchyTypeProperty p : props) {
                                    HierarchyTypePropertyDictVo d = hierarchyTypePropertyDictMapper.selectVoById(p.getPropertyDictId());
                                    if (d != null && d.getDataType().equals(DataTypeEnum.HIERARCHY.getCode()) && d.getDictValues().equals(parentTypeId.toString())) {
                                        parentProperty = p;
                                        break;
                                    }
                                }
                                if (parentProperty == null) {
                                    break;
                                }
                                // 获取绑定的父级hierarchyId
                                Hierarchy bound = baseMapper.selectById(currentBoundId);
                                if (bound == null || bound.getParentId() == null) {
                                    break;
                                }
                                Long parentBoundId = bound.getParentId();
                                // 创建隐藏property
                                HierarchyProperty hidden = new HierarchyProperty();
                                hidden.setHierarchyId(add.getId());
                                hidden.setTypePropertyId(parentProperty.getId());
                                hidden.setPropertyValue(parentBoundId.toString());
                                hidden.setScope(0);
                                extraProperties.add(hidden);
                                // 更新到下一级
                                currentBoundId = parentBoundId;
                                currentType = hierarchyTypeMapper.selectById(parentTypeId);
                            }
                        }
                    }
                }
            }
            hierarchyPropertyMapper.insertBatch(bo.getProperties());
            if (!extraProperties.isEmpty()) {
                hierarchyPropertyMapper.insertBatch(extraProperties);
            }
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(HierarchyBo bo) {
        Hierarchy update = MapstructUtils.convert(bo, Hierarchy.class);
        if (update != null) {
            validEntityBeforeSave(update);
        }
        List<HierarchyProperty> properties = bo.getProperties();
        for (HierarchyProperty property : properties) {
            hierarchyPropertyMapper.update(new LambdaUpdateWrapper<HierarchyProperty>()
                .set(HierarchyProperty::getPropertyValue,property.getPropertyValue())
                .eq(HierarchyProperty::getTypePropertyId,property.getTypePropertyId())
                .eq(HierarchyProperty::getHierarchyId,bo.getId())
            );

            String[] split = property.getPropertyValue().split(",");
            for (String s : split) {
                Testpoint testpoint = new Testpoint();
                testpoint.setId(Long.valueOf(s));
                testpoint.setHierarchyOwnerId(bo.getId());
                testpointMapper.updateById(testpoint);
            }


        }

        return baseMapper.updateById(update) > 0;
    }
    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(Hierarchy entity) {
        // 校验层级名称不能重复
        if (StringUtils.isNotBlank(entity.getName())) {
            LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(Hierarchy::getName, entity.getName());
            if (entity.getId() != null) {
                wrapper.ne(Hierarchy::getId, entity.getId());
            }
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("层级名称已存在");
            }
        }

        // 校验父级层级是否存在
        if (entity.getParentId() != null) {
            Hierarchy parent = baseMapper.selectById(entity.getParentId());
            if (parent == null) {
                throw new ServiceException("父级层级不存在");
            }

            // 防止循环引用
            if (entity.getId() != null && entity.getParentId().equals(entity.getId())) {
                throw new ServiceException("不能将自己设置为父级");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验删除权限
            List<Hierarchy> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("您没有删除权限!");
            }
            hierarchyPropertyMapper.delete(Wrappers.<HierarchyProperty>lambdaQuery().in(HierarchyProperty::getHierarchyId, ids));
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<Hierarchy> list) {
        return baseMapper.insertBatch(list);
    }

    @Override
    public List<HierarchyVo> getChildrenByParentId(Long parentId) {
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(parentId != null, Hierarchy::getParentId, parentId);
        wrapper.orderByAsc(Hierarchy::getId);
        return baseMapper.selectVoList(wrapper);
    }

}
