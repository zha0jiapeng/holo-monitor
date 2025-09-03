package org.dromara.hm.service.impl;

import cn.hutool.core.date.DateUtil;
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
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.Testpoint;
import org.dromara.hm.domain.bo.TestpointBo;
import org.dromara.hm.domain.vo.TestpointVo;
import org.dromara.hm.enums.PositionSourceEnum;
import org.dromara.hm.enums.TestpointTypeEnum;
import org.dromara.hm.mapper.TestpointMapper;
import org.dromara.hm.service.ITestpointService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * 测点Service业务层处理
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TestpointServiceImpl extends ServiceImpl<TestpointMapper, Testpoint> implements ITestpointService {

    private final TestpointMapper baseMapper;

    @Override
    public TestpointVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<TestpointVo> queryPageList(TestpointBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Testpoint> lqw = buildQueryWrapper(bo);
        Page<TestpointVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<TestpointVo> customPageList(TestpointBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Testpoint> lqw = buildQueryWrapper(bo);
        Page<TestpointVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<TestpointVo> queryList(TestpointBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<Testpoint> buildQueryWrapper(TestpointBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<Testpoint> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getHierarchyId() != null, Testpoint::getHierarchyId, bo.getHierarchyId());
        lqw.eq(bo.getHierarchyOwnerId() != null && bo.getHierarchyOwnerId() != -1, Testpoint::getHierarchyOwnerId, bo.getHierarchyOwnerId());
        lqw.isNull(bo.getHierarchyOwnerId() == -1, Testpoint::getHierarchyOwnerId);
       // lqw.eq(bo.getType() != null, Testpoint::getType, bo.getType());
        lqw.eq(bo.getMt() != null, Testpoint::getMt, bo.getMt());
        lqw.eq(StringUtils.isNotBlank(bo.getKksCode()), Testpoint::getKksCode, bo.getKksCode());
        lqw.like(StringUtils.isNotBlank(bo.getKksName()), Testpoint::getKksName, bo.getKksName());
        lqw.eq(bo.getLastSt() != null, Testpoint::getLastSt, bo.getLastSt());
        lqw.eq(bo.getLastAlarmType() != null, Testpoint::getLastAlarmType, bo.getLastAlarmType());
        lqw.between(params.get("beginLastAcquisitionTime") != null && params.get("endLastAcquisitionTime") != null,
            Testpoint::getLastAcquisitionTime, params.get("beginLastAcquisitionTime"), params.get("endLastAcquisitionTime"));
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            Testpoint::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByAsc(Testpoint::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(TestpointBo bo) {
        Testpoint add = MapstructUtils.convert(bo, Testpoint.class);
        validEntityBeforeSave(add);
        setDefaultThresholds(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(TestpointBo bo) {
        Testpoint update = MapstructUtils.convert(bo, Testpoint.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateBatchByBo(List<TestpointBo> bos) {
        Boolean flag = true;
        for (TestpointBo bo : bos) {
            Testpoint update = MapstructUtils.convert(bo, Testpoint.class);
            validEntityBeforeSave(update);
            if(update.getPositionSource()!=null) update.setPositionSource(PositionSourceEnum.LOCAL.getCode());
            if(baseMapper.updateById(update)==0){
                flag = false;
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                break;
            }
        }
        return flag;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(Testpoint entity) {


        // 校验KKS编码唯一性
        if (StringUtils.isNotBlank(entity.getKksCode())) {
            LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(Testpoint::getKksCode, entity.getKksCode());
            if (entity.getId() != null) {
                wrapper.ne(Testpoint::getId, entity.getId());
            }
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("测点编码已存在");
            }
        }
    }

    /**
     * 设置默认阈值
     */
    private void setDefaultThresholds(Testpoint entity) {
        if (entity.getUhfIgnoreThreshold() == null) {
            entity.setUhfIgnoreThreshold(BigDecimal.valueOf(-20.00));
        }
        if (entity.getUhfMutationThreshold() == null) {
            entity.setUhfMutationThreshold(BigDecimal.valueOf(-20.00));
        }
        if (entity.getUhfLevel1AlarmThreshold() == null) {
            entity.setUhfLevel1AlarmThreshold(BigDecimal.valueOf(-20.00));
        }
        if (entity.getUhfLevel2AlarmThreshold() == null) {
            entity.setUhfLevel2AlarmThreshold(BigDecimal.valueOf(-40.00));
        }
        if (entity.getUhfLevel3AlarmThreshold() == null) {
            entity.setUhfLevel3AlarmThreshold(BigDecimal.valueOf(-60.00));
        }
        if (entity.getDischargeEventRatioThreshold() == null) {
            entity.setDischargeEventRatioThreshold(BigDecimal.valueOf(0.25));
        }
        if (entity.getEventCountThresholdPeriod() == null) {
            entity.setEventCountThresholdPeriod(24);
        }
        if (entity.getAlarmResetDelay() == null) {
            entity.setAlarmResetDelay(4);
        }
        if (entity.getOfflineJudgmentThreshold() == null) {
            entity.setOfflineJudgmentThreshold(24);
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验删除权限
            List<Testpoint> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("您没有删除权限!");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<Testpoint> list) {
        // 批量保存前设置默认值
        for (Testpoint testPoint : list) {
            setDefaultThresholds(testPoint);
        }
        return baseMapper.insertBatch(list);
    }

    @Override
    public List<TestpointVo> queryByEquipmentId(Long equipmentId) {
        LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Testpoint::getHierarchyId, equipmentId);
        wrapper.orderByAsc(Testpoint::getKksCode);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public TestpointVo queryByKksCode(String kksCode) {
        LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Testpoint::getKksCode, kksCode);
        wrapper.last("LIMIT 1");
        return baseMapper.selectVoOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean importFromJson(JsonNode testPointJson) {
        try {
            Testpoint testPoint = parseTestPointFromJson(testPointJson);
            if (!isValidTestPoint(testPoint)) {
                log.warn("测点数据无效，跳过处理：{}", testPointJson);
                return false;
            }

            return processTestPoint(testPoint);

        } catch (Exception e) {
            log.error("测点同步失败：{}", e.getMessage(), e);
            throw new ServiceException("测点同步失败：" + e.getMessage());
        }
    }

    /**
     * 验证测点数据有效性
     */
    private boolean isValidTestPoint(Testpoint testPoint) {
        return testPoint != null
            && testPoint.getId() != null
            && testPoint.getHierarchyId() != null;
    }

    /**
     * 处理测点数据（新增或更新）
     */
    private boolean processTestPoint(Testpoint testPoint) {
        Testpoint existing = baseMapper.selectById(testPoint.getId());

        if (existing != null) {
            return updateExistingTestPoint(testPoint);
        } else {
            return insertNewTestPoint(testPoint);
        }
    }

    /**
     * 更新现有测点
     */
    private boolean updateExistingTestPoint(Testpoint testpoint) {
        try {
            // 保留原有的show_name，不覆盖
            Testpoint existing = baseMapper.selectById(testpoint.getId());
            if (existing != null && StringUtils.isNotBlank(existing.getShowName())) {
                testpoint.setShowName(existing.getShowName());
            }
            updateTestPointBySql(testpoint.getId(), testpoint);
            log.debug("更新测点成功：{}", testpoint.getId());
            return true;
        } catch (Exception e) {
            log.error("更新测点失败：{}，错误：{}", testpoint.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * 插入新测点
     */
    private boolean insertNewTestPoint(Testpoint testPoint) {
        try {
            setDefaultThresholds(testPoint);
            // 新增测点 - 初始化show_name为name
            // 如果show_name为空，设置为name
            if (StringUtils.isBlank(testPoint.getShowName()) && StringUtils.isNotBlank(testPoint.getKksName())) {
                testPoint.setShowName(testPoint.getKksName());
            }
            int result = baseMapper.insert(testPoint);
            if (result > 0) {
                log.debug("新增测点成功：{}", testPoint.getId());
                return true;
            } else {
                log.error("新增测点失败：{}", testPoint.getId());
                return false;
            }
        } catch (Exception e) {
            log.error("新增测点异常：{}，错误：{}", testPoint.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public Boolean unbind(List<Long> testPointIds) {
        LambdaUpdateWrapper<Testpoint> wrapper = Wrappers.lambdaUpdate();
        wrapper.set(Testpoint::getPositionX, null);
        wrapper.set(Testpoint::getPositionY, null);
        wrapper.set(Testpoint::getPositionZ, null);
        wrapper.in(Testpoint::getId, testPointIds);
        return  baseMapper.update(wrapper) > 0;
    }

    /**
     * 从JSON节点解析测点对象
     */
    private Testpoint parseTestPointFromJson(JsonNode testPointNode) {
        if (testPointNode == null || testPointNode.isNull()) {
            log.warn("测点JSON数据为空");
            return null;
        }

        try {
            Testpoint testPoint = new Testpoint();

            // 解析测点ID
            Long testPointId = parseTestPointId(testPointNode);
            if (testPointId == null) {
                return null;
            }
            testPoint.setId(testPointId);

            // 解析设备ID
            Long equipmentId = parseEquipmentId(testPointNode);
            testPoint.setHierarchyId(equipmentId);

            // 解析KKS编码和名称
            testPoint.setKksCode(getJsonStringValue(testPointNode, "key"));
            testPoint.setKksName(getJsonStringValue(testPointNode, "name", testPoint.getKksCode()));

            // 解析监测类型
            int mt = getJsonIntValue(testPointNode, "mt", -1);
            testPoint.setMt(mt);

            // 根据mt值自动设置测点类型
            TestpointTypeEnum typeEnum = TestpointTypeEnum.getByMtValue(mt);
            testPoint.setType(typeEnum.getCode());

            log.debug("解析测点数据成功：ID={}, 设备ID={}, KKS编码={}, mt={} ({})",
                testPoint.getId(), testPoint.getHierarchyId(),
                testPoint.getKksCode(), testPoint.getMt(), typeEnum.getName());

            return testPoint;

        } catch (Exception e) {
            log.error("解析测点数据失败：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析测点ID
     */
    private Long parseTestPointId(JsonNode testPointNode) {
        String idStr = getJsonStringValue(testPointNode, "id");
        if (idStr == null || idStr.trim().isEmpty()) {
            log.warn("测点缺少ID字段");
            return null;
        }

        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            log.error("测点ID格式错误: {}", idStr);
            return null;
        }
    }

    /**
     * 解析设备ID
     */
    private Long parseEquipmentId(JsonNode testPointNode) {
        return getJsonLongValue(testPointNode, "idEq");
    }

    /**
     * 获取JSON字符串值
     */
    private String getJsonStringValue(JsonNode node, String fieldName) {
        return getJsonStringValue(node, fieldName, null);
    }

    /**
     * 获取JSON字符串值（带默认值）
     */
    private String getJsonStringValue(JsonNode node, String fieldName, String defaultValue) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return defaultValue;
    }

    /**
     * 获取JSON长整型值
     */
    private Long getJsonLongValue(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asLong();
        }
        return null;
    }

    /**
     * 获取JSON整型值（带默认值）
     */
    private int getJsonIntValue(JsonNode node, String fieldName, int defaultValue) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asInt();
        }
        return defaultValue;
    }

    /**
     * 使用SQL更新测点配置信息（避免Sa-Token上下文问题）
     * 注意：只更新配置字段，不影响实时监测数据和阈值设置
     */
    private void updateTestPointBySql(Long testpointId, Testpoint newData) {
        LambdaUpdateWrapper<Testpoint> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(Testpoint::getId, testpointId)
            .set(Testpoint::getKksCode, newData.getKksCode())
            .set(Testpoint::getKksName, newData.getKksName())
            .set(Testpoint::getMt, newData.getMt())
            //.set(Testpoint::getType, newData.getType())
            .set(Testpoint::getUpdateTime, DateUtil.dateSecond())
            .set(Testpoint::getHierarchyId, newData.getHierarchyId());

        baseMapper.update(null, updateWrapper);
        // 注意：不更新last_开头的实时数据字段和阈值配置，保持现有数据
    }
}
