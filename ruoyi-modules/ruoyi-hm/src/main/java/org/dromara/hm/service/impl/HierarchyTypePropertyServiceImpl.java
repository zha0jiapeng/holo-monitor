package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.HierarchyProperty;
import org.dromara.hm.domain.HierarchyTypeProperty;
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import org.dromara.hm.domain.bo.HierarchyTypePropertyBo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.mapper.HierarchyMapper;
import org.dromara.hm.mapper.HierarchyPropertyMapper;
import org.dromara.hm.mapper.HierarchyTypePropertyDictMapper;
import org.dromara.hm.mapper.HierarchyTypePropertyMapper;
import org.dromara.hm.service.IHierarchyTypePropertyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 层级类型属性Service业务层处理
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class HierarchyTypePropertyServiceImpl extends ServiceImpl<HierarchyTypePropertyMapper, HierarchyTypeProperty> implements IHierarchyTypePropertyService {

    private final HierarchyTypePropertyMapper baseMapper;
    private final HierarchyPropertyMapper hierarchyPropertyMapper;
    private final HierarchyMapper hierarchyMapper;
    private final HierarchyTypePropertyDictMapper hierarchyTypePropertyDictMapper;

    @Override
    public HierarchyTypePropertyVo queryById(Long id) {
        HierarchyTypePropertyVo hierarchyTypePropertyVo = baseMapper.selectVoById(id);
        hierarchyTypePropertyVo.setDict(hierarchyTypePropertyDictMapper.selectVoById(hierarchyTypePropertyVo.getPropertyDictId()));
        return hierarchyTypePropertyVo;
    }

    @Override
    public TableDataInfo<HierarchyTypePropertyVo> queryPageList(HierarchyTypePropertyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyTypeProperty> lqw = buildQueryWrapper(bo);
        Page<HierarchyTypePropertyVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<HierarchyTypePropertyVo> customPageList(HierarchyTypePropertyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyTypeProperty> lqw = buildQueryWrapper(bo);
        Page<HierarchyTypePropertyVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<HierarchyTypePropertyVo> queryList(HierarchyTypePropertyBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<HierarchyTypeProperty> buildQueryWrapper(HierarchyTypePropertyBo bo) {
        LambdaQueryWrapper<HierarchyTypeProperty> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getTypeId() != null, HierarchyTypeProperty::getTypeId, bo.getTypeId());
        lqw.eq(bo.getPropertyDictId()!=null, HierarchyTypeProperty::getPropertyDictId, bo.getPropertyDictId());
        lqw.eq(bo.getRequired() != null, HierarchyTypeProperty::getRequired, bo.getRequired());
        lqw.orderByAsc(HierarchyTypeProperty::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(HierarchyTypePropertyBo bo) {
        HierarchyTypeProperty add = MapstructUtils.convert(bo, HierarchyTypeProperty.class);
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
    public Boolean updateByBo(HierarchyTypePropertyBo bo) {
        HierarchyTypeProperty update = MapstructUtils.convert(bo, HierarchyTypeProperty.class);
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
    private void validEntityBeforeSave(HierarchyTypeProperty entity) {
        // 校验字典id在同一层级类型下不能重复
        if (entity.getPropertyDictId()!=null && entity.getTypeId() != null) {
            LambdaQueryWrapper<HierarchyTypeProperty> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(HierarchyTypeProperty::getTypeId, entity.getTypeId());
            wrapper.eq(HierarchyTypeProperty::getPropertyDictId, entity.getPropertyDictId());
            if (entity.getId() != null) {
                wrapper.ne(HierarchyTypeProperty::getId, entity.getId());
            }
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("该层级类型下已存在相同的字典id");
            }
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {

        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveBatch(List<HierarchyTypeProperty> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }

        // 获取前端传来的第一个记录的typeId，作为本次操作的层级类型ID
        Long typeId = list.get(0).getTypeId();
        if (typeId == null) {
            throw new ServiceException("层级类型ID不能为空");
        }

        // 校验所有记录的typeId必须一致
        for (HierarchyTypeProperty item : list) {
            if (!typeId.equals(item.getTypeId())) {
                throw new ServiceException("批量操作的所有记录必须属于同一层级类型");
            }
            if (item.getPropertyDictId() == null) {
                throw new ServiceException("属性字典ID不能为空");
            }
        }

        // 获取数据库中现有的所有记录
        LambdaQueryWrapper<HierarchyTypeProperty> existingWrapper = Wrappers.lambdaQuery();
        existingWrapper.eq(HierarchyTypeProperty::getTypeId, typeId);
        List<HierarchyTypeProperty> existingList = baseMapper.selectList(existingWrapper);

        // 分析需要执行的操作
        List<HierarchyTypeProperty> insertList = new ArrayList<>();
        //List<HierarchyTypeProperty> updateList = new ArrayList<>();
        List<Long> deleteIds = new ArrayList<>();

        // 找出需要删除的记录（现有记录中不在前端数据中的）
        for (HierarchyTypeProperty existing : existingList) {
            boolean found = false;
            for (HierarchyTypeProperty item : list) {
                if (existing.getPropertyDictId().equals(item.getPropertyDictId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                deleteIds.add(existing.getId());
            }
        }

        // 找出需要更新或新增的记录
        for (HierarchyTypeProperty item : list) {
            HierarchyTypeProperty existing = null;
            for (HierarchyTypeProperty existItem : existingList) {
                if (existItem.getPropertyDictId().equals(item.getPropertyDictId())) {
                    existing = existItem;
                    break;
                }
            }

            if (existing == null) {
                // 不存在，则准备新增
                insertList.add(item);
            }
        }

        // 执行删除操作
        if (!deleteIds.isEmpty()) {
            // 检查要删除的记录是否有引用
            for (Long deleteId : deleteIds) {
                HierarchyTypeProperty deleteItem = baseMapper.selectById(deleteId);
                if (deleteItem != null) {
                    LambdaQueryWrapper<HierarchyProperty> refWrapper = Wrappers.lambdaQuery();
                    refWrapper.eq(HierarchyProperty::getTypePropertyId, deleteItem.getPropertyDictId());
                    long referenceCount = hierarchyPropertyMapper.selectCount(refWrapper);

                    if (referenceCount > 0) {
                        throw new ServiceException("属性字典ID [" + deleteItem.getPropertyDictId() + "] 已被使用，无法删除");
                    }
                }
            }
            baseMapper.deleteByIds(deleteIds);
        }

        // 批量执行新增
        if (!insertList.isEmpty()) {
            for (HierarchyTypeProperty insertItem : insertList) {
                validEntityBeforeSave(insertItem);
                baseMapper.insert(insertItem);

                // 为所有该类型下的层级实例添加新属性
                List<Hierarchy> hierarchies = hierarchyMapper.selectList(
                    new LambdaQueryWrapper<Hierarchy>().eq(Hierarchy::getTypeId, insertItem.getTypeId())
                );

                List<HierarchyProperty> newProperties = new ArrayList<>();
                for (Hierarchy hierarchy : hierarchies) {
                    HierarchyProperty property = new HierarchyProperty();
                    property.setHierarchyId(hierarchy.getId());
                    property.setTypePropertyId(insertItem.getId());
                    property.setPropertyValue(null);
                    property.setScope(1); // 默认设置为可见
                    newProperties.add(property);
                }

                if (!newProperties.isEmpty()) {
                    hierarchyPropertyMapper.insertBatch(newProperties);
                }
            }
        }

        return true;
    }

    @Override
    public List<HierarchyTypePropertyVo> getPropertiesByTypeId(Long typeId) {
        LambdaQueryWrapper<HierarchyTypeProperty> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(HierarchyTypeProperty::getTypeId, typeId)
               .orderByAsc(HierarchyTypeProperty::getId);
        return baseMapper.selectVoList(wrapper);
    }

}
