package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.dromara.hm.domain.TestpointData;
import org.dromara.hm.domain.bo.TestpointDataBo;
import org.dromara.hm.domain.vo.TestpointDataVo;
import org.dromara.hm.mapper.TestpointDataMapper;
import org.dromara.hm.service.ITestpointDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 测点数据Service业务层处理
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TestpointDataServiceImpl extends ServiceImpl<TestpointDataMapper, TestpointData> implements ITestpointDataService {

    private final TestpointDataMapper baseMapper;

    @Override
    public TestpointDataVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<TestpointDataVo> queryPageList(TestpointDataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestpointData> lqw = buildQueryWrapper(bo);
        Page<TestpointDataVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public TableDataInfo<TestpointDataVo> customPageList(TestpointDataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestpointData> lqw = buildQueryWrapper(bo);
        Page<TestpointDataVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<TestpointDataVo> queryList(TestpointDataBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<TestpointData> buildQueryWrapper(TestpointDataBo bo) {
        LambdaQueryWrapper<TestpointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getKksCode()), TestpointData::getKksCode, bo.getKksCode());
        lqw.eq(bo.getFrequency() != null, TestpointData::getFrequency, bo.getFrequency());
        lqw.eq(bo.getPulseCount() != null, TestpointData::getPulseCount, bo.getPulseCount());
        lqw.eq(bo.getMagnitude() != null, TestpointData::getMagnitude, bo.getMagnitude());
        lqw.eq(StringUtils.isNotBlank(bo.getAlarmType()), TestpointData::getAlarmType, bo.getAlarmType());
        lqw.eq(bo.getSt() != null, TestpointData::getSt, bo.getSt());
        lqw.eq(bo.getAcquisitionTime() != null, TestpointData::getAcquisitionTime, bo.getAcquisitionTime());
        lqw.eq(StringUtils.isNotBlank(bo.getPdtypeSite()), TestpointData::getPdtypeSite, bo.getPdtypeSite());
        lqw.eq(StringUtils.isNotBlank(bo.getPdtypePlatform()), TestpointData::getPdtypePlatform, bo.getPdtypePlatform());
        lqw.orderByDesc(TestpointData::getAcquisitionTime);
        return lqw;
    }

    @Override
    public Boolean insertByBo(TestpointDataBo bo) {
        TestpointData add = MapstructUtils.convert(bo, TestpointData.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(TestpointDataBo bo) {
        TestpointData update = MapstructUtils.convert(bo, TestpointData.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 执行删除前校验
            // 可以在这里添加业务逻辑校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<TestpointData> list) {
        return baseMapper.insertBatch(list);
    }

    @Override
    public List<TestpointDataVo> queryByKksCode(String kksCode) {
        LambdaQueryWrapper<TestpointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestpointData::getKksCode, kksCode);
        lqw.orderByDesc(TestpointData::getAcquisitionTime);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<TestpointDataVo> queryByKksCodeAndTimeRange(String kksCode, String startTime, String endTime) {
        LambdaQueryWrapper<TestpointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestpointData::getKksCode, kksCode);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (StringUtils.isNotBlank(startTime)) {
            LocalDateTime start = LocalDateTime.parse(startTime, formatter);
            lqw.ge(TestpointData::getAcquisitionTime, start);
        }
        if (StringUtils.isNotBlank(endTime)) {
            LocalDateTime end = LocalDateTime.parse(endTime, formatter);
            lqw.le(TestpointData::getAcquisitionTime, end);
        }

        lqw.orderByDesc(TestpointData::getAcquisitionTime);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<TestpointDataVo> queryByAlarmStatus(String alarmType, Integer st) {
        LambdaQueryWrapper<TestpointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(alarmType), TestpointData::getAlarmType, alarmType);
        lqw.eq(st != null, TestpointData::getSt, st);
        lqw.orderByDesc(TestpointData::getAcquisitionTime);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public TestpointDataVo getLatestByKksCode(String kksCode) {
        LambdaQueryWrapper<TestpointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestpointData::getKksCode, kksCode);
        lqw.orderByDesc(TestpointData::getAcquisitionTime);
        lqw.last("LIMIT 1");
        return baseMapper.selectVoOne(lqw);
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(TestpointData entity) {
        // 校验KKS编码不能为空
        if (StringUtils.isBlank(entity.getKksCode())) {
            throw new ServiceException("KKS编码不能为空");
        }

        // 可以添加其他业务校验逻辑
        // 例如：校验数据格式、范围等
    }

}
