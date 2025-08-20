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
import org.dromara.hm.domain.TestPointData;
import org.dromara.hm.domain.bo.TestPointDataBo;
import org.dromara.hm.domain.vo.TestPointDataVo;
import org.dromara.hm.mapper.TestPointDataMapper;
import org.dromara.hm.service.ITestPointDataService;
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
public class TestPointDataServiceImpl extends ServiceImpl<TestPointDataMapper, TestPointData> implements ITestPointDataService {

    private final TestPointDataMapper baseMapper;

    @Override
    public TestPointDataVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<TestPointDataVo> queryPageList(TestPointDataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestPointData> lqw = buildQueryWrapper(bo);
        Page<TestPointDataVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public TableDataInfo<TestPointDataVo> customPageList(TestPointDataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<TestPointData> lqw = buildQueryWrapper(bo);
        Page<TestPointDataVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<TestPointDataVo> queryList(TestPointDataBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<TestPointData> buildQueryWrapper(TestPointDataBo bo) {
        LambdaQueryWrapper<TestPointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getKksCode()), TestPointData::getKksCode, bo.getKksCode());
        lqw.eq(bo.getFrequency() != null, TestPointData::getFrequency, bo.getFrequency());
        lqw.eq(bo.getPulseCount() != null, TestPointData::getPulseCount, bo.getPulseCount());
        lqw.eq(bo.getMagnitude() != null, TestPointData::getMagnitude, bo.getMagnitude());
        lqw.eq(StringUtils.isNotBlank(bo.getAlarmType()), TestPointData::getAlarmType, bo.getAlarmType());
        lqw.eq(bo.getSt() != null, TestPointData::getSt, bo.getSt());
        lqw.eq(bo.getAcquisitionTime() != null, TestPointData::getAcquisitionTime, bo.getAcquisitionTime());
        lqw.eq(StringUtils.isNotBlank(bo.getPdtypeSite()), TestPointData::getPdtypeSite, bo.getPdtypeSite());
        lqw.eq(StringUtils.isNotBlank(bo.getPdtypePlatform()), TestPointData::getPdtypePlatform, bo.getPdtypePlatform());
        lqw.orderByDesc(TestPointData::getAcquisitionTime);
        return lqw;
    }

    @Override
    public Boolean insertByBo(TestPointDataBo bo) {
        TestPointData add = MapstructUtils.convert(bo, TestPointData.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(TestPointDataBo bo) {
        TestPointData update = MapstructUtils.convert(bo, TestPointData.class);
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
    public Boolean saveBatch(List<TestPointData> list) {
        return baseMapper.insertBatch(list);
    }

    @Override
    public List<TestPointDataVo> queryByKksCode(String kksCode) {
        LambdaQueryWrapper<TestPointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestPointData::getKksCode, kksCode);
        lqw.orderByDesc(TestPointData::getAcquisitionTime);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<TestPointDataVo> queryByKksCodeAndTimeRange(String kksCode, String startTime, String endTime) {
        LambdaQueryWrapper<TestPointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestPointData::getKksCode, kksCode);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (StringUtils.isNotBlank(startTime)) {
            LocalDateTime start = LocalDateTime.parse(startTime, formatter);
            lqw.ge(TestPointData::getAcquisitionTime, start);
        }
        if (StringUtils.isNotBlank(endTime)) {
            LocalDateTime end = LocalDateTime.parse(endTime, formatter);
            lqw.le(TestPointData::getAcquisitionTime, end);
        }

        lqw.orderByDesc(TestPointData::getAcquisitionTime);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<TestPointDataVo> queryByAlarmStatus(String alarmType, Integer st) {
        LambdaQueryWrapper<TestPointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(alarmType), TestPointData::getAlarmType, alarmType);
        lqw.eq(st != null, TestPointData::getSt, st);
        lqw.orderByDesc(TestPointData::getAcquisitionTime);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public TestPointDataVo getLatestByKksCode(String kksCode) {
        LambdaQueryWrapper<TestPointData> lqw = Wrappers.lambdaQuery();
        lqw.eq(TestPointData::getKksCode, kksCode);
        lqw.orderByDesc(TestPointData::getAcquisitionTime);
        lqw.last("LIMIT 1");
        return baseMapper.selectVoOne(lqw);
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(TestPointData entity) {
        // 校验KKS编码不能为空
        if (StringUtils.isBlank(entity.getKksCode())) {
            throw new ServiceException("KKS编码不能为空");
        }

        // 可以添加其他业务校验逻辑
        // 例如：校验数据格式、范围等
    }

}
