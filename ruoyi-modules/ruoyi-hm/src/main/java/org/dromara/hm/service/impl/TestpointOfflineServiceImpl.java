package org.dromara.hm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hm.domain.TestpointOffline;
import org.dromara.hm.domain.bo.TestpointOfflineBo;
import org.dromara.hm.domain.vo.TestpointOfflineVo;
import org.dromara.hm.mapper.TestpointOfflineMapper;
import org.dromara.hm.service.ITestpointOfflineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 测点离线记录Service业务层处理
 *
 * @author Mashir0
 * @date 2025-01-27
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TestpointOfflineServiceImpl extends ServiceImpl<TestpointOfflineMapper, TestpointOffline> implements ITestpointOfflineService {

    private final TestpointOfflineMapper baseMapper;

    /**
     * 查询单个
     */
    @Override
    public TestpointOfflineVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询列表
     */
    @Override
    public TableDataInfo<TestpointOfflineVo> queryPageList(TestpointOfflineBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestpointOffline> lqw = buildQueryWrapper(bo);
        Page<TestpointOfflineVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<TestpointOfflineVo> customPageList(TestpointOfflineBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestpointOffline> lqw = buildQueryWrapper(bo);
        Page<TestpointOfflineVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询列表
     */
    @Override
    public List<TestpointOfflineVo> queryList(TestpointOfflineBo bo) {
        LambdaQueryWrapper<TestpointOffline> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<TestpointOffline> buildQueryWrapper(TestpointOfflineBo bo) {
        LambdaQueryWrapper<TestpointOffline> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getKksCode()), TestpointOffline::getKksCode, bo.getKksCode());
        lqw.eq(bo.getOfflineTime() != null, TestpointOffline::getOfflineTime, bo.getOfflineTime());
        lqw.eq(bo.getRecoveryTime() != null, TestpointOffline::getRecoveryTime, bo.getRecoveryTime());
        lqw.eq(bo.getStatus() != null, TestpointOffline::getStatus, bo.getStatus());
        lqw.eq(bo.getOfflineJudgmentThreshold() != null, TestpointOffline::getOfflineJudgmentThreshold, bo.getOfflineJudgmentThreshold());
        lqw.orderByDesc(TestpointOffline::getCreateTime);
        return lqw;
    }

    /**
     * 新增测点离线记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(TestpointOfflineBo bo) {
        TestpointOffline add = MapstructUtils.convert(bo, TestpointOffline.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改测点离线记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(TestpointOfflineBo bo) {
        TestpointOffline update = MapstructUtils.convert(bo, TestpointOffline.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(TestpointOffline entity) {
        // 在这里进行实体保存前的数据校验，根据业务规则自行实现
    }

    /**
     * 批量删除测点离线记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
        }
        return removeBatchByIds(ids);
    }

    /**
     * 记录测点离线
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean recordOffline(Long equipmentId, String kksCode, Integer offlineJudgmentThreshold, LocalDateTime offlineTime) {
        // 检查是否已经存在未恢复的离线记录
        if (isOfflineRecordExists(kksCode)) {
            log.debug("测点 {} 已存在未恢复的离线记录", kksCode);
            return false;
        }

        TestpointOffline offlineRecord = new TestpointOffline();
        offlineRecord.setKksCode(kksCode);
        offlineRecord.setEquipmentId(equipmentId);
        offlineRecord.setOfflineTime(offlineTime);
        offlineRecord.setStatus(1); // 1-离线中
        offlineRecord.setOfflineJudgmentThreshold(offlineJudgmentThreshold);

        boolean flag = baseMapper.insert(offlineRecord) > 0;
        if (flag) {
            log.info("记录测点 {} 离线，离线时间: {}", kksCode, offlineTime);
        }
        return flag;
    }

    /**
     * 记录测点恢复在线
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean recordRecovery(String kksCode) {
        // 查找当前未恢复的离线记录
        LambdaQueryWrapper<TestpointOffline> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestpointOffline::getKksCode, kksCode);
        lqw.eq(TestpointOffline::getStatus, 1); // 1-离线中
        lqw.orderByDesc(TestpointOffline::getOfflineTime);
        lqw.last("LIMIT 1");

        TestpointOffline offlineRecord = baseMapper.selectOne(lqw);
        if (ObjectUtil.isNull(offlineRecord)) {
            log.debug("测点 {} 没有找到未恢复的离线记录", kksCode);
            return false;
        }

        LocalDateTime recoveryTime = LocalDateTime.now();
        offlineRecord.setRecoveryTime(recoveryTime);
        offlineRecord.setStatus(0); // 0-已恢复

        // 计算离线持续时长（秒）
        if (offlineRecord.getOfflineTime() != null) {
            Duration duration = Duration.between(offlineRecord.getOfflineTime(), recoveryTime);
            offlineRecord.setOfflineDuration(duration.getSeconds());
        }

        boolean flag = baseMapper.updateById(offlineRecord) > 0;
        if (flag) {
            log.info("记录测点 {} 恢复在线，恢复时间: {}，离线持续时长: {}秒",
                kksCode, recoveryTime, offlineRecord.getOfflineDuration());
        }
        return flag;
    }

    /**
     * 检查是否存在离线记录（未恢复的）
     */
    @Override
    public Boolean isOfflineRecordExists(String kksCode) {
        LambdaQueryWrapper<TestpointOffline> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestpointOffline::getKksCode, kksCode);
        lqw.eq(TestpointOffline::getStatus, 1); // 1-离线中
        return baseMapper.selectCount(lqw) > 0;
    }

    /**
     * 根据测点编码查询当前离线记录
     */
    @Override
    public TestpointOfflineVo queryCurrentOfflineByKksCode(String kksCode) {
        LambdaQueryWrapper<TestpointOffline> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestpointOffline::getKksCode, kksCode);
        lqw.eq(TestpointOffline::getStatus, 1); // 1-离线中
        lqw.orderByDesc(TestpointOffline::getOfflineTime);
        lqw.last("LIMIT 1");
        return baseMapper.selectVoOne(lqw);
    }

    /**
     * 根据测点编码查询历史离线记录
     */
    @Override
    public List<TestpointOfflineVo> queryHistoryByKksCode(String kksCode) {
        LambdaQueryWrapper<TestpointOffline> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestpointOffline::getKksCode, kksCode);
        lqw.orderByDesc(TestpointOffline::getOfflineTime);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 获取离线状态统计
     */
    @Override
    public List<TestpointOfflineVo> getOfflineStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<TestpointOffline> lqw = Wrappers.lambdaQuery();
        lqw.ge(startTime != null, TestpointOffline::getOfflineTime, startTime);
        lqw.le(endTime != null, TestpointOffline::getOfflineTime, endTime);
        lqw.orderByDesc(TestpointOffline::getOfflineTime);
        return baseMapper.selectVoList(lqw);
    }
}