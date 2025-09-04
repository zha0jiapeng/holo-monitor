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
import org.dromara.hm.domain.DictType;
import org.dromara.hm.domain.bo.DictTypeBo;
import org.dromara.hm.domain.vo.DictTypeVo;
import org.dromara.hm.mapper.DictTypeMapper;
import org.dromara.hm.service.IDictTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 字典类型Service业务层处理
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class DictTypeServiceImpl implements IDictTypeService {

    private final DictTypeMapper baseMapper;

    @Override
    public DictTypeVo queryById(Long dictId) {
        return baseMapper.selectVoById(dictId);
    }

    @Override
    public TableDataInfo<DictTypeVo> queryPageList(DictTypeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DictType> lqw = buildQueryWrapper(bo);
        Page<DictTypeVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<DictTypeVo> customPageList(DictTypeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DictType> lqw = buildQueryWrapper(bo);
        Page<DictTypeVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<DictTypeVo> queryList(DictTypeBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<DictType> buildQueryWrapper(DictTypeBo bo) {
        LambdaQueryWrapper<DictType> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getDictName()), DictType::getDictName, bo.getDictName());
        lqw.like(StringUtils.isNotBlank(bo.getDictType()), DictType::getDictType, bo.getDictType());
        lqw.orderByAsc(DictType::getDictId);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(DictTypeBo bo) {
        DictType add = MapstructUtils.convert(bo, DictType.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setDictId(add.getDictId());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(DictTypeBo bo) {
        DictType update = MapstructUtils.convert(bo, DictType.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DictType entity) {
        // 校验字典类型唯一性
        if (StringUtils.isNotBlank(entity.getDictType())) {
            LambdaQueryWrapper<DictType> lqw = Wrappers.lambdaQuery();
            lqw.eq(DictType::getDictType, entity.getDictType());
            if (entity.getDictId() != null) {
                lqw.ne(DictType::getDictId, entity.getDictId());
            }
            long count = baseMapper.selectCount(lqw);
            if (count > 0) {
                throw new ServiceException("字典类型已存在: " + entity.getDictType());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> dictIds, Boolean isValid) {
        if (isValid) {
            // 校验是否存在关联数据
            // TODO: 根据业务需要添加关联数据校验逻辑
        }
        return baseMapper.deleteByIds(dictIds) > 0;
    }

    @Override
    public Boolean saveBatch(List<DictType> list) {
        return baseMapper.insertBatch(list);
    }

}
