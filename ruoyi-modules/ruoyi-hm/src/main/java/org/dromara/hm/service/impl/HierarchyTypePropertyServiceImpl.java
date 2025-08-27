package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.HierarchyTypeProperty;
import org.dromara.hm.domain.bo.HierarchyTypePropertyBo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.mapper.HierarchyTypePropertyMapper;
import org.dromara.hm.service.IHierarchyTypePropertyService;
import org.springframework.stereotype.Service;

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
public class HierarchyTypePropertyServiceImpl implements IHierarchyTypePropertyService {

    private final HierarchyTypePropertyMapper baseMapper;

    @Override
    public HierarchyTypePropertyVo queryById(Long id) {
        return baseMapper.selectVoById(id);
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
            // 可以在这里添加删除前校验逻辑，比如检查是否有其他地方引用了这些属性
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<HierarchyTypeProperty> list) {
        return baseMapper.insertBatch(list);
    }

    @Override
    public List<HierarchyTypePropertyVo> getPropertiesByTypeId(Long typeId) {
        LambdaQueryWrapper<HierarchyTypeProperty> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(HierarchyTypeProperty::getTypeId, typeId)
               .orderByAsc(HierarchyTypeProperty::getId);
        return baseMapper.selectVoList(wrapper);
    }

}
