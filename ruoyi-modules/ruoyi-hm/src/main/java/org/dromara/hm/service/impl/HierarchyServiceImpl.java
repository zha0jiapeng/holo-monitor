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

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * 生成层级完整编码
     * 根据层级属性中data_type=1001的隐藏属性，按层级类型的code_sort排序组合编码
     *
     * @param hierarchyId 层级ID
     * @return 生成的编码字符串
     */
    private String generateHierarchyCode(Long hierarchyId) {
        List<Map<String, Object>> hierarchyCodeInfoList = new ArrayList<>();

        // 查询该层级的所有属性
        List<HierarchyProperty> properties = hierarchyPropertyMapper.selectList(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .eq(HierarchyProperty::getHierarchyId, hierarchyId)
        );

        // 添加当前层级本身的编码
        Hierarchy current = baseMapper.selectById(hierarchyId);
        if (current != null && current.getCode() != null && !current.getCode().isEmpty()) {
            HierarchyType currentType = hierarchyTypeMapper.selectById(current.getTypeId());
            if (currentType != null) {
                Map<String, Object> currentInfo = new HashMap<>();
                currentInfo.put("code", current.getCode());
                currentInfo.put("codeSort", currentType.getCodeSort() != null ? currentType.getCodeSort() : 0);
                hierarchyCodeInfoList.add(currentInfo);
            }
        }

        // 遍历属性，找出data_type=1001的隐藏属性
        for (HierarchyProperty property : properties) {
            HierarchyTypeProperty typeProperty = hierarchyTypePropertyMapper.selectById(property.getTypePropertyId());
            if (typeProperty != null) {
                HierarchyTypePropertyDictVo dictVo = hierarchyTypePropertyDictMapper.selectVoById(typeProperty.getPropertyDictId());
                if (dictVo != null && dictVo.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
                    // 这是一个层级类型的隐藏属性，property_value代表层级id
                    Long relatedHierarchyId = Long.valueOf(property.getPropertyValue());
                    Hierarchy relatedHierarchy = baseMapper.selectById(relatedHierarchyId);

                    if (relatedHierarchy != null && relatedHierarchy.getCode() != null && !relatedHierarchy.getCode().isEmpty()) {
                        // 获取相关层级的类型信息
                        HierarchyType relatedType = hierarchyTypeMapper.selectById(relatedHierarchy.getTypeId());
                        if (relatedType != null) {
                            Map<String, Object> hierarchyInfo = new HashMap<>();
                            hierarchyInfo.put("code", relatedHierarchy.getCode());
                            hierarchyInfo.put("codeSort", relatedType.getCodeSort() != null ? relatedType.getCodeSort() : 0);
                            hierarchyCodeInfoList.add(hierarchyInfo);
                        }
                    }
                }
            }
        }

        if (hierarchyCodeInfoList.isEmpty()) {
            return null;
        }

        // 按code_sort升序排序
        hierarchyCodeInfoList.sort(Comparator.comparingInt(info -> (Integer) info.get("codeSort")));

        // 提取排序后的编码
        List<String> codeParts = hierarchyCodeInfoList.stream()
            .map(info -> (String) info.get("code"))
            .collect(Collectors.toList());

        return String.join("", codeParts);
    }

    /**
     * 从父级层级创建隐藏属性
     * 为父级层级的所有隐藏属性（data_type=1001）创建隐藏property
     *
     * @param currentHierarchyId 当前层级ID
     * @param parentHierarchyId 父级层级ID
     * @param extraProperties 额外属性列表
     */
    private void createHiddenPropertiesFromParent(Long currentHierarchyId, Long parentHierarchyId, List<HierarchyProperty> extraProperties) {
        // 获取父级层级信息
        Hierarchy parentHierarchy = baseMapper.selectById(parentHierarchyId);
        if (parentHierarchy == null) {
            return;
        }

        // 获取父级层级类型的所有隐藏属性（data_type=1001）
        List<HierarchyTypeProperty> parentTypeProperties = hierarchyTypePropertyMapper.selectList(
            Wrappers.<HierarchyTypeProperty>lambdaQuery().eq(HierarchyTypeProperty::getTypeId, parentHierarchy.getTypeId())
        );

        // 为父级的每个隐藏属性创建隐藏property
        for (HierarchyTypeProperty parentTypeProperty : parentTypeProperties) {
            HierarchyTypePropertyDictVo dictVo = hierarchyTypePropertyDictMapper.selectVoById(parentTypeProperty.getPropertyDictId());
            if (dictVo != null && dictVo.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
                // 查找父级层级的这个属性值
                HierarchyProperty parentProperty = hierarchyPropertyMapper.selectOne(
                    Wrappers.<HierarchyProperty>lambdaQuery()
                        .eq(HierarchyProperty::getHierarchyId, parentHierarchyId)
                        .eq(HierarchyProperty::getTypePropertyId, parentTypeProperty.getId())
                );

                if (parentProperty != null) {
                    // 为当前层级创建这个隐藏属性
                    HierarchyProperty hidden = new HierarchyProperty();
                    hidden.setHierarchyId(currentHierarchyId);
                    hidden.setTypePropertyId(parentTypeProperty.getId());
                    hidden.setPropertyValue(parentProperty.getPropertyValue());
                    hidden.setScope(0); // 隐藏属性
                    extraProperties.add(hidden);
                }
            }
        }

        // 递归处理父级的父级
        if (parentHierarchy.getParentId() != null) {
            createHiddenPropertiesFromParent(currentHierarchyId, parentHierarchy.getParentId(), extraProperties);
        }
    }

    /**
     * 在新事务中更新层级编码
     * 解决同事务内数据可见性问题
     *
     * @param hierarchyId 层级ID
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateHierarchyCodeInNewTransaction(Long hierarchyId) {
        String generatedCode = generateHierarchyCode(hierarchyId);
        if (generatedCode != null && !generatedCode.isEmpty()) {
            Hierarchy update = new Hierarchy();
            update.setId(hierarchyId);
            update.setCode(generatedCode);
            baseMapper.updateById(update);
        }
    }

    private LambdaQueryWrapper<Hierarchy> buildQueryWrapper(HierarchyBo bo) {
        LambdaQueryWrapper<Hierarchy> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getTypeId() != null, Hierarchy::getTypeId, bo.getTypeId());
        lqw.eq(bo.getParentId() != null, Hierarchy::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), Hierarchy::getName, bo.getName());
        lqw.like(StringUtils.isNotBlank(bo.getCode()), Hierarchy::getCode, bo.getCode());
        lqw.orderByAsc(Hierarchy::getId);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(HierarchyBo bo) {
        String code = bo.getCode();
        HierarchyType type = hierarchyTypeMapper.selectById(bo.getTypeId());
        if(code!=null&&!type.getCodeLength().equals(code.length())) {
            return false;
        }
        Hierarchy add = MapstructUtils.convert(bo, Hierarchy.class);
        if (add != null) {
            validEntityBeforeSave(add);
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            if (add != null) {
                bo.setId(add.getId());
            }
            initProperty(bo,type);
        }
        return flag;
    }

    private void initProperty(HierarchyBo bo,HierarchyType type) {
        List<HierarchyProperty> extraProperties = new ArrayList<>();

        // 处理用户提供的属性，找到有级联关系的属性来设置父级
        Long parentHierarchyId = null;
        for (HierarchyProperty property : bo.getProperties()) {
            property.setHierarchyId(bo != null ? bo.getId() : null);

            HierarchyTypeProperty hierarchyTypeProperty = hierarchyTypePropertyMapper.selectById(property.getTypePropertyId());
            HierarchyTypePropertyDictVo hierarchyTypePropertyDictVo = hierarchyTypePropertyDictMapper.selectVoById(hierarchyTypeProperty.getPropertyDictId());
            if (hierarchyTypePropertyDictVo.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
                HierarchyType hierarchyType = hierarchyTypeMapper.selectById(Long.valueOf(hierarchyTypePropertyDictVo.getDictValues()));
                if (hierarchyType != null && hierarchyType.getCascadeFlag()) {
                    // 找到级联属性，设置父级关系
                    parentHierarchyId = Long.valueOf(property.getPropertyValue());
                    Hierarchy hierarchy = new Hierarchy();
                    hierarchy.setId(bo.getId());
                    hierarchy.setParentId(parentHierarchyId);
                    baseMapper.updateById(hierarchy);
                    break; // 只需要找到一个级联属性来设置父级关系
                }
            }
        }

        // 如果找到了父级，需要为父级的所有隐藏属性创建隐藏property
        if (parentHierarchyId != null) {
            createHiddenPropertiesFromParent(bo.getId(), parentHierarchyId, extraProperties);
        }

        // 确保所有属性都设置了hierarchyId
        for (HierarchyProperty property : bo.getProperties()) {
            if (property.getHierarchyId() == null) {
                property.setHierarchyId(bo.getId());
            }
        }
        
        // 过滤掉hierarchyId为空的属性
        List<HierarchyProperty> validProperties = bo.getProperties().stream()
            .filter(p -> p.getHierarchyId() != null)
            .collect(Collectors.toList());
            
        List<HierarchyProperty> validExtraProperties = extraProperties.stream()
            .filter(p -> p.getHierarchyId() != null)
            .collect(Collectors.toList());
        
        if (!validProperties.isEmpty()) {
            hierarchyPropertyMapper.insertBatch(validProperties);
        }
        if (!validExtraProperties.isEmpty()) {
            hierarchyPropertyMapper.insertBatch(validExtraProperties);
        }
        Long count = hierarchyTypeMapper.selectCount(new LambdaQueryWrapper<HierarchyType>()
            .eq(HierarchyType::getCascadeParentId, bo.getTypeId())
        );
        if (count == 0 && type.getCascadeFlag()) {
            // 最底层，生成完整code - 需要在新事务中处理以确保数据可见性
            updateHierarchyCodeInNewTransaction(bo.getId());
        }

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
            wrapper.eq(Hierarchy::getTypeId, entity.getTypeId());
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

