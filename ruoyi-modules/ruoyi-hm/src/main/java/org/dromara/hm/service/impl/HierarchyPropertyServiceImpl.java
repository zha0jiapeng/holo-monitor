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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.HierarchyProperty;
import org.dromara.hm.domain.HierarchyTypeProperty;
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import org.dromara.hm.domain.bo.HierarchyPropertyBo;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.enums.DataTypeEnum;
import org.dromara.hm.mapper.HierarchyMapper;
import org.dromara.hm.mapper.HierarchyPropertyMapper;
import org.dromara.hm.mapper.HierarchyTypePropertyDictMapper;
import org.dromara.hm.mapper.HierarchyTypePropertyMapper;
import org.dromara.hm.service.IHierarchyPropertyService;
import org.dromara.hm.service.IHierarchyService;
import org.dromara.hm.service.IHierarchyTypePropertyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 层级属性Service业务层处理
 *
 * @author Mashir0
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class HierarchyPropertyServiceImpl extends ServiceImpl<HierarchyPropertyMapper, HierarchyProperty>  implements IHierarchyPropertyService {

    private final HierarchyPropertyMapper baseMapper;
    private final IHierarchyTypePropertyService hierarchyTypePropertyService;
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
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            if (add != null) {
                bo.setId(add.getId());
            }
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(HierarchyPropertyBo bo) {
        HierarchyProperty update = MapstructUtils.convert(bo, HierarchyProperty.class);
        if (update != null) {
            validEntityBeforeSave(update);
        }
        //TODO 传感器绑定设备反向关系 目前没法建立 没有typePropertyId
//        HierarchyTypePropertyVo hierarchyTypeProperty = hierarchyTypePropertyService.queryById(bo.getTypePropertyId());
//        if(hierarchyTypeProperty.getDict().getDataType().equals(DataTypeEnum.ASSOCIATION.getCode())){
//            String[] split = bo.getPropertyValue().split("\\,");
//            for (String hierarchyIdStr : split) {
//                Long hierarchyId = Long.valueOf(hierarchyIdStr);
//                HierarchyVo hierarchyVo = hierarchyService.queryById(hierarchyId, false);
//                HierarchyProperty hierarchyProperty = new  HierarchyProperty();
//                hierarchyProperty.setScope(0);
//                hierarchyProperty.setTypePropertyId();
//                hierarchyProperty.setHierarchyId(hierarchyId);
//                hierarchyProperty.setPropertyValue(bo.getHierarchyId() + "");
//                baseMapper.insert(hierarchyProperty);
//            }
//        }

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
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("该层级下已存在相同的属性key");
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
