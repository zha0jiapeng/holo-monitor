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
import org.dromara.hm.domain.bo.HierarchyTypeBo;
import org.dromara.hm.domain.vo.HierarchyTypeVo;
import org.dromara.hm.mapper.*;
import org.dromara.hm.service.IHierarchyTypeService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 层级类型Service业务层处理
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class HierarchyTypeServiceImpl implements IHierarchyTypeService {

    private final HierarchyTypeMapper baseMapper;
    private final HierarchyMapper hierarchyMapper;
    private final HierarchyTypePropertyMapper hierarchyTypePropertyMapper;
    private final HierarchyTypePropertyDictMapper hierarchyTypePropertyDictMapper;

    @Override
    public HierarchyTypeVo queryById(Long id) {
        HierarchyTypeVo hierarchyTypeVo = baseMapper.selectVoById(id);
        initVo(hierarchyTypeVo);
        return hierarchyTypeVo;
    }

    private void initVo(HierarchyTypeVo hierarchyTypeVo) {
        List<HierarchyTypeProperty> hierarchyTypeProperties = hierarchyTypePropertyMapper.selectList(
            new LambdaQueryWrapper<HierarchyTypeProperty>().eq(HierarchyTypeProperty::getTypeId, hierarchyTypeVo.getId()
            ));
        for (HierarchyTypeProperty hierarchyTypeProperty : hierarchyTypeProperties) {
            HierarchyTypePropertyDict hierarchyTypePropertyDict = hierarchyTypePropertyDictMapper.selectById(hierarchyTypeProperty.getPropertyDictId());
            if(hierarchyTypePropertyDict!=null){
                hierarchyTypeProperty.setDataType(hierarchyTypePropertyDict.getDataType());
                hierarchyTypeProperty.setDictName(hierarchyTypePropertyDict.getDictName());
                hierarchyTypeProperty.setDictValues(hierarchyTypePropertyDict.getDictValues());
            }
        }
        hierarchyTypeVo.setProperties(hierarchyTypeProperties);
    }

    @Override
    public TableDataInfo<HierarchyTypeVo> queryPageList(HierarchyTypeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyType> lqw = buildQueryWrapper(bo);
        Page<HierarchyTypeVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if(bo.getNeedProperty()) {
            List<HierarchyTypeVo> records = result.getRecords();
            for (HierarchyTypeVo hierarchyTypeVo : records) {
                initVo(hierarchyTypeVo);
            }
        }
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<HierarchyTypeVo> customPageList(HierarchyTypeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyType> lqw = buildQueryWrapper(bo);
        Page<HierarchyTypeVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<HierarchyTypeVo> queryList(HierarchyTypeBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<HierarchyType> buildQueryWrapper(HierarchyTypeBo bo) {
        LambdaQueryWrapper<HierarchyType> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), HierarchyType::getName, bo.getName());
        lqw.eq(bo.getCascadeParentId() != null, HierarchyType::getCascadeParentId, bo.getCascadeParentId());
        lqw.eq(bo.getCodeLength() != null, HierarchyType::getCodeLength, bo.getCodeLength());
        lqw.eq(bo.getCodeSort() != null, HierarchyType::getCodeSort, bo.getCodeSort());
        lqw.eq(bo.getTypeKey() != null, HierarchyType::getTypeKey, bo.getTypeKey());
        lqw.orderByAsc(HierarchyType::getId);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(HierarchyTypeBo bo) {
        HierarchyType add = MapstructUtils.convert(bo, HierarchyType.class);
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
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(HierarchyTypeBo bo) {
        // 获取原始数据
        HierarchyType original = baseMapper.selectById(bo.getId());
        if (original == null) {
            throw new ServiceException("层级类型不存在");
        }

        // 检查该类型下是否有层级数据
        LambdaQueryWrapper<Hierarchy> hierarchyWrapper = Wrappers.lambdaQuery();
        hierarchyWrapper.eq(Hierarchy::getTypeId, bo.getId());
        boolean hasHierarchyData = hierarchyMapper.exists(hierarchyWrapper);

        if (hasHierarchyData) {
            if (!Objects.equals(original.getCascadeParentId(), bo.getCascadeParentId())
            ) {
                throw new ServiceException("该层级类型下存在层级数据，只能修改名称字段，无法修改级联父级");
            }
        }

        HierarchyType update = MapstructUtils.convert(bo, HierarchyType.class);
        if (update != null) {
            validEntityBeforeSave(update);

            // 使用UpdateWrapper来确保null值也能被更新
            LambdaUpdateWrapper<HierarchyType> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(HierarchyType::getId, bo.getId());

            // 构建更新字段
            if (bo.getName() != null) {
                updateWrapper.set(HierarchyType::getName, bo.getName());
            }
            // 无论cascadeParentId是否为null，都要更新这个字段
            updateWrapper.set(HierarchyType::getCascadeParentId, bo.getCascadeParentId());

            return baseMapper.update(null, updateWrapper) > 0;
        }
        return false;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(HierarchyType entity) {
        // 校验层级类型名称不能重复
        if (StringUtils.isNotBlank(entity.getName())) {
            LambdaQueryWrapper<HierarchyType> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(HierarchyType::getName, entity.getName());
            if (entity.getId() != null) {
                wrapper.ne(HierarchyType::getId, entity.getId());
            }
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("层级类型名称已存在");
            }
        }

        // 校验级联父级是否存在
        if (entity.getCascadeParentId() != null) {
            HierarchyType cascadeParent = baseMapper.selectById(entity.getCascadeParentId());
            if (cascadeParent == null) {
                throw new ServiceException("级联父级不存在");
            }
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 检查是否有层级在使用这些类型ID
            for (Long typeId : ids) {
                LambdaQueryWrapper<Hierarchy> hierarchyWrapper = Wrappers.lambdaQuery();
                hierarchyWrapper.eq(Hierarchy::getTypeId, typeId);
                if (hierarchyMapper.exists(hierarchyWrapper)) {
                    throw new ServiceException("存在层级正在使用该类型，无法删除");
                }
            }

            // 校验删除权限
            List<HierarchyType> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("您没有删除权限!");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<HierarchyType> list) {
        return baseMapper.insertBatch(list);
    }


}
