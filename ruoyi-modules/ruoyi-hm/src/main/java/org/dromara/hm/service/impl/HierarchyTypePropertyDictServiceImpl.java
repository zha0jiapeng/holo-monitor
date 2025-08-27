package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.HierarchyProperty;
import org.dromara.hm.domain.HierarchyTypeProperty;
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import org.dromara.hm.domain.bo.HierarchyTypePropertyDictBo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.mapper.HierarchyPropertyMapper;
import org.dromara.hm.mapper.HierarchyTypePropertyDictMapper;
import org.dromara.hm.mapper.HierarchyTypePropertyMapper;
import org.dromara.hm.service.IHierarchyTypePropertyDictService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 层级类型属性字典Service业务层处理
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class HierarchyTypePropertyDictServiceImpl implements IHierarchyTypePropertyDictService {

    private final HierarchyTypePropertyDictMapper baseMapper;
    private final HierarchyTypePropertyMapper hierarchyTypePropertyMapper;

    @Override
    public HierarchyTypePropertyDictVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<HierarchyTypePropertyDictVo> queryPageList(HierarchyTypePropertyDictBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyTypePropertyDict> lqw = buildQueryWrapper(bo);
        Page<HierarchyTypePropertyDictVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<HierarchyTypePropertyDictVo> customPageList(HierarchyTypePropertyDictBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyTypePropertyDict> lqw = buildQueryWrapper(bo);
        Page<HierarchyTypePropertyDictVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<HierarchyTypePropertyDictVo> queryList(HierarchyTypePropertyDictBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<HierarchyTypePropertyDict> buildQueryWrapper(HierarchyTypePropertyDictBo bo) {
        LambdaQueryWrapper<HierarchyTypePropertyDict> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getDictName()), HierarchyTypePropertyDict::getDictName, bo.getDictName());
        lqw.eq(bo.getDataType() != null, HierarchyTypePropertyDict::getDataType, bo.getDataType());
        lqw.orderByAsc(HierarchyTypePropertyDict::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(HierarchyTypePropertyDictBo bo) {
        HierarchyTypePropertyDict add = MapstructUtils.convert(bo, HierarchyTypePropertyDict.class);
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
    public Boolean updateByBo(HierarchyTypePropertyDictBo bo) {
        HierarchyTypePropertyDict update = MapstructUtils.convert(bo, HierarchyTypePropertyDict.class);
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
    private void validEntityBeforeSave(HierarchyTypePropertyDict entity) {
        // 校验字典名称不能重复
        if (entity.getDictName() != null) {
            LambdaQueryWrapper<HierarchyTypePropertyDict> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(HierarchyTypePropertyDict::getDictName, entity.getDictName());
            if (entity.getId() != null) {
                wrapper.ne(HierarchyTypePropertyDict::getId, entity.getId());
            }
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("字典名称已存在");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验删除权限
            List<HierarchyTypePropertyDict> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("您没有删除权限!");
            }

            // 校验是否存在引用
            for (HierarchyTypePropertyDict dict : list) {
                LambdaQueryWrapper<HierarchyTypeProperty> wrapper = Wrappers.lambdaQuery();
                wrapper.eq(HierarchyTypeProperty::getPropertyDictId, dict.getId().toString());
                if (hierarchyTypePropertyMapper.exists(wrapper)) {
                    throw new ServiceException("字典项【" + dict.getDictName() + "】正在被使用，无法删除!");
                }
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<HierarchyTypePropertyDict> list) {
        return baseMapper.insertBatch(list);
    }

}
