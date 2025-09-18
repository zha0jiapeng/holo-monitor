package org.dromara.hm.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.*;
import org.dromara.hm.domain.bo.HierarchyBo;
import org.dromara.hm.domain.template.HierarchyExcelTemplate;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.domain.vo.HierarchyTreeVo;
import org.dromara.hm.enums.DataTypeEnum;
import org.dromara.hm.mapper.*;
import org.dromara.hm.service.IHierarchyService;
import org.dromara.hm.utils.HierarchyCodeUtils;
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
@Slf4j
@RequiredArgsConstructor
@Service
public class HierarchyServiceImpl extends ServiceImpl<HierarchyMapper, Hierarchy> implements IHierarchyService {

    private final HierarchyMapper baseMapper;
    private final HierarchyPropertyMapper hierarchyPropertyMapper;
    private final HierarchyTypeMapper hierarchyTypeMapper;
    private final HierarchyTypePropertyMapper hierarchyTypePropertyMapper;
    private final HierarchyTypePropertyDictMapper hierarchyTypePropertyDictMapper;

    @Override
    public HierarchyVo queryById(Long id, boolean needProperty) {
        HierarchyVo hierarchyVo = baseMapper.selectVoById(id);
        if(hierarchyVo==null) return null;
        if(needProperty) {
            List<HierarchyPropertyVo> properties = hierarchyPropertyMapper.selectVoList(
                Wrappers.<HierarchyProperty>lambdaQuery().eq(HierarchyProperty::getHierarchyId, id)
            );
            initProperty(properties,hierarchyVo);
        }
        return hierarchyVo;
    }

    @Override
    public List<HierarchyVo> queryByIds(List<Long> ids, boolean needProperty) {
        List<HierarchyVo> hierarchyVo = baseMapper.selectVoByIds(ids);
        if(needProperty) {
            for (HierarchyVo vo : hierarchyVo) {
                List<HierarchyPropertyVo> properties = hierarchyPropertyMapper.selectVoList(
                    Wrappers.<HierarchyProperty>lambdaQuery().eq(HierarchyProperty::getHierarchyId, vo.getId())
                );
                initProperty(properties,vo);
            }

        }
        return hierarchyVo;
    }


    @Override
    public TableDataInfo<HierarchyVo> queryPageList(HierarchyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Hierarchy> lqw = buildQueryWrapper(bo);
        Page<HierarchyVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if(bo.getNeedProperty()) {
            for (HierarchyVo vo : result.getRecords()) {
                List<HierarchyPropertyVo> properties = hierarchyPropertyMapper.selectVoList(
                    Wrappers.<HierarchyProperty>lambdaQuery().eq(HierarchyProperty::getHierarchyId, vo.getId()).eq(HierarchyProperty::getScope,1)
                );
                initProperty(properties,vo);
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
     * 调用工具类方法生成编码
     *
     * @param hierarchyId 层级ID
     * @return 生成的编码字符串
     */
    private Map<String, Object> generateHierarchyCode(Long hierarchyId) {
        return HierarchyCodeUtils.generateHierarchyCode(hierarchyId, baseMapper, hierarchyPropertyMapper,
                hierarchyTypeMapper, hierarchyTypePropertyMapper, hierarchyTypePropertyDictMapper);
    }


    /**
     * 查找级联属性并设置父级关系
     *
     * @param properties 属性列表
     * @param hierarchyId 当前层级ID
     * @return 父级层级ID，如果没有找到返回null
     */
    private Map<String,Object> findCascadeParentAndSetRelation(List<HierarchyProperty> properties, Long hierarchyId) {
        Map<String,Object> map = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (HierarchyProperty property : properties) {
            HierarchyTypeProperty typeProperty = hierarchyTypePropertyMapper.selectById(property.getTypePropertyId());
            HierarchyTypePropertyDictVo dictVo = hierarchyTypePropertyDictMapper.selectVoById(typeProperty.getPropertyDictId());

            if (dictVo.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
                HierarchyType relatedType = hierarchyTypeMapper.selectById(Long.valueOf(dictVo.getDictValues()));
                Long parentId = Long.valueOf(property.getPropertyValue());
                if (relatedType != null && relatedType.getCascadeFlag()) {
                    updateHierarchyParent(hierarchyId, parentId);
                    map.put("parentId", parentId);
                }else{
                    ids.add(parentId);
                }
                map.put("otherIds", ids);
            }
        }
        return map;
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
        // 先获取当前类型信息
        HierarchyType currentType = hierarchyTypeMapper.selectById(typeId);
        if (currentType != null && currentType.getTypeKey() != null) {
            // 如果是device_point或sensor类型，直接返回true
            if ("device_point".equals(currentType.getTypeKey()) || "sensor".equals(currentType.getTypeKey())) {
                return true;
            }
        }
        return false;
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
        List<HierarchyTypeProperty> parentTypeProperties = hierarchyTypePropertyMapper.selectList(
            Wrappers.<HierarchyTypeProperty>lambdaQuery().eq(HierarchyTypeProperty::getTypeId, parentHierarchy.getTypeId())
        );
        for (HierarchyTypeProperty parentTypeProperty : parentTypeProperties) {
            createHiddenPropertyIfNeeded(currentHierarchyId, parentHierarchyId, parentTypeProperty, extraProperties);
        }
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
        // 首先检查当前层级是否有编码，如果没有则先生成单层编码
        Hierarchy currentHierarchy = baseMapper.selectById(hierarchyId);
        if (currentHierarchy != null && (currentHierarchy.getCode() == null || currentHierarchy.getCode().isEmpty())) {
            // 获取层级类型信息
            HierarchyType type = hierarchyTypeMapper.selectById(currentHierarchy.getTypeId());
            if (type != null && type.getCodeLength() != null) {
                // 生成单层编码
                String singleCode = HierarchyCodeUtils.generateNextCode(currentHierarchy.getTypeId(), currentHierarchy.getParentId(), type.getCodeLength(), baseMapper);
                if (singleCode != null) {
                    Hierarchy updateSingle = new Hierarchy();
                    updateSingle.setId(hierarchyId);
                    updateSingle.setCode(singleCode);
                    baseMapper.updateById(updateSingle);
                }
            }
        }

        // 然后生成完整编码
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
        Integer codeLength = type.getCodeLength();
        if(code!=null&&!codeLength.equals(code.length())) {
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
            } else if(bo.getCode() == null){
                // 如果不需要生成完整编码，但编码为空，则生成单层编码
                String generatedCode = HierarchyCodeUtils.generateNextCode(bo.getTypeId(), bo.getParentId(), type.getCodeLength(), baseMapper);
                if (generatedCode != null) {
                    bo.setCode(generatedCode);
                    // 更新数据库中的编码
                    Hierarchy updateEntity = new Hierarchy();
                    updateEntity.setId(bo.getId());
                    updateEntity.setCode(generatedCode);
                    baseMapper.updateById(updateEntity);
                }
            }
        }
        return flag;
    }

    private void initProperty(HierarchyBo bo, HierarchyType type) {
        List<HierarchyProperty> extraProperties = new ArrayList<>();
        Long hierarchyId = bo.getId();
        bo.getProperties().forEach(property -> property.setHierarchyId(hierarchyId));
        Map<String, Object> cascadeParentAndSetRelation = findCascadeParentAndSetRelation(bo.getProperties(), hierarchyId);
        Object parentIdObj = cascadeParentAndSetRelation.get("parentId");
        Object otherIdsObj = cascadeParentAndSetRelation.get("otherIds");
        if (parentIdObj != null) {
            Long parentId = Long.valueOf(parentIdObj.toString());
            List<Long> otherIds = (List<Long>) otherIdsObj;
            otherIds.add(parentId);
            for (Long otherId : otherIds) {
                createHiddenPropertiesFromParent(hierarchyId, otherId, extraProperties);
            }
            bo.setParentId(parentId);
        }
        batchInsertProperties(bo.getProperties(), extraProperties);
        bo.setNeedGenerateCode(isBottomLevel(bo.getTypeId()) && type.getCascadeFlag());
    }

    @Override
    @Transactional()
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
       // if (StringUtils.isNotBlank(entity.getName())) {
//            LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
//            wrapper.eq(Hierarchy::getName, entity.getName());
//            wrapper.eq(Hierarchy::getTypeId, entity.getTypeId());
//            if (entity.getId() != null) {
//                wrapper.ne(Hierarchy::getId, entity.getId());
//            }
//            if (baseMapper.exists(wrapper)) {
//                throw new ServiceException("层级名称已存在");
//            }
        //}

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
            HierarchyType sensor = hierarchyTypeMapper.selectOne(new LambdaQueryWrapper<HierarchyType>().eq(HierarchyType::getTypeKey, "sensor"));
            HierarchyTypePropertyDict sensorDevice = hierarchyTypePropertyDictMapper.selectOne(new LambdaQueryWrapper<HierarchyTypePropertyDict>().eq(HierarchyTypePropertyDict::getDictKey, "sensor_device"));
            for (Hierarchy hierarchy : list) {
                List<HierarchyProperty> hierarchyProperties = hierarchyPropertyMapper.selectList(new LambdaQueryWrapper<HierarchyProperty>()
                    .eq(HierarchyProperty::getPropertyValue, hierarchy.getId().toString())
                );
                for (HierarchyProperty hierarchyProperty : hierarchyProperties) {
                    HierarchyTypeProperty hierarchyDictTypeProperty = hierarchyTypePropertyMapper.selectById(hierarchyProperty.getTypePropertyId());
                    if(hierarchyDictTypeProperty!=null){
                        HierarchyTypePropertyDict hierarchyDict = hierarchyTypePropertyDictMapper.selectById(hierarchyDictTypeProperty.getPropertyDictId());
                        if(hierarchyDict!=null && hierarchyDict.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())){
                            throw new ServiceException("层级已绑定子层级，无法删除");
                        }
                    }
                }

                if(hierarchy.getTypeId().equals(sensor.getId())){
                     HierarchyTypeProperty hierarchyTypeProperty = hierarchyTypePropertyMapper.selectOne(new LambdaQueryWrapper<HierarchyTypeProperty>()
                        .eq(HierarchyTypeProperty::getPropertyDictId, sensorDevice.getId())
                        .eq(HierarchyTypeProperty::getTypeId, hierarchy.getTypeId())
                    );
                    HierarchyProperty hierarchyProperty = hierarchyPropertyMapper.selectOne(
                        new LambdaQueryWrapper<HierarchyProperty>()
                            .eq(HierarchyProperty::getHierarchyId, hierarchy.getId())
                            .eq(HierarchyProperty::getTypePropertyId, hierarchyTypeProperty.getId())
                    );
                    if(hierarchyProperty!=null){
                        throw new ServiceException("传感器已绑定设备，无法删除");
                    }
                }
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

        // 3. 获取最底层类型的层级数据
        List<Hierarchy> bottomHierarchies = baseMapper.selectList(
            Wrappers.<Hierarchy>lambdaQuery()
                .eq(Hierarchy::getTypeId, 19)
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
                initProperty(properties,vo);
                result.add(vo);
            }
        }

        return result;
    }

    @Override
    public List<Long> selectChildHierarchyIds(Long hierarchyId) {
        return baseMapper.selectChildHierarchyIds(hierarchyId);
    }

    @Override
    public List<Map<String,String>> selectTargetTypeHierarchyList(List<Long> ids, Long targetTypeId) {
        return baseMapper.selectTargetTypeHierarchyList(ids,targetTypeId);
    }

    @Override
    public List<HierarchyVo> selectByIds(List<Long> matchedIds,boolean needProperty) {
        List<HierarchyVo> hierarchies = baseMapper.selectVoByIds(matchedIds);
        if(needProperty) {
            for (HierarchyVo hierarchy : hierarchies) {
                List<HierarchyPropertyVo> properties = hierarchyPropertyMapper.selectVoList(
                    Wrappers.<HierarchyProperty>lambdaQuery().eq(HierarchyProperty::getHierarchyId, hierarchy.getId())
                );
                initProperty(properties, hierarchy);
            }
        }
        return hierarchies;
    }

    @Override
    public List<HierarchyVo> selectByIds(List<Long> matchedIds, List<String> diceNames) {
        List<HierarchyVo> hierarchies = baseMapper.selectVoByIds(matchedIds);
        if(diceNames!=null) {
            List<HierarchyTypePropertyDictVo> dicts = hierarchyTypePropertyDictMapper.selectVoList(
                new LambdaQueryWrapper<HierarchyTypePropertyDict>().in(HierarchyTypePropertyDict::getDictKey, diceNames)
            );
            List<Long> idList = dicts.stream()
                .map(HierarchyTypePropertyDictVo::getId)
                .toList();
            for (HierarchyVo hierarchy : hierarchies) {
                List<HierarchyTypePropertyVo> hierarchyTypePropertyVos = hierarchyTypePropertyMapper.selectVoList(new LambdaQueryWrapper<HierarchyTypeProperty>()
                    .in(HierarchyTypeProperty::getPropertyDictId, idList)
                    .eq(HierarchyTypeProperty::getTypeId, hierarchy.getTypeId())
                );
                List<Long> ids = hierarchyTypePropertyVos.stream()
                    .map(HierarchyTypePropertyVo::getId)
                    .toList();
                List<HierarchyPropertyVo> hierarchyPropertyVos = hierarchyPropertyMapper.selectVoList(new LambdaQueryWrapper<HierarchyProperty>()
                    .in(HierarchyProperty::getTypePropertyId, ids)
                    .eq(HierarchyProperty::getHierarchyId, hierarchy.getId())
                );
                for (HierarchyPropertyVo hierarchyPropertyVo : hierarchyPropertyVos) {
                    for (HierarchyTypePropertyVo hierarchyTypePropertyVo : hierarchyTypePropertyVos) {
                        if(hierarchyTypePropertyVo.getId().equals(hierarchyPropertyVo.getTypePropertyId())) {
                            hierarchyPropertyVo.setTypeProperty(hierarchyTypePropertyVo);
                            break;
                        }
                    }
                }
                for (HierarchyTypePropertyVo hierarchyTypePropertyVo : hierarchyTypePropertyVos) {
                    for (HierarchyTypePropertyDictVo dict : dicts) {
                        if(dict.getId().equals(hierarchyTypePropertyVo.getPropertyDictId())){
                            hierarchyTypePropertyVo.setDict(dict);
                            break;
                        }
                    }
                }
                hierarchy.setProperties(hierarchyPropertyVos);
            }
        }
        return hierarchies;
    }


    @Override
    public List<HierarchyVo> getSensorListByDeviceId(Long hierarchyId,boolean showAllFlag) {
        List<HierarchyVo> list = new ArrayList<>();
        HierarchyVo hierarchy = queryById(hierarchyId,true);
        if(hierarchy.isHaveSensorFlag()){
            String sensorStrs = "";
            for (HierarchyPropertyVo property : hierarchy.getProperties()) {
                if(property.getTypeProperty().getDict().getDataType().equals(DataTypeEnum.ASSOCIATION.getCode())){
                    sensorStrs = property.getPropertyValue();
                    break;
                }
            }
            String[] split = sensorStrs.split("\\,");
            for (String sensorIdStr : split) {
                HierarchyVo hierarchyVo = queryById(Long.valueOf(sensorIdStr), false);
                if(hierarchyVo!=null) {
                    HierarchyTypePropertyDict sensorLocation = hierarchyTypePropertyDictMapper.selectOne(new LambdaQueryWrapper<HierarchyTypePropertyDict>().eq(HierarchyTypePropertyDict::getDictKey, "sensor_location"));
                    if(sensorLocation==null) continue;
                    HierarchyTypeProperty hierarchyTypeProperty = hierarchyTypePropertyMapper.selectOne(new LambdaQueryWrapper<HierarchyTypeProperty>().eq(HierarchyTypeProperty::getPropertyDictId, sensorLocation.getId()));
                    if(hierarchyTypeProperty==null) continue;
                    HierarchyPropertyVo hierarchyProperties = hierarchyPropertyMapper.selectVoOne(
                        new LambdaQueryWrapper<HierarchyProperty>()
                            .eq(HierarchyProperty::getHierarchyId, Long.valueOf(sensorIdStr))
                            .eq(HierarchyProperty::getTypePropertyId, hierarchyTypeProperty.getId())
                    );
                    if(hierarchyProperties==null) continue;
                    hierarchyVo.setProperties(List.of(hierarchyProperties));

                    List<HierarchyTypePropertyDict> hierarchyTypePropertyDicts = hierarchyTypePropertyDictMapper.selectList(new LambdaQueryWrapper<HierarchyTypePropertyDict>().eq(HierarchyTypePropertyDict::getSystemFlag, 1));
                    List<String> tags = hierarchyTypePropertyDicts.stream().map(item -> item.getDictKey()).toList();
                    //List<String> tags = List.of("sys:cs", "mont/pd/mag", "mont/pd/au", "sys:st");
                    JSONObject entries = SD400MPUtils.testpointFind(hierarchyVo.getCode());
                    if (entries.getInt("code") == 200) {
                        String id = entries.getJSONObject("data").getStr("id");
                        JSONObject data = SD400MPUtils.data(Long.valueOf(id), tags, null);
                        if (data.getInt("code") == 200) {
                            Object online = data.getByPath("data.groups[0].online");
                            if (online == null) continue;
                            JSONArray onlines = (JSONArray) online;
                            boolean addFlag = true;
                            for (Object o : onlines) {
                                JSONObject item = (JSONObject) o;
                                if(!showAllFlag && "sys:st".equals(item.getStr("key"))){
                                    String val = item.getStr("val");
                                    if(!val.equals("0")){
                                        addFlag = false;
                                    }
                                }
                            }
                            if(addFlag) hierarchyVo.setDataSet(onlines);
                        }
                    }
                    list.add(hierarchyVo);
                }
            }
        }
        return list;
    }

    @Override
    public List<HierarchyVo> getAllSensorsWithConfiguration() {
        // 1. 获取所有具有采集配置(dataType=1005)的属性字典
        List<HierarchyTypePropertyDict> configDicts = hierarchyTypePropertyDictMapper.selectList(
            Wrappers.<HierarchyTypePropertyDict>lambdaQuery()
                .eq(HierarchyTypePropertyDict::getDataType, DataTypeEnum.CONFIGURATION.getCode())
        );

        if (configDicts.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 获取这些字典对应的类型属性关联
        Set<Long> dictIds = configDicts.stream()
            .map(HierarchyTypePropertyDict::getId)
            .collect(Collectors.toSet());

        List<HierarchyTypeProperty> typeProperties = hierarchyTypePropertyMapper.selectList(
            Wrappers.<HierarchyTypeProperty>lambdaQuery()
                .in(HierarchyTypeProperty::getPropertyDictId, dictIds)
        );

        if (typeProperties.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 获取这些类型属性对应的层级属性
        Set<Long> typePropertyIds = typeProperties.stream()
            .map(HierarchyTypeProperty::getId)
            .collect(Collectors.toSet());

        List<HierarchyProperty> properties = hierarchyPropertyMapper.selectList(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .in(HierarchyProperty::getTypePropertyId, typePropertyIds)
        );

        if (properties.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 获取具有这些属性的层级ID
        Set<Long> hierarchyIds = properties.stream()
            .map(HierarchyProperty::getHierarchyId)
            .collect(Collectors.toSet());

        // 5. 获取这些层级的详细信息
        List<Hierarchy> hierarchies = baseMapper.selectList(
            Wrappers.<Hierarchy>lambdaQuery().in(Hierarchy::getId, hierarchyIds)
        );

        // 6. 转换为VO对象并填充属性信息
        List<HierarchyVo> result = new ArrayList<>();
        for (Hierarchy hierarchy : hierarchies) {
            HierarchyVo vo = baseMapper.selectVoById(hierarchy.getId());

            // 获取该层级的属性信息
            List<HierarchyPropertyVo> hierarchyProperties = hierarchyPropertyMapper.selectVoList(
                Wrappers.<HierarchyProperty>lambdaQuery().eq(HierarchyProperty::getHierarchyId, hierarchy.getId())
            );

            // 为属性填充类型信息
            initProperty(hierarchyProperties, vo);
            result.add(vo);
        }

        return result;
    }

    @Override
    public Map<String, List<HierarchyVo>> sensorList(Long parentId, Long hierarchyId) {


        // 1. 获取传感器类型ID（type_key = "sensor"）
        HierarchyType sensorType = hierarchyTypeMapper.selectOne(
            Wrappers.<HierarchyType>lambdaQuery()
                .eq(HierarchyType::getTypeKey, "sensor")
        );
        if (sensorType == null) {
            Map<String, List<HierarchyVo>> result = new HashMap<>();
            result.put("unbound", new ArrayList<>());
            result.put("bound", new ArrayList<>());
            return result;
        }

        // 2. 递归查询指定层级下所有传感器类型的子孙层级
        List<HierarchyVo> allSensors = getDescendantsByType(parentId, sensorType.getId());

        // 3. 获取sensor_device属性字典
        HierarchyTypePropertyDict sensorDeviceDict = hierarchyTypePropertyDictMapper.selectOne(
            Wrappers.<HierarchyTypePropertyDict>lambdaQuery()
                .eq(HierarchyTypePropertyDict::getDictKey, "sensor_device")
        );

        if (sensorDeviceDict == null) {
            // 如果没有sensor_device字典，则所有传感器都算未绑定
            Map<String, List<HierarchyVo>> result = new HashMap<>();
            result.put("unbound", allSensors);
            result.put("bound", new ArrayList<>());
            return result;
        }

        // 4. 获取传感器类型对应的sensor_device属性定义
        List<HierarchyTypeProperty> sensorDeviceTypeProperties = hierarchyTypePropertyMapper.selectList(
            Wrappers.<HierarchyTypeProperty>lambdaQuery()
                .eq(HierarchyTypeProperty::getTypeId, sensorType.getId())
                .eq(HierarchyTypeProperty::getPropertyDictId, sensorDeviceDict.getId())
        );

        if (sensorDeviceTypeProperties.isEmpty()) {
            // 如果传感器类型没有sensor_device属性定义，则所有传感器都算未绑定
            Map<String, List<HierarchyVo>> result = new HashMap<>();
            result.put("unbound", allSensors);
            result.put("bound", new ArrayList<>());
            return result;
        }

        // 5. 获取所有传感器ID
        List<Long> sensorIds = allSensors.stream()
            .map(HierarchyVo::getId)
            .collect(Collectors.toList());

        if (sensorIds.isEmpty()) {
            Map<String, List<HierarchyVo>> result = new HashMap<>();
            result.put("unbound", allSensors);
            result.put("bound", new ArrayList<>());
            return result;
        }

        // 6. 查找已绑定sensor_device属性的传感器ID
        List<Long> typePropertyIds = sensorDeviceTypeProperties.stream()
            .map(HierarchyTypeProperty::getId)
            .collect(Collectors.toList());

        List<HierarchyProperty> boundProperties = hierarchyPropertyMapper.selectList(
            Wrappers.<HierarchyProperty>lambdaQuery()
                .in(HierarchyProperty::getHierarchyId, sensorIds)
                .in(HierarchyProperty::getTypePropertyId, typePropertyIds)
                .isNotNull(HierarchyProperty::getPropertyValue)
                .ne(HierarchyProperty::getPropertyValue, "")
        );

        Set<Long> boundSensorIds = boundProperties.stream()
            .map(HierarchyProperty::getHierarchyId)
            .collect(Collectors.toSet());

        // 7. 筛选出未绑定的传感器
        List<HierarchyVo> unboundSensors = allSensors.stream()
            .filter(sensor -> !boundSensorIds.contains(sensor.getId()))
            .collect(Collectors.toList());

        // 8. 查询当前层级已绑定的传感器（用于回显）
        List<HierarchyVo> boundSensors = new ArrayList<>();
        if (hierarchyId != null) {
            // 查询当前层级绑定的传感器ID
            List<HierarchyProperty> currentBoundProperties = hierarchyPropertyMapper.selectList(
                Wrappers.<HierarchyProperty>lambdaQuery()
                    .eq(HierarchyProperty::getPropertyValue, hierarchyId)
                    .in(HierarchyProperty::getTypePropertyId, typePropertyIds)
                    .isNotNull(HierarchyProperty::getPropertyValue)
                    .ne(HierarchyProperty::getPropertyValue, "")
            );
            List<Long> list = currentBoundProperties.stream().map(HierarchyProperty::getHierarchyId).toList();

            if (!currentBoundProperties.isEmpty()) {
                boundSensors = queryByIds(list,false);
                List<String> dictKeys = Arrays.asList("sensor_location");
                addPropertiesByDictKeys(boundSensors, dictKeys);
            }
        }
        Map<String, List<HierarchyVo>> result = new HashMap<>();
        result.put("unbound", unboundSensors);
        result.put("bound", boundSensors);
        return result;
    }

    /**
     * 为层级列表添加指定字典key的属性信息
     *
     * @param hierarchyVos 层级VO列表
     * @param dictKeys 字典key列表
     */
    private void addPropertiesByDictKeys(List<HierarchyVo> hierarchyVos, List<String> dictKeys) {
        if (hierarchyVos == null || hierarchyVos.isEmpty() || dictKeys == null || dictKeys.isEmpty()) {
            return;
        }

        try {
            // 1. 根据dictKeys查找对应的属性字典
            List<HierarchyTypePropertyDict> dicts = hierarchyTypePropertyDictMapper.selectList(
                Wrappers.<HierarchyTypePropertyDict>lambdaQuery()
                    .in(HierarchyTypePropertyDict::getDictKey, dictKeys)
            );

            if (dicts.isEmpty()) {
                log.debug("未找到dictKeys对应的属性字典: {}", dictKeys);
                return;
            }

            // 2. 获取所有层级的类型ID
            Set<Long> typeIds = hierarchyVos.stream()
                .map(HierarchyVo::getTypeId)
                .collect(Collectors.toSet());

            // 3. 获取字典ID列表
            List<Long> dictIds = dicts.stream()
                .map(HierarchyTypePropertyDict::getId)
                .collect(Collectors.toList());

            // 4. 根据typeId和dictId查找类型属性
            List<HierarchyTypeProperty> typeProperties = hierarchyTypePropertyMapper.selectList(
                Wrappers.<HierarchyTypeProperty>lambdaQuery()
                    .in(HierarchyTypeProperty::getTypeId, typeIds)
                    .in(HierarchyTypeProperty::getPropertyDictId, dictIds)
            );

            if (typeProperties.isEmpty()) {
                log.debug("未找到对应的类型属性");
                return;
            }

            // 5. 建立字典映射
            Map<Long, HierarchyTypePropertyDict> dictMap = dicts.stream()
                .collect(Collectors.toMap(HierarchyTypePropertyDict::getId, d -> d));

            // 6. 建立类型属性映射 (typeId -> List<HierarchyTypeProperty>)
            Map<Long, List<HierarchyTypeProperty>> typeToPropertiesMap = typeProperties.stream()
                .collect(Collectors.groupingBy(HierarchyTypeProperty::getTypeId));

            // 7. 获取所有层级ID
            List<Long> hierarchyIds = hierarchyVos.stream()
                .map(HierarchyVo::getId)
                .collect(Collectors.toList());

            // 8. 获取类型属性ID列表
            List<Long> typePropertyIds = typeProperties.stream()
                .map(HierarchyTypeProperty::getId)
                .collect(Collectors.toList());

            // 9. 查找所有相关的属性值（使用selectVoList确保主键被正确查询）
            List<HierarchyPropertyVo> hierarchyProperties = hierarchyPropertyMapper.selectVoList(
                Wrappers.<HierarchyProperty>lambdaQuery()
                    .in(HierarchyProperty::getHierarchyId, hierarchyIds)
                    .in(HierarchyProperty::getTypePropertyId, typePropertyIds)
            );

            // 10. 建立层级ID到属性值的映射
            Map<Long, List<HierarchyPropertyVo>> hierarchyToPropertiesMap = hierarchyProperties.stream()
                .collect(Collectors.groupingBy(HierarchyPropertyVo::getHierarchyId));

            // 11. 循环每个hierarchyVo，为其添加属性信息
            for (HierarchyVo hierarchyVo : hierarchyVos) {
                // 获取该类型的类型属性
                List<HierarchyTypeProperty> typeProps = typeToPropertiesMap.get(hierarchyVo.getTypeId());
                if (typeProps == null || typeProps.isEmpty()) {
                    continue;
                }

                // 获取该层级的属性值
                List<HierarchyPropertyVo> hierarchyProps = hierarchyToPropertiesMap.get(hierarchyVo.getId());

                // 初始化properties列表（如果为空）
                if (hierarchyVo.getProperties() == null) {
                    hierarchyVo.setProperties(new ArrayList<>());
                }

                // 为每个类型属性查找对应的属性值
                for (HierarchyTypeProperty typeProperty : typeProps) {
                    HierarchyPropertyVo propertyVo = null;

                    // 查找是否已存在该属性
                    if (hierarchyProps != null) {
                        Optional<HierarchyPropertyVo> foundProperty = hierarchyProps.stream()
                            .filter(hp -> hp.getTypePropertyId().equals(typeProperty.getId()))
                            .findFirst();
                        if (foundProperty.isPresent()) {
                            propertyVo = foundProperty.get();
                        }
                    }

                    // 只有在数据库中找到对应属性时，才添加到结果中
                    if (propertyVo != null) {
                        // 创建类型属性VO
                        HierarchyTypePropertyVo typePropertyVo = new HierarchyTypePropertyVo();
                        typePropertyVo.setId(typeProperty.getId());
                        typePropertyVo.setTypeId(typeProperty.getTypeId());
                        typePropertyVo.setPropertyDictId(typeProperty.getPropertyDictId());
                        typePropertyVo.setRequired(typeProperty.getRequired());

                        // 创建字典VO
                        HierarchyTypePropertyDict dict = dictMap.get(typeProperty.getPropertyDictId());
                        if (dict != null) {
                            HierarchyTypePropertyDictVo dictVo = new HierarchyTypePropertyDictVo();
                            dictVo.setId(dict.getId());
                            dictVo.setDictName(dict.getDictName());
                            dictVo.setDataType(dict.getDataType());
                            dictVo.setDictValues(dict.getDictValues());
                            dictVo.setSystemFlag(dict.getSystemFlag());
                            dictVo.setDictKey(dict.getDictKey());

                            typePropertyVo.setDict(dictVo);
                        }

                        propertyVo.setTypeProperty(typePropertyVo);
                        hierarchyVo.getProperties().add(propertyVo);
                    }
                }
            }

            log.debug("成功为{}个层级添加了{}个字典key的属性信息", hierarchyVos.size(), dictKeys.size());

        } catch (Exception e) {
            log.error("添加属性信息时发生异常，dictKeys: {}", dictKeys, e);
        }
    }

    @Override
    public JSONObject getLocationByHierarchyId(Long hierarchyId) {
        HierarchyVo hierarchyVo = queryById(hierarchyId,true);
        if (hierarchyVo != null) {
            List<HierarchyPropertyVo> properties = hierarchyVo.getProperties();
            for (HierarchyPropertyVo property : properties) {
                HierarchyTypePropertyDictVo dict = property.getTypeProperty().getDict();
                if(dict.getDataType().equals(DataTypeEnum.LOCATION_ID.getCode())){
                    String fileId = property.getPropertyValue();
                    Map<String, Object> result = new HashMap<>();
                    result.put("id",fileId);
                    JSONObject entries = SD400MPUtils.locationResult(result);
                    return entries;
                }
            }
        }
        return null;
    }

    @Override
    public JSONObject downloadTemplate(Long typeId) {
        //TODO 根据typeId生成表头
        return null;
    }


    private void initProperty(List<HierarchyPropertyVo> properties,HierarchyVo hierarchyVo) {
        for (HierarchyPropertyVo prop : properties) {
            HierarchyTypePropertyVo typeProp = hierarchyTypePropertyMapper.selectVoById(prop.getTypePropertyId());
            if (typeProp != null) {
                HierarchyTypePropertyDictVo dict = hierarchyTypePropertyDictMapper.selectVoById(typeProp.getPropertyDictId());
                typeProp.setDict(dict);
                prop.setTypeProperty(typeProp);
                if (dict.getDataType().equals(DataTypeEnum.HIERARCHY.getCode())) {
                    prop.setHierarchyName(baseMapper.selectById(Long.valueOf(prop.getPropertyValue())).getName());
                }
                if (dict.getDataType().equals(DataTypeEnum.ASSOCIATION.getCode())) {
                    hierarchyVo.setHaveSensorFlag(true);
                }
            }
        }
        hierarchyVo.setProperties(properties);
    }









    @Override
    public Long getIdByNameAndType(String name, Long typeId) {
        List<Hierarchy> list = lambdaQuery()
            .eq(Hierarchy::getName, name)
            .eq(Hierarchy::getTypeId, typeId)
            .list();
        if (list.isEmpty()) {
            throw new RuntimeException("No hierarchy found for name: " + name + " and typeId: " + typeId);
        } else if (list.size() > 1) {
            throw new RuntimeException("Multiple hierarchies found for name: " + name + " and typeId: " + typeId);
        }
        return list.get(0).getId();
    }

    @Override
    public void upload(List<HierarchyExcelTemplate> hierarchyExcelTemplates, String properties, Long typeId) {

        for (HierarchyExcelTemplate item : hierarchyExcelTemplates) {
            HierarchyBo bo = new HierarchyBo();
            JSONArray objects = JSONUtil.parseArray(properties);
            List<HierarchyProperty> list = objects.toList(HierarchyProperty.class);
            bo.setProperties(list);
            bo.setName(item.getName());
            bo.setTypeId(typeId);
            insertByBo(bo);
        }
    }

    @Override
    public List<HierarchyTreeVo> getUnitHierarchyTree(Long parentId, Long hierarchyId) {
        // 1. 获取unit类型ID（type_key = "unit"）
        HierarchyType unitType = hierarchyTypeMapper.selectOne(
            Wrappers.<HierarchyType>lambdaQuery()
                .eq(HierarchyType::getTypeKey, "unit")
        );

        if (unitType == null) {
            return new ArrayList<>();
        }

        // 2. 获取所有unit层级的传感器绑定情况
        Map<String, List<HierarchyVo>> sensorMap = sensorList(parentId, hierarchyId);
        List<HierarchyVo> unboundSensors = sensorMap.getOrDefault("unbound", new ArrayList<>());
        List<HierarchyVo> boundSensors = sensorMap.getOrDefault("bound", new ArrayList<>());

        // 3. 创建传感器按unit分组的Map
        Map<Long, List<HierarchyVo>> unboundSensorMap = groupSensorsByUnit(unboundSensors);
        Map<Long, List<HierarchyVo>> boundSensorMap = groupSensorsByUnit(boundSensors);

        // 4. 从指定父级开始递归构建树，遇到unit类型停止递归
        return buildHierarchyTreeToUnit(parentId, unitType.getId(), unboundSensorMap, boundSensorMap);
    }

    /**
     * 递归构建层级树结构，直到遇到unit类型为止
     *
     * @param parentId 父级层级ID
     * @param unitTypeId unit类型ID
     * @param unboundSensorMap 未绑定传感器按unit分组的Map
     * @param boundSensorMap 已绑定传感器按unit分组的Map
     * @return 层级树结构
     */
    private List<HierarchyTreeVo> buildHierarchyTreeToUnit(Long parentId, Long unitTypeId,
            Map<Long, List<HierarchyVo>> unboundSensorMap, Map<Long, List<HierarchyVo>> boundSensorMap) {
        List<HierarchyTreeVo> result = new ArrayList<>();

        // 查询指定父级下的所有直接子层级
        List<HierarchyVo> children = baseMapper.selectVoList(
            Wrappers.<Hierarchy>lambdaQuery()
                .eq(parentId != null, Hierarchy::getParentId, parentId)
                .ne (Hierarchy::getTypeId, 24)
                .isNull(parentId == null, Hierarchy::getParentId)
                .orderByAsc(Hierarchy::getCode)
        );

        for (HierarchyVo hierarchyVo : children) {
            // 创建树节点
            HierarchyTreeVo treeVo = convertToTreeVo(hierarchyVo);

            // 如果当前层级不是unit类型，继续递归查询子层级
            if (!unitTypeId.equals(hierarchyVo.getTypeId())) {
                List<HierarchyTreeVo> childrenTree = buildHierarchyTreeToUnit(hierarchyVo.getId(), unitTypeId, unboundSensorMap, boundSensorMap);
                if (!childrenTree.isEmpty()) {
                    treeVo.setChildren(childrenTree);
                }
            } else {
                // 如果是unit类型，为其添加传感器信息
                treeVo.setUnboundSensors(unboundSensorMap.get(hierarchyVo.getId()));
                treeVo.setBoundSensors(boundSensorMap.get(hierarchyVo.getId()));
            }

            result.add(treeVo);
        }

        return result;
    }

    /**
     * 将传感器按unit分组
     *
     * @param sensors 传感器列表
     * @return 按unit ID分组的传感器Map
     */
    private Map<Long, List<HierarchyVo>> groupSensorsByUnit(List<HierarchyVo> sensors) {
        Map<Long, List<HierarchyVo>> sensorMap = new HashMap<>();
        if (sensors == null || sensors.isEmpty()) {
            return sensorMap;
        }

        for (HierarchyVo sensor : sensors) {
            Long unitId = sensor.getParentId();
            if (unitId != null) {
                sensorMap.computeIfAbsent(unitId, k -> new ArrayList<>()).add(sensor);
            }
        }

        return sensorMap;
    }

    /**
     * 将HierarchyVo转换为HierarchyTreeVo
     */
    private HierarchyTreeVo convertToTreeVo(HierarchyVo hierarchyVo) {
        HierarchyTreeVo treeVo = new HierarchyTreeVo();
        treeVo.setId(hierarchyVo.getId());
        treeVo.setTypeId(hierarchyVo.getTypeId());
        treeVo.setParentId(hierarchyVo.getParentId());
        treeVo.setName(hierarchyVo.getName());
        treeVo.setCode(hierarchyVo.getCode());

        return treeVo;
    }

}


