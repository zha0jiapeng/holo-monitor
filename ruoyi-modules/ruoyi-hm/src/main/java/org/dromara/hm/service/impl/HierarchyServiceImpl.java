package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class HierarchyServiceImpl extends ServiceImpl<HierarchyMapper, Hierarchy> implements IHierarchyService {

    private final HierarchyMapper baseMapper;
    private final HierarchyPropertyMapper hierarchyPropertyMapper;
    private final HierarchyTypeMapper hierarchyTypeMapper;
    private final HierarchyTypePropertyMapper hierarchyTypePropertyMapper;
    private final HierarchyTypePropertyDictMapper hierarchyTypePropertyDictMapper;
    private final TestpointMapper testpointMapper;

    @Override
    public HierarchyVo queryById(Long id) {
        HierarchyVo hierarchyVo = baseMapper.selectVoById(id);
        List<HierarchyPropertyVo> properties = hierarchyPropertyMapper.selectVoList(
            Wrappers.<HierarchyProperty>lambdaQuery().eq(HierarchyProperty::getHierarchyId, id)
        );
        for (HierarchyPropertyVo prop : properties) {
            HierarchyTypePropertyVo typeProp = hierarchyTypePropertyMapper.selectVoById(prop.getTypePropertyId());
            if (typeProp != null) {
                HierarchyTypePropertyDictVo dict = hierarchyTypePropertyDictMapper.selectVoById(typeProp.getPropertyDictId());
                typeProp.setDict(dict);
                prop.setTypeProperty(typeProp);
            }
        }
        hierarchyVo.setProperties(properties);
        return hierarchyVo;
    }

    @Override
    public TableDataInfo<HierarchyVo> queryPageList(HierarchyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Hierarchy> lqw = buildQueryWrapper(bo);
        Page<HierarchyVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if(bo.getNeedProperty()) {
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
    private Map<String, Object> generateHierarchyCode(Long hierarchyId) {
        List<Map<String, Object>> hierarchyCodeInfoList = new ArrayList<>();
        List<Map<String, Object>> configurationList = new ArrayList<>();

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

                        // 获取相关层级的采集配置属性（data_type=1005）
                        List<HierarchyProperty> relatedProperties = hierarchyPropertyMapper.selectList(
                            Wrappers.<HierarchyProperty>lambdaQuery()
                                .eq(HierarchyProperty::getHierarchyId, relatedHierarchyId)
                        );

                        for (HierarchyProperty relatedProperty : relatedProperties) {
                            HierarchyTypeProperty relatedTypeProperty = hierarchyTypePropertyMapper.selectById(relatedProperty.getTypePropertyId());
                            if (relatedTypeProperty != null) {
                                HierarchyTypePropertyDictVo relatedDictVo = hierarchyTypePropertyDictMapper.selectVoById(relatedTypeProperty.getPropertyDictId());
                                if (relatedDictVo != null && relatedDictVo.getDataType().equals(DataTypeEnum.CONFIGURATION.getCode())) {
                                    // 这是一个采集配置属性
                                    Map<String, Object> configInfo = new HashMap<>();
                                    configInfo.put("propertyValue", relatedProperty.getPropertyValue());
                                    configInfo.put("typePropertyId", relatedTypeProperty.getId());
                                    configInfo.put("codeSort", relatedType != null ? (relatedType.getCodeSort() != null ? relatedType.getCodeSort() : 0) : 0);
                                    configurationList.add(configInfo);
                                }
                            }
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

        // 选择code_sort最大的采集配置
        String selectedConfiguration = null;
        Long typePropertyId = null;
        if (!configurationList.isEmpty()) {
            configurationList.sort(Comparator.comparingInt((Map<String, Object> info) -> (Integer) info.get("codeSort")).reversed());
            selectedConfiguration = (String) configurationList.get(0).get("propertyValue");
            typePropertyId = (Long) configurationList.get(0).get("typePropertyId");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", String.join("", codeParts));
        result.put("configuration", selectedConfiguration);
        result.put("typePropertyId", typePropertyId);
        return result;
    }

    /**
     * 查找级联属性并设置父级关系
     *
     * @param properties 属性列表
     * @param hierarchyId 当前层级ID
     * @return 父级层级ID，如果没有找到返回null
     */
    private Long findCascadeParentAndSetRelation(List<HierarchyProperty> properties, Long hierarchyId) {
        for (HierarchyProperty property : properties) {
            HierarchyTypeProperty typeProperty = hierarchyTypePropertyMapper.selectById(property.getTypePropertyId());
            HierarchyTypePropertyDictVo dictVo = hierarchyTypePropertyDictMapper.selectVoById(typeProperty.getPropertyDictId());

            if (dictVo.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
                HierarchyType relatedType = hierarchyTypeMapper.selectById(Long.valueOf(dictVo.getDictValues()));
                if (relatedType != null && relatedType.getCascadeFlag()) {
                    Long parentId = Long.valueOf(property.getPropertyValue());
                    updateHierarchyParent(hierarchyId, parentId);
                    return parentId;
                }
            }
        }
        return null;
    }

    /**
     * 更新层级父级关系
     */
    private void updateHierarchyParent(Long hierarchyId, Long parentId) {
        Hierarchy hierarchy = new Hierarchy();
        hierarchy.setId(hierarchyId);
        hierarchy.setParentId(parentId);
        baseMapper.updateById(hierarchy);
    }

    /**
     * 批量插入属性
     */
    private void batchInsertProperties(List<HierarchyProperty> properties, List<HierarchyProperty> extraProperties) {
        List<HierarchyProperty> allProperties = new ArrayList<>();
        allProperties.addAll(properties);
        allProperties.addAll(extraProperties);

        // 过滤有效属性并批量插入
        List<HierarchyProperty> validProperties = allProperties.stream()
            .filter(p -> p.getHierarchyId() != null)
            .collect(Collectors.toList());

        if (!validProperties.isEmpty()) {
            hierarchyPropertyMapper.insertBatch(validProperties);
        }
    }

    /**
     * 检查是否为最底层
     */
    private boolean isBottomLevel(Long typeId) {
        return hierarchyTypeMapper.selectCount(
            Wrappers.<HierarchyType>lambdaQuery().eq(HierarchyType::getCascadeParentId, typeId)
        ) == 0;
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
        Hierarchy parentHierarchy = baseMapper.selectById(parentHierarchyId);
        if (parentHierarchy == null) return;

        // 获取父级层级类型的所有隐藏属性
        List<HierarchyTypeProperty> parentTypeProperties = hierarchyTypePropertyMapper.selectList(
            Wrappers.<HierarchyTypeProperty>lambdaQuery().eq(HierarchyTypeProperty::getTypeId, parentHierarchy.getTypeId())
        );

        // 创建隐藏属性
        for (HierarchyTypeProperty parentTypeProperty : parentTypeProperties) {
            createHiddenPropertyIfNeeded(currentHierarchyId, parentHierarchyId, parentTypeProperty, extraProperties);
        }

        // 递归处理父级的父级
        if (parentHierarchy.getParentId() != null) {
            createHiddenPropertiesFromParent(currentHierarchyId, parentHierarchy.getParentId(), extraProperties);
        }
    }

    /**
     * 如需要则创建隐藏属性
     */
    private void createHiddenPropertyIfNeeded(Long currentHierarchyId, Long parentHierarchyId,
                                            HierarchyTypeProperty parentTypeProperty, List<HierarchyProperty> extraProperties) {
        HierarchyTypePropertyDictVo dictVo = hierarchyTypePropertyDictMapper.selectVoById(parentTypeProperty.getPropertyDictId());
        if (dictVo == null || !dictVo.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
            return;
        }

        // 查找父级层级的这个属性值
        HierarchyProperty parentProperty = hierarchyPropertyMapper.selectOne(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .eq(HierarchyProperty::getHierarchyId, parentHierarchyId)
                .eq(HierarchyProperty::getTypePropertyId, parentTypeProperty.getId())
        );

        if (parentProperty != null) {
            HierarchyProperty hidden = new HierarchyProperty();
            hidden.setHierarchyId(currentHierarchyId);
            hidden.setTypePropertyId(parentTypeProperty.getId());
            hidden.setPropertyValue(parentProperty.getPropertyValue());
            hidden.setScope(0);
            extraProperties.add(hidden);
        }
    }

    /**
     * 在新事务中更新层级编码并处理采集配置
     * 解决同事务内数据可见性问题
     *
     * @param hierarchyId 层级ID
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateHierarchyCodeInNewTransaction(Long hierarchyId) {
        Map<String, Object> result = generateHierarchyCode(hierarchyId);
        if (result != null) {
            String generatedCode = (String) result.get("code");
            String selectedConfiguration = (String) result.get("configuration");
            Long typePropertyId = (Long) result.get("typePropertyId");

            if (generatedCode != null && !generatedCode.isEmpty()) {
                Hierarchy update = new Hierarchy();
                update.setId(hierarchyId);
                update.setCode(generatedCode);
                baseMapper.updateById(update);
            }

            // 处理采集配置 - 保存为当前层级的一个属性
            if (selectedConfiguration != null && !selectedConfiguration.isEmpty()) {
                saveConfigurationToHierarchyProperty(hierarchyId, selectedConfiguration, typePropertyId);
            }
        }
    }

    /**
     * 保存采集配置到层级属性
     *
     * @param hierarchyId 层级ID
     * @param configuration 采集配置值
     */
    private void saveConfigurationToHierarchyProperty(Long hierarchyId, String configuration, Long typePropertyId) {
        HierarchyTypeProperty typeProperty = hierarchyTypePropertyMapper.selectById(typePropertyId);
        HierarchyTypePropertyDictVo dictVo = hierarchyTypePropertyDictMapper.selectVoById(typeProperty.getPropertyDictId());
        if (dictVo != null && dictVo.getDataType().equals(DataTypeEnum.CONFIGURATION.getCode())) {

            // 找到了采集配置属性定义，检查是否已存在属性值
            HierarchyProperty existingProperty = hierarchyPropertyMapper.selectOne(
                Wrappers.<HierarchyProperty>lambdaQuery()
                    .eq(HierarchyProperty::getHierarchyId, hierarchyId)
                    .eq(HierarchyProperty::getTypePropertyId, typeProperty.getId())
            );

            if (existingProperty != null) {
                // 更新现有属性
                existingProperty.setPropertyValue(configuration);
                hierarchyPropertyMapper.updateById(existingProperty);
            } else {
                // 创建新属性
                HierarchyProperty newProperty = new HierarchyProperty();
                newProperty.setHierarchyId(hierarchyId);
                newProperty.setTypePropertyId(typeProperty.getId());
                newProperty.setPropertyValue(configuration);
                newProperty.setScope(0); // 用户属性
                hierarchyPropertyMapper.insert(newProperty);
            }
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
            initProperty(bo, type);

            // 在主事务外调用新事务方法生成编码
            if (bo.getNeedGenerateCode() != null && bo.getNeedGenerateCode()) {
                updateHierarchyCodeInNewTransaction(bo.getId());
            }
        }
        return flag;
    }

    private void initProperty(HierarchyBo bo, HierarchyType type) {
        List<HierarchyProperty> extraProperties = new ArrayList<>();
        Long hierarchyId = bo.getId();

        // 设置所有用户属性的hierarchyId
        bo.getProperties().forEach(property -> property.setHierarchyId(hierarchyId));

        // 查找级联属性并设置父级关系
        Long parentHierarchyId = findCascadeParentAndSetRelation(bo.getProperties(), hierarchyId);

        // 创建父级的所有隐藏属性
        if (parentHierarchyId != null) {
            createHiddenPropertiesFromParent(hierarchyId, parentHierarchyId, extraProperties);
        }

        // 批量插入属性
        batchInsertProperties(bo.getProperties(), extraProperties);

        // 返回是否需要生成编码的标志
        bo.setNeedGenerateCode(isBottomLevel(bo.getTypeId()) && type.getCascadeFlag());
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

    @Override
    public List<HierarchyVo> getDescendantsByType(Long hierarchyId, Long targetTypeId) {
        List<HierarchyVo> result = new ArrayList<>();

        // 递归获取所有子孙层级
        getAllDescendants(hierarchyId, result);

        // 过滤出指定类型的层级
        return result.stream()
                .filter(hierarchy -> targetTypeId.equals(hierarchy.getTypeId()))
                .collect(Collectors.toList());
    }

    /**
     * 递归获取所有子孙层级
     *
     * @param parentId 父级层级ID
     * @param result 结果列表
     */
    private void getAllDescendants(Long parentId, List<HierarchyVo> result) {
        List<HierarchyVo> children = getChildrenByParentId(parentId);
        for (HierarchyVo child : children) {
            result.add(child);
            // 递归获取子级的子级
            getAllDescendants(child.getId(), result);
        }
    }

    @Override
    public List<HierarchyVo> getBottomLevelWithConfiguration() {
        // 1. 获取所有层级类型
        List<HierarchyType> allTypes = hierarchyTypeMapper.selectList(null);

        // 2. 找出最底层的层级类型（没有其他类型以它作为级联父级）
        List<Long> bottomLevelTypeIds = allTypes.stream()
            .filter(type -> isBottomLevel(type.getId()))
            .map(HierarchyType::getId)
            .collect(Collectors.toList());

        if (bottomLevelTypeIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 获取最底层类型的层级数据
        List<Hierarchy> bottomHierarchies = baseMapper.selectList(
            Wrappers.<Hierarchy>lambdaQuery()
                .in(Hierarchy::getTypeId, bottomLevelTypeIds)
        );

        if (bottomHierarchies.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> hierarchyIds = bottomHierarchies.stream()
            .map(Hierarchy::getId)
            .collect(Collectors.toList());

        // 4. 获取这些层级的属性，筛选出有采集配置(1005)的层级
        List<HierarchyProperty> allProperties = hierarchyPropertyMapper.selectList(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .in(HierarchyProperty::getHierarchyId, hierarchyIds)
        );

        // 5. 获取所有相关的类型属性信息
        Set<Long> typePropertyIds = allProperties.stream()
            .map(HierarchyProperty::getTypePropertyId)
            .collect(Collectors.toSet());

        List<HierarchyTypeProperty> typeProperties = hierarchyTypePropertyMapper.selectList(
            Wrappers.<HierarchyTypeProperty>lambdaQuery()
                .in(HierarchyTypeProperty::getId, typePropertyIds)
        );

        // 6. 获取属性字典信息
        Set<Long> dictIds = typeProperties.stream()
            .map(HierarchyTypeProperty::getPropertyDictId)
            .collect(Collectors.toSet());

        List<HierarchyTypePropertyDict> dicts = hierarchyTypePropertyDictMapper.selectList(
            Wrappers.<HierarchyTypePropertyDict>lambdaQuery()
                .in(HierarchyTypePropertyDict::getId, dictIds)
        );

        // 7. 筛选出具有采集配置(1005)属性的层级
        Set<Long> hierarchyWithConfigIds = new HashSet<>();
        for (HierarchyProperty property : allProperties) {
            HierarchyTypeProperty typeProperty = typeProperties.stream()
                .filter(tp -> tp.getId().equals(property.getTypePropertyId()))
                .findFirst().orElse(null);

            if (typeProperty != null) {
                HierarchyTypePropertyDict dict = dicts.stream()
                    .filter(d -> d.getId().equals(typeProperty.getPropertyDictId()))
                    .findFirst().orElse(null);

                if (dict != null && dict.getDataType().equals(DataTypeEnum.CONFIGURATION.getCode())) {
                    hierarchyWithConfigIds.add(property.getHierarchyId());
                }
            }
        }

        // 8. 转换为VO对象并填充属性信息
        List<HierarchyVo> result = new ArrayList<>();
        for (Hierarchy hierarchy : bottomHierarchies) {
            if (hierarchyWithConfigIds.contains(hierarchy.getId())) {
                HierarchyVo vo = baseMapper.selectVoById(hierarchy.getId());
                List<HierarchyPropertyVo> properties = hierarchyPropertyMapper.selectVoList(
                    Wrappers.<HierarchyProperty>lambdaQuery().eq(HierarchyProperty::getHierarchyId, hierarchy.getId())
                );

                // 为属性填充类型信息
                for (HierarchyPropertyVo prop : properties) {
                    HierarchyTypePropertyVo typeProp = hierarchyTypePropertyMapper.selectVoById(prop.getTypePropertyId());
                    if (typeProp != null) {
                        HierarchyTypePropertyDictVo dict = hierarchyTypePropertyDictMapper.selectVoById(typeProp.getPropertyDictId());
                        typeProp.setDict(dict);
                        prop.setTypeProperty(typeProp);
                    }
                }
                vo.setProperties(properties);
                result.add(vo);
            }
        }

        return result;
    }

}

