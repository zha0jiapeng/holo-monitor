package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.HierarchyTypeShow;
import org.dromara.hm.domain.bo.HierarchyTypeShowBo;
import org.dromara.hm.domain.vo.HierarchyTypeShowVo;
import org.dromara.hm.mapper.HierarchyTypeShowMapper;
import org.dromara.hm.service.IHierarchyTypeShowService;

import java.util.Collection;
import java.util.List;

/**
 * 层级类型展示Service业务层处理
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class HierarchyTypeShowServiceImpl implements IHierarchyTypeShowService {

    private final HierarchyTypeShowMapper baseMapper;

    @Override
    public HierarchyTypeShowVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<HierarchyTypeShowVo> queryPageList(HierarchyTypeShowBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyTypeShow> lqw = buildQueryWrapper(bo);
        Page<HierarchyTypeShowVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<HierarchyTypeShowVo> customPageList(HierarchyTypeShowBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyTypeShow> lqw = buildQueryWrapper(bo);
        Page<HierarchyTypeShowVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<HierarchyTypeShowVo> queryList(HierarchyTypeShowBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<HierarchyTypeShow> buildQueryWrapper(HierarchyTypeShowBo bo) {
        LambdaQueryWrapper<HierarchyTypeShow> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getTypeId() != null, HierarchyTypeShow::getTypeId, bo.getTypeId());
        lqw.eq(bo.getShowTypeId() != null, HierarchyTypeShow::getShowTypeId, bo.getShowTypeId());
        lqw.orderByAsc(HierarchyTypeShow::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(HierarchyTypeShowBo bo) {
        HierarchyTypeShow add = MapstructUtils.convert(bo, HierarchyTypeShow.class);
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
    public Boolean updateByBo(HierarchyTypeShowBo bo) {
        HierarchyTypeShow update = MapstructUtils.convert(bo, HierarchyTypeShow.class);
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
    private void validEntityBeforeSave(HierarchyTypeShow entity) {
        // 校验typeId和showTypeId的组合不能重复
        if (entity.getTypeId() != null && entity.getShowTypeId() != null) {
            LambdaQueryWrapper<HierarchyTypeShow> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(HierarchyTypeShow::getTypeId, entity.getTypeId());
            wrapper.eq(HierarchyTypeShow::getShowTypeId, entity.getShowTypeId());
            if (entity.getId() != null) {
                wrapper.ne(HierarchyTypeShow::getId, entity.getId());
            }
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("该层级类型的展示类型已存在");
            }
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 可以在这里添加删除前校验逻辑
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<HierarchyTypeShow> list) {
        return baseMapper.insertBatch(list);
    }

}
