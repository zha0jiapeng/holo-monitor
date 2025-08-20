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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.bo.HierarchyBo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.mapper.HierarchyMapper;
import org.dromara.hm.service.IHierarchyService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 层级Service业务层处理
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
public class HierarchyServiceImpl implements IHierarchyService {

    private final HierarchyMapper baseMapper;

    @Override
    public HierarchyVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<HierarchyVo> queryPageList(HierarchyBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Hierarchy> lqw = buildQueryWrapper(bo);
        Page<HierarchyVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
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

    private LambdaQueryWrapper<Hierarchy> buildQueryWrapper(HierarchyBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<Hierarchy> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getUniqueKey()), Hierarchy::getUniqueKey, bo.getUniqueKey());
        lqw.eq(bo.getIdParent() != null, Hierarchy::getIdParent, bo.getIdParent());
        lqw.like(StringUtils.isNotBlank(bo.getName()), Hierarchy::getName, bo.getName());
        lqw.like(StringUtils.isNotBlank(bo.getDesc()), Hierarchy::getDesc, bo.getDesc());
        lqw.eq(bo.getType() != null, Hierarchy::getType, bo.getType());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            Hierarchy::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByAsc(Hierarchy::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(HierarchyBo bo) {
        Hierarchy add = MapstructUtils.convert(bo, Hierarchy.class);
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
    public Boolean updateByBo(HierarchyBo bo) {
        Hierarchy update = MapstructUtils.convert(bo, Hierarchy.class);
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
    private void validEntityBeforeSave(Hierarchy entity) {
        // 校验唯一键不能重复
        if (StringUtils.isNotBlank(entity.getUniqueKey())) {
            LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(Hierarchy::getUniqueKey, entity.getUniqueKey());
            if (entity.getId() != null) {
                wrapper.ne(Hierarchy::getId, entity.getId());
            }
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("层级唯一键已存在");
            }
        }

        // 校验父级层级是否存在
        if (entity.getIdParent() != null) {
            Hierarchy parent = baseMapper.selectById(entity.getIdParent());
            if (parent == null) {
                throw new ServiceException("父级层级不存在");
            }
            
            // 防止循环引用
            if (entity.getId() != null && entity.getIdParent().equals(entity.getId())) {
                throw new ServiceException("不能将自己设置为父级");
            }
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 检查是否有子层级，如果有则不能删除
            for (Long id : ids) {
                LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
                wrapper.eq(Hierarchy::getIdParent, id);
                if (baseMapper.exists(wrapper)) {
                    throw new ServiceException("存在子层级，无法删除");
                }
            }

            // 校验删除权限
            List<Hierarchy> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("您没有删除权限!");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<Hierarchy> list) {
        return baseMapper.insertBatch(list);
    }

    @Override
    public List<Hierarchy> getHierarchiesByType(Integer type) {
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(type != null, Hierarchy::getType, type);
        wrapper.orderByAsc(Hierarchy::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<HierarchyVo> getChildrenByParentId(Long idParent) {
        LambdaQueryWrapper<Hierarchy> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(idParent != null, Hierarchy::getIdParent, idParent);
        wrapper.orderByAsc(Hierarchy::getId);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public Boolean importFromJson(String jsonData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null || !dataNode.isArray()) {
                throw new ServiceException("JSON格式错误：缺少data数组");
            }

            // 获取当前数据库中的所有层级，按id建立映射
            List<Hierarchy> existingHierarchies = baseMapper.selectList(null);
            Map<Long, Hierarchy> existingIdMap = existingHierarchies.stream()
                .filter(h -> h.getId() != null)
                .collect(Collectors.toMap(Hierarchy::getId, h -> h, (h1, h2) -> h1));

            // 解析层级数据（此时数据已经过滤，只包含层级）
            List<Hierarchy> newHierarchies = new ArrayList<>();
            Map<Long, Long> parentRelations = new HashMap<>();

            // 第一轮：解析层级数据
            for (JsonNode itemNode : dataNode) {
                Hierarchy hierarchy = parseHierarchyFromJson(itemNode);
                if (hierarchy != null && hierarchy.getId() != null) {
                    newHierarchies.add(hierarchy);
                    // 记录父级关系
                    if (itemNode.has("idParent") && !itemNode.get("idParent").isNull()) {
                        parentRelations.put(hierarchy.getId(), itemNode.get("idParent").asLong());
                    }
                }
            }

            // 第二轮：新增和更新层级（暂不处理父级关系）
            for (Hierarchy hierarchy : newHierarchies) {
                Hierarchy existing = existingIdMap.get(hierarchy.getId());
                if (existing != null) {
                    // 更新现有层级
                    hierarchy.setCreateBy(existing.getCreateBy());
                    hierarchy.setCreateTime(existing.getCreateTime());
                    hierarchy.setCreateDept(existing.getCreateDept());
                    hierarchy.setTenantId(existing.getTenantId());
                    baseMapper.updateById(hierarchy);
                } else {
                    // 新增层级
                    hierarchy.setIdParent(null);
                    baseMapper.insert(hierarchy);
                }
            }

            // 第三轮：更新父级关系
            for (Hierarchy hierarchy : newHierarchies) {
                Long parentId = parentRelations.get(hierarchy.getId());
                if (parentId != null) {
                    // 检查父级层级是否存在
                    if (existingIdMap.containsKey(parentId) || 
                        newHierarchies.stream().anyMatch(h -> h.getId().equals(parentId))) {
                        updateParentId(hierarchy.getId(), parentId);
                    }
                }
            }

            // 第四轮：删除不存在的层级
            List<Long> newIds = newHierarchies.stream().map(Hierarchy::getId).toList();
            List<Long> toDeleteIds = existingHierarchies.stream()
                .filter(h -> h.getId() != null && !newIds.contains(h.getId()))
                .map(Hierarchy::getId)
                .toList();

            if (!toDeleteIds.isEmpty()) {
                List<Long> allDeleteIds = new ArrayList<>(toDeleteIds);
                findAndAddChildIds(allDeleteIds, existingHierarchies);
                baseMapper.deleteByIds(allDeleteIds);
            }

            return true;
        } catch (Exception e) {
            throw new ServiceException("层级同步失败：" + e.getMessage());
        }
    }



    /**
     * 从JSON节点解析层级对象
     */
    private Hierarchy parseHierarchyFromJson(JsonNode hierarchyNode) {
        try {
            Hierarchy hierarchy = new Hierarchy();

            // 直接使用SD400MP的ID作为主键
            if (hierarchyNode.has("id") && !hierarchyNode.get("id").isNull()) {
                hierarchy.setId(hierarchyNode.get("id").asLong());
            } else {
                return null;
            }

            // 唯一键
            if (hierarchyNode.has("key") && !hierarchyNode.get("key").isNull()) {
                hierarchy.setUniqueKey(hierarchyNode.get("key").asText());
            }

            // 层级名称 - 必须字段
            if (hierarchyNode.has("name") && !hierarchyNode.get("name").isNull()) {
                hierarchy.setName(hierarchyNode.get("name").asText());
            } else {
                hierarchy.setName("未命名层级");
            }

            // 层级类型
            if (hierarchyNode.has("type") && !hierarchyNode.get("type").isNull()) {
                hierarchy.setType(hierarchyNode.get("type").asInt());
            } else {
                hierarchy.setType(0); // 默认类型
            }

            // 描述
            if (hierarchyNode.has("desc") && !hierarchyNode.get("desc").isNull()) {
                hierarchy.setDesc(hierarchyNode.get("desc").asText());
            }

            // 设置（blob类型）
            if (hierarchyNode.has("settings") && !hierarchyNode.get("settings").isNull()) {
                JsonNode settingsNode = hierarchyNode.get("settings");
                try {
                    String settingsStr;
                    if (settingsNode.isTextual()) {
                        settingsStr = settingsNode.asText();
                    } else {
                        settingsStr = settingsNode.toString();
                    }
                    hierarchy.setSettings(settingsStr.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    hierarchy.setSettings(settingsNode.toString().getBytes(StandardCharsets.UTF_8));
                }
            }

            return hierarchy;
        } catch (Exception e) {
            throw new ServiceException("解析层级数据失败：" + e.getMessage());
        }
    }

    /**
     * 更新父级ID
     */
    private void updateParentId(Long hierarchyId, Long parentId) {
        LambdaUpdateWrapper<Hierarchy> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(Hierarchy::getId, hierarchyId)
            .set(Hierarchy::getIdParent, parentId);
        baseMapper.update(updateWrapper);
    }

    /**
     * 递归查找并添加子层级ID
     */
    private void findAndAddChildIds(List<Long> deleteIds, List<Hierarchy> allHierarchies) {
        List<Long> newChildIds = new ArrayList<>();
        for (Long parentId : deleteIds) {
            for (Hierarchy hierarchy : allHierarchies) {
                if (parentId.equals(hierarchy.getIdParent()) && !deleteIds.contains(hierarchy.getId())) {
                    newChildIds.add(hierarchy.getId());
                }
            }
        }
        if (!newChildIds.isEmpty()) {
            deleteIds.addAll(newChildIds);
            // 递归查找子层级的子层级
            findAndAddChildIds(newChildIds, allHierarchies);
        }
    }

}
