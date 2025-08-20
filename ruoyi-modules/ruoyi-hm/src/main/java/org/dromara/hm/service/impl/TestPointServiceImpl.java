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
import org.dromara.hm.domain.TestPoint;
import org.dromara.hm.domain.bo.TestPointBo;
import org.dromara.hm.domain.vo.TestPointVo;
import org.dromara.hm.mapper.TestPointMapper;
import org.dromara.hm.service.ITestPointService;
import org.dromara.hm.service.IEquipmentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import org.dromara.hm.enums.TestPointTypeEnum;
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
public class TestPointServiceImpl extends ServiceImpl<TestPointMapper, TestPoint> implements ITestPointService {

    private final TestPointMapper baseMapper;
    private final IEquipmentService equipmentService;

    @Override
    public TestPointVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<TestPointVo> queryPageList(TestPointBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestPoint> lqw = buildQueryWrapper(bo);
        Page<TestPointVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<TestPointVo> customPageList(TestPointBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestPoint> lqw = buildQueryWrapper(bo);
        Page<TestPointVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<TestPointVo> queryList(TestPointBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<TestPoint> buildQueryWrapper(TestPointBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<TestPoint> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getEquipmentId() != null, TestPoint::getEquipmentId, bo.getEquipmentId());
        lqw.eq(bo.getType() != null, TestPoint::getType, bo.getType());
        lqw.eq(bo.getMt() != null, TestPoint::getMt, bo.getMt());
        lqw.eq(StringUtils.isNotBlank(bo.getKksCode()), TestPoint::getKksCode, bo.getKksCode());
        lqw.like(StringUtils.isNotBlank(bo.getKksName()), TestPoint::getKksName, bo.getKksName());
        lqw.eq(bo.getLastSt() != null, TestPoint::getLastSt, bo.getLastSt());
        lqw.eq(bo.getLastAlarmType() != null, TestPoint::getLastAlarmType, bo.getLastAlarmType());
        lqw.between(params.get("beginLastAcquisitionTime") != null && params.get("endLastAcquisitionTime") != null,
            TestPoint::getLastAcquisitionTime, params.get("beginLastAcquisitionTime"), params.get("endLastAcquisitionTime"));
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            TestPoint::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByAsc(TestPoint::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(TestPointBo bo) {
        TestPoint add = MapstructUtils.convert(bo, TestPoint.class);
        validEntityBeforeSave(add);
        setDefaultThresholds(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(TestPointBo bo) {
        TestPoint update = MapstructUtils.convert(bo, TestPoint.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateBatchByBo(List<TestPointBo> bos) {
        Boolean flag = true;
        for (TestPointBo bo : bos) {
            TestPoint update = MapstructUtils.convert(bo, TestPoint.class);
            validEntityBeforeSave(update);
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
    private void validEntityBeforeSave(TestPoint entity) {
        // 校验设备是否存在
        if (entity.getEquipmentId() != null) {
            if (equipmentService.queryById(entity.getEquipmentId()) == null) {
                throw new ServiceException("关联的设备不存在");
            }
        }

        // 校验KKS编码唯一性
        if (StringUtils.isNotBlank(entity.getKksCode())) {
            LambdaQueryWrapper<TestPoint> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(TestPoint::getKksCode, entity.getKksCode());
            if (entity.getId() != null) {
                wrapper.ne(TestPoint::getId, entity.getId());
            }
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("测点编码已存在");
            }
        }
    }

    /**
     * 设置默认阈值
     */
    private void setDefaultThresholds(TestPoint entity) {
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
            List<TestPoint> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("您没有删除权限!");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<TestPoint> list) {
        // 批量保存前设置默认值
        for (TestPoint testPoint : list) {
            setDefaultThresholds(testPoint);
        }
        return baseMapper.insertBatch(list);
    }

    @Override
    public List<TestPointVo> queryByEquipmentId(Long equipmentId) {
        LambdaQueryWrapper<TestPoint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TestPoint::getEquipmentId, equipmentId);
        wrapper.orderByAsc(TestPoint::getKksCode);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public TestPointVo queryByKksCode(String kksCode) {
        LambdaQueryWrapper<TestPoint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TestPoint::getKksCode, kksCode);
        wrapper.last("LIMIT 1");
        return baseMapper.selectVoOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean importFromJson(JsonNode testPointJson) {
        try {
            TestPoint testPoint = parseTestPointFromJson(testPointJson);
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
    private boolean isValidTestPoint(TestPoint testPoint) {
        return testPoint != null
            && testPoint.getId() != null
            && testPoint.getEquipmentId() != null;
    }

    /**
     * 处理测点数据（新增或更新）
     */
    private boolean processTestPoint(TestPoint testPoint) {
        TestPoint existing = baseMapper.selectById(testPoint.getId());

        if (existing != null) {
            return updateExistingTestPoint(testPoint);
        } else {
            return insertNewTestPoint(testPoint);
        }
    }

    /**
     * 更新现有测点
     */
    private boolean updateExistingTestPoint(TestPoint testPoint) {
        try {
            updateTestPointBySql(testPoint.getId(), testPoint);
            log.debug("更新测点成功：{}", testPoint.getId());
            return true;
        } catch (Exception e) {
            log.error("更新测点失败：{}，错误：{}", testPoint.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * 插入新测点
     */
    private boolean insertNewTestPoint(TestPoint testPoint) {
        try {
            setDefaultThresholds(testPoint);
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
        LambdaUpdateWrapper<TestPoint> wrapper = Wrappers.lambdaUpdate();
        wrapper.set(TestPoint::getPositionX, null);
        wrapper.set(TestPoint::getPositionY, null);
        wrapper.set(TestPoint::getPositionZ, null);
        wrapper.in(TestPoint::getId, testPointIds);
        return  baseMapper.update(wrapper) > 0;
    }

    /**
     * 从JSON节点解析测点对象
     */
    private TestPoint parseTestPointFromJson(JsonNode testPointNode) {
        if (testPointNode == null || testPointNode.isNull()) {
            log.warn("测点JSON数据为空");
            return null;
        }

        try {
            TestPoint testPoint = new TestPoint();

            // 解析测点ID
            Long testPointId = parseTestPointId(testPointNode);
            if (testPointId == null) {
                return null;
            }
            testPoint.setId(testPointId);

            // 解析设备ID
            Long equipmentId = parseEquipmentId(testPointNode);
            testPoint.setEquipmentId(equipmentId);

            // 解析KKS编码和名称
            testPoint.setKksCode(getJsonStringValue(testPointNode, "key"));
            testPoint.setKksName(getJsonStringValue(testPointNode, "name", testPoint.getKksCode()));

            // 解析监测类型
            int mt = getJsonIntValue(testPointNode, "mt", -1);
            testPoint.setMt(mt);

            // 根据mt值自动设置测点类型
            TestPointTypeEnum typeEnum = TestPointTypeEnum.getByMtValue(mt);
            testPoint.setType(typeEnum.getCode());

            log.debug("解析测点数据成功：ID={}, 设备ID={}, KKS编码={}, mt={}, type={} ({})",
                testPoint.getId(), testPoint.getEquipmentId(),
                testPoint.getKksCode(), testPoint.getMt(), testPoint.getType(), typeEnum.getName());

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
    private void updateTestPointBySql(Long testPointId, TestPoint newData) {
        LambdaUpdateWrapper<TestPoint> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(TestPoint::getId, testPointId)
            .set(TestPoint::getKksCode, newData.getKksCode())
            .set(TestPoint::getKksName, newData.getKksName())
            .set(TestPoint::getMt, newData.getMt())
            .set(TestPoint::getType, newData.getType())
            .set(TestPoint::getUpdateTime, DateUtil.dateSecond())
            .set(TestPoint::getEquipmentId, newData.getEquipmentId());

        baseMapper.update(null, updateWrapper);
        // 注意：不更新last_开头的实时数据字段和阈值配置，保持现有数据
    }
}
