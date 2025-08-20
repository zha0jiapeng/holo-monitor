package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hm.domain.TestPoint;
import org.dromara.hm.domain.vo.TestPointTypeVo;
import org.dromara.hm.enums.TestPointTypeEnum;
import org.dromara.hm.mapper.TestPointMapper;
import org.dromara.hm.service.ITestPointTypeService;
import org.dromara.hm.utils.TestPointTypeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 测点类型服务实现
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TestPointTypeServiceImpl implements ITestPointTypeService {

    private final TestPointMapper testPointMapper;

    @Override
    public List<TestPointTypeVo> getAllTestPointTypes() {
        // 获取各类型的测点数量统计
        Map<String, Long> typeStatistics = getTestPointTypeStatistics();
        
        return Arrays.stream(TestPointTypeEnum.values())
            .map(typeEnum -> TestPointTypeVo.builder()
                .code(typeEnum.getCode())
                .name(typeEnum.getName())
                .description(typeEnum.getDescription())
                .mtValues(typeEnum.getMtValues())
                .mtRangeText(TestPointTypeUtils.getMtRangeDescription(typeEnum.getCode()))
                .testPointCount(typeStatistics.getOrDefault(typeEnum.getName(), 0L))
                .isDefault(TestPointTypeEnum.OTHER.equals(typeEnum))
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getTestPointTypeStatistics() {
        LambdaQueryWrapper<TestPoint> wrapper = Wrappers.lambdaQuery();
        wrapper.select(TestPoint::getType);
        
        List<TestPoint> testPoints = testPointMapper.selectList(wrapper);
        
        return testPoints.stream()
            .map(TestPoint::getType)
            .map(TestPointTypeEnum::getByCode)
            .filter(typeEnum -> typeEnum != null)
            .collect(Collectors.groupingBy(
                TestPointTypeEnum::getName,
                Collectors.counting()
            ));
    }

    @Override
    public Map<String, Long> getTestPointTypeDistributionByEquipment(Long equipmentId) {
        LambdaQueryWrapper<TestPoint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TestPoint::getEquipmentId, equipmentId)
               .select(TestPoint::getType);
        
        List<TestPoint> testPoints = testPointMapper.selectList(wrapper);
        
        return testPoints.stream()
            .map(TestPoint::getType)
            .map(TestPointTypeEnum::getByCode)
            .filter(typeEnum -> typeEnum != null)
            .collect(Collectors.groupingBy(
                TestPointTypeEnum::getName,
                Collectors.counting()
            ));
    }

    @Override
    public Long getTestPointCountByType(Integer typeCode) {
        LambdaQueryWrapper<TestPoint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TestPoint::getType, typeCode);
        
        return testPointMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long batchUpdateTestPointTypeByMt() {
        log.info("开始批量更新测点类型...");
        
        LambdaQueryWrapper<TestPoint> wrapper = Wrappers.lambdaQuery();
        wrapper.select(TestPoint::getId, TestPoint::getMt, TestPoint::getType);
        
        List<TestPoint> testPoints = testPointMapper.selectList(wrapper);
        
        long updateCount = 0;
        
        for (TestPoint testPoint : testPoints) {
            Integer currentMt = testPoint.getMt();
            Integer currentType = testPoint.getType();
            
            TestPointTypeEnum expectedType = TestPointTypeEnum.getByMtValue(currentMt);
            Integer expectedTypeCode = expectedType.getCode();
            
            // 如果类型不匹配，则更新
            if (!expectedTypeCode.equals(currentType)) {
                LambdaUpdateWrapper<TestPoint> updateWrapper = Wrappers.lambdaUpdate();
                updateWrapper.eq(TestPoint::getId, testPoint.getId())
                           .set(TestPoint::getType, expectedTypeCode);
                
                int result = testPointMapper.update(null, updateWrapper);
                if (result > 0) {
                    updateCount++;
                    log.debug("更新测点 {} 的类型：{} -> {} (mt={})", 
                        testPoint.getId(), currentType, expectedTypeCode, currentMt);
                }
            }
        }
        
        log.info("批量更新测点类型完成，共更新 {} 个测点", updateCount);
        return updateCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long validateAndFixTestPointTypes() {
        log.info("开始验证并修复测点类型数据...");
        
        LambdaQueryWrapper<TestPoint> wrapper = Wrappers.lambdaQuery();
        List<TestPoint> testPoints = testPointMapper.selectList(wrapper);
        
        long fixCount = 0;
        
        for (TestPoint testPoint : testPoints) {
            boolean needUpdate = false;
            LambdaUpdateWrapper<TestPoint> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(TestPoint::getId, testPoint.getId());
            
            // 检查并修复 type 字段
            if (testPoint.getType() == null) {
                TestPointTypeEnum typeEnum = TestPointTypeEnum.getByMtValue(testPoint.getMt());
                updateWrapper.set(TestPoint::getType, typeEnum.getCode());
                needUpdate = true;
                log.debug("修复测点 {} 缺失的类型字段：设置为 {}", testPoint.getId(), typeEnum.getName());
            }
            
            // 检查 type 和 mt 的一致性
            if (testPoint.getType() != null && testPoint.getMt() != null) {
                TestPointTypeEnum expectedType = TestPointTypeEnum.getByMtValue(testPoint.getMt());
                if (!expectedType.getCode().equals(testPoint.getType())) {
                    updateWrapper.set(TestPoint::getType, expectedType.getCode());
                    needUpdate = true;
                    log.debug("修复测点 {} 类型不一致：{} -> {} (mt={})", 
                        testPoint.getId(), testPoint.getType(), expectedType.getCode(), testPoint.getMt());
                }
            }
            
            if (needUpdate) {
                int result = testPointMapper.update(null, updateWrapper);
                if (result > 0) {
                    fixCount++;
                }
            }
        }
        
        log.info("验证并修复测点类型数据完成，共修复 {} 个测点", fixCount);
        return fixCount;
    }
}
