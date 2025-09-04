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
import org.dromara.hm.domain.DictData;
import org.dromara.hm.domain.bo.DictDataBo;
import org.dromara.hm.domain.vo.DictDataVo;
import org.dromara.hm.mapper.DictDataMapper;
import org.dromara.hm.service.IDictDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 字典数据Service业务层处理
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class DictDataServiceImpl implements IDictDataService {

    private final DictDataMapper baseMapper;

    @Override
    public DictDataVo queryById(Long dictCode) {
        return baseMapper.selectVoById(dictCode);
    }

    @Override
    public TableDataInfo<DictDataVo> queryPageList(DictDataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DictData> lqw = buildQueryWrapper(bo);
        Page<DictDataVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<DictDataVo> customPageList(DictDataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DictData> lqw = buildQueryWrapper(bo);
        Page<DictDataVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<DictDataVo> queryList(DictDataBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public List<DictDataVo> queryListByDictType(String dictType) {
        LambdaQueryWrapper<DictData> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(dictType), DictData::getDictType, dictType);
        lqw.orderByAsc(DictData::getDictCode);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DictData> buildQueryWrapper(DictDataBo bo) {
        LambdaQueryWrapper<DictData> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getDictLabel()), DictData::getDictLabel, bo.getDictLabel());
        lqw.like(StringUtils.isNotBlank(bo.getDictType()), DictData::getDictType, bo.getDictType());
        lqw.orderByAsc(DictData::getDictCode);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(DictDataBo bo) {
        DictData add = MapstructUtils.convert(bo, DictData.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setDictCode(add.getDictCode());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(DictDataBo bo) {
        DictData update = MapstructUtils.convert(bo, DictData.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DictData entity) {
        // 校验字典标签在同一字典类型下唯一性
        if (StringUtils.isNotBlank(entity.getDictType()) && StringUtils.isNotBlank(entity.getDictLabel())) {
            LambdaQueryWrapper<DictData> lqw = Wrappers.lambdaQuery();
            lqw.eq(DictData::getDictType, entity.getDictType());
            lqw.eq(DictData::getDictLabel, entity.getDictLabel());
            if (entity.getDictCode() != null) {
                lqw.ne(DictData::getDictCode, entity.getDictCode());
            }
            long count = baseMapper.selectCount(lqw);
            if (count > 0) {
                throw new ServiceException("字典标签已存在: " + entity.getDictLabel());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> dictCodes, Boolean isValid) {
        if (isValid) {
            // 校验是否存在关联数据
            // TODO: 根据业务需要添加关联数据校验逻辑
        }
        return baseMapper.deleteByIds(dictCodes) > 0;
    }

    @Override
    public Boolean saveBatch(List<DictData> list) {
        return baseMapper.insertBatch(list);
    }

}
