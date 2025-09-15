package org.dromara.hm.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
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
            // 添加填充逻辑
            for (HierarchyVo vo : result.getRecords()) {
                List<HierarchyPropertyVo> properties = hierarchyPropertyMapper.selectVoList(
                    Wrappers.<HierarchyProperty>lambdaQuery().eq(HierarchyProperty::getHierarchyId, vo.getId())
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
     * 根据层级属性中data_type=1001的隐藏属性，按层级类型的code_sort排序组合编码前缀
     * 然后根据前缀去数据库查找已有编码（code LIKE '前缀%'），按id倒序取最大值并递增生成新编码
     * 如果没找到已有编码，则用指定长度补零（如长度3就是001）
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

        // 获取当前层级信息（用于后续获取编码长度，但不加入前缀）
        Hierarchy current = baseMapper.selectById(hierarchyId);

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

        // 拼装属性编码前缀
        String codePrefix = String.join("", codeParts);

        // 获取当前层级类型信息用于确定编码长度
        Integer codeLength = 3; // 默认长度3
        if (current != null) {
            HierarchyType currentType = hierarchyTypeMapper.selectById(current.getTypeId());
            codeLength = (currentType != null && currentType.getCodeLength() != null) ? currentType.getCodeLength() : 3;
        }

        // 根据前缀生成完整编码
        String completeCode = generateCompleteCodeWithPrefix(codePrefix, codeLength);

        Map<String, Object> result = new HashMap<>();
        result.put("code", completeCode);
        result.put("configuration", selectedConfiguration);
        result.put("typePropertyId", typePropertyId);
        return result;
    }

    /**
     * 根据前缀生成完整编码
     * 根据属性拼装的前缀去数据库中查找已有编码，按id倒序取最大值然后递增
     * 如果没找到则用指定长度补零
     *
     * @param codePrefix 编码前缀
     * @param codeLength 编码长度（后缀部分）
     * @return 生成的完整编码
     */
    private String generateCompleteCodeWithPrefix(String codePrefix, Integer codeLength) {
        if (codePrefix == null || codePrefix.isEmpty()) {
            return null;
        }

        if (codeLength == null || codeLength <= 0) {
            codeLength = 3; // 默认长度3
        }

        // 查找数据库中以该前缀开头的编码，按id倒序取最大的一个
        LambdaQueryWrapper<Hierarchy> queryWrapper = Wrappers.<Hierarchy>lambdaQuery()
            .likeRight(Hierarchy::getCode, codePrefix) // code LIKE '前缀%'
            .isNotNull(Hierarchy::getCode)
            .orderByDesc(Hierarchy::getId)
            .last("LIMIT 1");

        Hierarchy latestHierarchy = baseMapper.selectOne(queryWrapper);

        if (latestHierarchy == null || latestHierarchy.getCode() == null) {
            // 没有找到已有编码，生成第一个编码：前缀 + 001（根据长度补零）
            return codePrefix + String.format("%0" + codeLength + "d", 1);
        }

        String existingCode = latestHierarchy.getCode();

        // 找到了已有编码，提取后缀部分并递增
        if (existingCode.length() <= codePrefix.length() || !existingCode.startsWith(codePrefix)) {
            // 如果现有编码长度不足或不是以前缀开头，重新开始：前缀 + 001
            return codePrefix + String.format("%0" + codeLength + "d", 1);
        }

        // 提取后缀部分
        String suffix = existingCode.substring(codePrefix.length());

        // 根据后缀长度确定实际的编码长度（不依赖传入的codeLength参数）
        int actualCodeLength = suffix.length();

        // 尝试将后缀解析为数字并递增
        try {
            int suffixNumber = Integer.parseInt(suffix);
            int nextNumber = suffixNumber + 1;

            // 使用实际的后缀长度来格式化数字，保持长度一致
            return codePrefix + String.format("%0" + actualCodeLength + "d", nextNumber);

        } catch (NumberFormatException e) {
            // 后缀不是纯数字，使用现有的字母递增逻辑
            String nextAlphaSuffix = generateNextCodeFromExisting(suffix, actualCodeLength);
            if (nextAlphaSuffix == null) {
                // 无法生成更多编码
                return null;
            }
            return codePrefix + nextAlphaSuffix;
        }
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
        // 首先检查当前层级是否有编码，如果没有则先生成单层编码
        Hierarchy currentHierarchy = baseMapper.selectById(hierarchyId);
        if (currentHierarchy != null && (currentHierarchy.getCode() == null || currentHierarchy.getCode().isEmpty())) {
            // 获取层级类型信息
            HierarchyType type = hierarchyTypeMapper.selectById(currentHierarchy.getTypeId());
            if (type != null && type.getCodeLength() != null) {
                // 生成单层编码
                String singleCode = generateNextCode(currentHierarchy.getTypeId(), currentHierarchy.getParentId(), type.getCodeLength());
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
                String generatedCode = generateNextCode(bo.getTypeId(), bo.getParentId(), type.getCodeLength());
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
        Long parentHierarchyId = findCascadeParentAndSetRelation(bo.getProperties(), hierarchyId);
        if (parentHierarchyId != null) {
            createHiddenPropertiesFromParent(hierarchyId, parentHierarchyId, extraProperties);
        }
        bo.setParentId(parentHierarchyId);
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
    public List<Map<String,Long>> selectTargetTypeHierarchyList(List<Long> ids, Long targetTypeId) {
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
    public List<HierarchyVo> getSensorListByDeviceId(Long hierarchyId) {
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
                    HierarchyPropertyVo hierarchyProperties = hierarchyPropertyMapper.selectVoOne(new LambdaQueryWrapper<HierarchyProperty>().eq(HierarchyProperty::getHierarchyId, Long.valueOf(sensorIdStr)).eq(HierarchyProperty::getTypePropertyId, hierarchyTypeProperty.getId()));
                    if(hierarchyProperties==null) continue;
                    hierarchyVo.setProperties(List.of(hierarchyProperties));
                    List<String> tags = List.of("sys:cs", "mont/pd/mag", "mont/pd/au", "sys:st");
                    JSONObject entries = SD400MPUtils.testpointFind(hierarchyVo.getCode());
                    if (entries.getInt("code") == 200) {
                        String id = entries.getJSONObject("data").getStr("id");
                        JSONObject data = SD400MPUtils.data(Long.valueOf(id), tags, null);
                        if (data.getInt("code") == 200) {
                            Object online = data.getByPath("data.groups[0].online");
                            if (online == null) continue;
                            JSONArray onlines = (JSONArray) online;
                            hierarchyVo.setDataSet(onlines);
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
                boundSensors = queryByIds(list,true);
            }
        }
        Map<String, List<HierarchyVo>> result = new HashMap<>();
        result.put("unbound", unboundSensors);
        result.put("bound", boundSensors);
        return result;
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

    /**
     * 生成下一个编码
     * 根据typeId和parentId查询最近的编码，生成下一个编码
     *
     * @param typeId 层级类型ID
     * @param parentId 父级ID
     * @param codeLength 编码长度
     * @return 生成的编码，如果无法生成则返回null
     */
    private String generateNextCode(Long typeId, Long parentId, Integer codeLength) {
        if (codeLength == null || codeLength <= 0) {
            return null;
        }

        // 查询同typeId和parentId的最近编码（按id倒序）
        LambdaQueryWrapper<Hierarchy> queryWrapper = Wrappers.<Hierarchy>lambdaQuery()
            .eq(Hierarchy::getTypeId, typeId)
            .eq(parentId != null, Hierarchy::getParentId, parentId)
            .isNull(parentId == null, Hierarchy::getParentId)
            .isNotNull(Hierarchy::getCode)
            .orderByDesc(Hierarchy::getId)
            .last("LIMIT 1");

        Hierarchy latestHierarchy = baseMapper.selectOne(queryWrapper);

        String nextCode;
        if (latestHierarchy == null || latestHierarchy.getCode() == null) {
            // 没有找到已有编码，生成第一个编码
            nextCode = generateFirstCode(codeLength);
        } else {
            // 基于最新编码生成下一个编码
            nextCode = generateNextCodeFromExisting(latestHierarchy.getCode(), codeLength);
        }

        return nextCode;
    }

    /**
     * 生成第一个编码
     * 根据长度生成初始编码，如：01, 001, 0001等
     */
    private String generateFirstCode(Integer codeLength) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength - 1; i++) {
            code.append("0");
        }
        code.append("1");
        return code.toString();
    }

    /**
     * 基于现有编码生成下一个编码
     * 编码规则：先用完所有数字组合，再用字母
     * 例如：01->02->...->99->0A->0B->...->0Z->1A->1B->...->9Z->AA->AB->...->ZZ
     */
    private String generateNextCodeFromExisting(String currentCode, Integer codeLength) {
        if (currentCode == null || currentCode.length() != codeLength) {
            return generateFirstCode(codeLength);
        }

        // 检查当前编码是否全为数字
        if (isAllDigits(currentCode)) {
            // 如果是全数字，尝试数字递增
            String nextNumericCode = incrementNumericCode(currentCode);
            if (nextNumericCode != null) {
                return nextNumericCode;
            }
            // 如果数字已达上限，转换为第一个字母编码
            return getFirstAlphaCode(codeLength);
        } else {
            // 如果已包含字母，按字母规则递增
            return incrementAlphaCode(currentCode, codeLength);
        }
    }

    /**
     * 检查字符串是否全为数字
     */
    private boolean isAllDigits(String code) {
        for (char c : code.toCharArray()) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * 递增纯数字编码
     */
    private String incrementNumericCode(String currentCode) {
        char[] codeArray = currentCode.toCharArray();
        int codeLength = codeArray.length;

        // 从最后一位开始递增
        for (int i = codeLength - 1; i >= 0; i--) {
            if (codeArray[i] < '9') {
                codeArray[i]++;
                return new String(codeArray);
            } else {
                codeArray[i] = '0';
                // 如果是第一位也需要进位，说明数字已达上限
                if (i == 0) {
                    return null; // 返回null表示数字编码已达上限
                }
            }
        }
        return null;
    }

    /**
     * 获取第一个字母编码
     * 例如：长度2时返回"0A"，长度3时返回"00A"
     */
    private String getFirstAlphaCode(Integer codeLength) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength - 1; i++) {
            code.append("0");
        }
        code.append("A");
        return code.toString();
    }

    /**
     * 递增字母编码（已包含字母的编码）
     */
    private String incrementAlphaCode(String currentCode, Integer codeLength) {
        char[] codeArray = currentCode.toCharArray();

        // 从最后一位开始进位
        for (int i = codeLength - 1; i >= 0; i--) {
            char currentChar = codeArray[i];
            char nextChar = getNextAlphaChar(currentChar);

            if (nextChar != '0') {
                // 当前位可以进位，不需要向前进位
                codeArray[i] = nextChar;
                return new String(codeArray);
            } else {
                // 当前位已到最大值，需要向前进位
                if (i == 0) {
                    // 已经到达最大编码，无法再生成
                    return null;
                }
                // 重置当前位为A（字母编码的起始字符）
                codeArray[i] = 'A';
                // 继续向前进位
            }
        }

        return new String(codeArray);
    }

    /**
     * 获取字母编码中字符的下一个字符
     * 0-9 -> 1-9,A
     * A-Z -> B-Z,0(进位)
     */
    private char getNextAlphaChar(char currentChar) {
        if (currentChar >= '0' && currentChar <= '8') {
            // 数字0-8，直接+1
            return (char) (currentChar + 1);
        } else if (currentChar == '9') {
            // 数字9，下一个是A
            return 'A';
        } else if (currentChar >= 'A' && currentChar <= 'Y') {
            // 字母A-Y，直接+1
            return (char) (currentChar + 1);
        } else if (currentChar == 'Z') {
            // 字母Z，需要进位，返回0表示需要进位
            return '0';
        }
        return '0'; // 默认返回0表示需要进位
    }

}

