package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class TestPointServiceImpl implements ITestPointService {

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
    public Boolean importFromJson(String jsonData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode dataNode = rootNode.get("data");

            if (dataNode == null || !dataNode.isArray()) {
                throw new ServiceException("JSON格式错误：缺少data数组");
            }

            // 解析SD400MP测点数据
            List<TestPoint> sdTestPointList = new ArrayList<>();
            Long currentEquipmentId = null;

            for (JsonNode testPointNode : dataNode) {
                TestPoint testPoint = parseTestPointFromJsonAll(testPointNode);
                if (testPoint != null && testPoint.getId() != null) {
                    sdTestPointList.add(testPoint);
                    // 记录当前处理的设备ID
                    if (currentEquipmentId == null && testPoint.getEquipmentId() != null) {
                        currentEquipmentId = testPoint.getEquipmentId();
                    }
                }
            }

            if (sdTestPointList.isEmpty()) {
                return true;
            }

            // 获取当前设备下的现有测点，按id建立映射
            LambdaQueryWrapper<TestPoint> wrapper = Wrappers.lambdaQuery();
            if (currentEquipmentId != null) {
                wrapper.eq(TestPoint::getEquipmentId, currentEquipmentId);
            }
            List<TestPoint> existingTestPoints = baseMapper.selectList(wrapper);
            Map<Long, TestPoint> existingIdMap = existingTestPoints.stream()
                .filter(tp -> tp.getId() != null)
                .collect(Collectors.toMap(TestPoint::getId, tp -> tp, (tp1, tp2) -> tp1));

            // 处理新增和更新
            for (TestPoint testPoint : sdTestPointList) {
                TestPoint existing = existingIdMap.get(testPoint.getId());
                if (existing != null) {
                    // 更新现有测点
                    updateTestPointBySql(testPoint.getId(), testPoint);
                } else {
                    // 新增测点
                    setDefaultThresholds(testPoint);
                    baseMapper.insert(testPoint);
                }
            }

            // 删除当前设备下在SD400MP中已不存在的测点
            List<Long> sdIds = sdTestPointList.stream()
                .map(TestPoint::getId)
                .collect(Collectors.toList());

            List<Long> toDeleteIds = existingTestPoints.stream()
                .filter(tp -> tp.getId() != null && !sdIds.contains(tp.getId()))
                .map(TestPoint::getId)
                .collect(Collectors.toList());

            if (!toDeleteIds.isEmpty()) {
                baseMapper.deleteByIds(toDeleteIds);
            }

            return true;
        } catch (Exception e) {
            throw new ServiceException("测点批量同步失败：" + e.getMessage());
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
     * 从JSON节点解析测点对象（批量导入版本，从JSON中解析equipmentId）
     */
    private TestPoint parseTestPointFromJsonAll(JsonNode testPointNode) {
        try {
            TestPoint testPoint = new TestPoint();

            // 直接使用SD400MP的ID作为主键（字符串转长整型）
            if (testPointNode.has("id") && !testPointNode.get("id").isNull()) {
                String idStr = testPointNode.get("id").asText();
                try {
                    testPoint.setId(Long.parseLong(idStr));
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                // 如果没有ID，跳过此测点
                return null;
            }

            // 从SD400MP的idEq字段解析设备ID
            if (testPointNode.has("idEq") && !testPointNode.get("idEq").isNull()) {
                testPoint.setEquipmentId(testPointNode.get("idEq").asLong());
            }

            // KKS编码 - 对应SD400MP的key字段
            if (testPointNode.has("key") && !testPointNode.get("key").isNull()) {
                testPoint.setKksCode(testPointNode.get("key").asText());
            }

            // KKS名称 - 对应SD400MP的name字段
            if (testPointNode.has("name") && !testPointNode.get("name").isNull()) {
                testPoint.setKksName(testPointNode.get("name").asText());
            } else {
                testPoint.setKksName("未命名测点");
            }

            // 启用状态
            if (testPointNode.has("enabled") && !testPointNode.get("enabled").isNull()) {
                boolean enabled = testPointNode.get("enabled").asBoolean();
                // 可以根据需要决定如何处理enabled状态
            }

            // SD400MP的settings字段暂时不处理，因为它是复杂的嵌套结构
            // 其他字段如lastMagnitude、lastSt等在SD400MP的testpoint接口中可能不包含
            // 这些数据可能需要通过其他接口获取

            return testPoint;
        } catch (Exception e) {
            throw new ServiceException("解析测点数据失败：" + e.getMessage());
        }
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
            .set(TestPoint::getEquipmentId, newData.getEquipmentId());

        baseMapper.update(null, updateWrapper);
        // 注意：不更新last_开头的实时数据字段和阈值配置，保持现有数据
    }
}
