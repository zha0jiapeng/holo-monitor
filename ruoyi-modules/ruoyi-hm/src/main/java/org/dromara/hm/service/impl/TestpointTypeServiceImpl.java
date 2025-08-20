package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hm.domain.Testpoint;
import org.dromara.hm.domain.vo.TestpointTypeVo;
import org.dromara.hm.enums.TestpointTypeEnum;
import org.dromara.hm.mapper.TestpointMapper;
import org.dromara.hm.service.ITestpointTypeService;
import org.dromara.hm.utils.TestpointTypeUtils;
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
public class TestpointTypeServiceImpl implements ITestpointTypeService {

    private final TestpointMapper testPointMapper;

    @Override
    public List<TestpointTypeVo> getAllTestpointTypes() {
        // 获取各类型的测点数量统计
        Map<String, Long> typeStatistics = getTestpointTypeStatistics();

        return Arrays.stream(TestpointTypeEnum.values())
            .map(typeEnum -> TestpointTypeVo.builder()
                .code(typeEnum.getCode())
                .name(typeEnum.getName())
                .description(typeEnum.getDescription())
                .mtValues(typeEnum.getMtValues())
                .mtRangeText(TestpointTypeUtils.getMtRangeDescription(typeEnum.getCode()))
                .testPointCount(typeStatistics.getOrDefault(typeEnum.getName(), 0L))
                .isDefault(TestpointTypeEnum.OTHER.equals(typeEnum))
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getTestpointTypeStatistics() {
        LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
        wrapper.select(Testpoint::getType);

        List<Testpoint> testPoints = testPointMapper.selectList(wrapper);

        return testPoints.stream()
            .map(Testpoint::getType)
            .map(TestpointTypeEnum::getByCode)
            .filter(typeEnum -> typeEnum != null)
            .collect(Collectors.groupingBy(
                TestpointTypeEnum::getName,
                Collectors.counting()
            ));
    }

    @Override
    public Map<String, Long> getTestpointTypeDistributionByEquipment(Long equipmentId) {
        LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Testpoint::getEquipmentId, equipmentId)
               .select(Testpoint::getType);

        List<Testpoint> testPoints = testPointMapper.selectList(wrapper);

        return testPoints.stream()
            .map(Testpoint::getType)
            .map(TestpointTypeEnum::getByCode)
            .filter(typeEnum -> typeEnum != null)
            .collect(Collectors.groupingBy(
                TestpointTypeEnum::getName,
                Collectors.counting()
            ));
    }

    @Override
    public Long getTestpointCountByType(Integer typeCode) {
        LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Testpoint::getType, typeCode);

        return testPointMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long batchUpdateTestpointTypeByMt() {
        log.info("开始批量更新测点类型...");

        LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
        wrapper.select(Testpoint::getId, Testpoint::getMt, Testpoint::getType);

        List<Testpoint> testPoints = testPointMapper.selectList(wrapper);

        long updateCount = 0;

        for (Testpoint testPoint : testPoints) {
            Integer currentMt = testPoint.getMt();
            Integer currentType = testPoint.getType();

            TestpointTypeEnum expectedType = TestpointTypeEnum.getByMtValue(currentMt);
            Integer expectedTypeCode = expectedType.getCode();

            // 如果类型不匹配，则更新
            if (!expectedTypeCode.equals(currentType)) {
                LambdaUpdateWrapper<Testpoint> updateWrapper = Wrappers.lambdaUpdate();
                updateWrapper.eq(Testpoint::getId, testPoint.getId())
                           .set(Testpoint::getType, expectedTypeCode);

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
    public Long validateAndFixTestpointTypes() {
        log.info("开始验证并修复测点类型数据...");

        LambdaQueryWrapper<Testpoint> wrapper = Wrappers.lambdaQuery();
        List<Testpoint> testPoints = testPointMapper.selectList(wrapper);

        long fixCount = 0;

        for (Testpoint testPoint : testPoints) {
            boolean needUpdate = false;
            LambdaUpdateWrapper<Testpoint> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(Testpoint::getId, testPoint.getId());

            // 检查并修复 type 字段
            if (testPoint.getType() == null) {
                TestpointTypeEnum typeEnum = TestpointTypeEnum.getByMtValue(testPoint.getMt());
                updateWrapper.set(Testpoint::getType, typeEnum.getCode());
                needUpdate = true;
                log.debug("修复测点 {} 缺失的类型字段：设置为 {}", testPoint.getId(), typeEnum.getName());
            }

            // 检查 type 和 mt 的一致性
            if (testPoint.getType() != null && testPoint.getMt() != null) {
                TestpointTypeEnum expectedType = TestpointTypeEnum.getByMtValue(testPoint.getMt());
                if (!expectedType.getCode().equals(testPoint.getType())) {
                    updateWrapper.set(Testpoint::getType, expectedType.getCode());
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
