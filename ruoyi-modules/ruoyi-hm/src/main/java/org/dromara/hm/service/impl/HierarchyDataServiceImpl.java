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
import org.dromara.hm.domain.HierarchyData;
import org.dromara.hm.domain.bo.HierarchyDataBo;
import org.dromara.hm.domain.vo.HierarchyDataVo;
import org.dromara.hm.mapper.HierarchyDataMapper;
import org.dromara.hm.service.IHierarchyDataService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 层级数据Service业务层处理
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class HierarchyDataServiceImpl implements IHierarchyDataService {

    private final HierarchyDataMapper baseMapper;

    @Override
    public HierarchyDataVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<HierarchyDataVo> queryPageList(HierarchyDataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<HierarchyData> lqw = buildQueryWrapper(bo);
        Page<HierarchyDataVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<HierarchyDataVo> queryList(HierarchyDataBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public Boolean insertByBo(HierarchyDataBo bo) {
        HierarchyData add = MapstructUtils.convert(bo, HierarchyData.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(HierarchyDataBo bo) {
        HierarchyData update = MapstructUtils.convert(bo, HierarchyData.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(HierarchyData entity) {
        // 校验时间不能为空
        if (entity.getTime() == null) {
            throw new ServiceException("时间不能为空");
        }

        // 校验层级ID不能为空
        if (entity.getHierarchyId() == null) {
            throw new ServiceException("层级ID不能为空");
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验删除权限
            List<HierarchyData> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("您没有删除权限!");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<HierarchyData> list) {
        return baseMapper.insertBatch(list);
    }

    private LambdaQueryWrapper<HierarchyData> buildQueryWrapper(HierarchyDataBo bo) {
        LambdaQueryWrapper<HierarchyData> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getHierarchyId() != null, HierarchyData::getHierarchyId, bo.getHierarchyId());
        lqw.like(StringUtils.isNotBlank(bo.getTag()), HierarchyData::getTag, bo.getTag());
        lqw.like(StringUtils.isNotBlank(bo.getName()), HierarchyData::getName, bo.getName());
        lqw.eq(bo.getValue() != null, HierarchyData::getValue, bo.getValue());
        lqw.between(bo.getTime() != null, HierarchyData::getTime, bo.getTime(), bo.getTime());
        lqw.orderByDesc(HierarchyData::getTime);
        return lqw;
    }

}
