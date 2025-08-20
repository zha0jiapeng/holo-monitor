package org.dromara.job.sd400mp;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.constant.ZdsFileConstants;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.domain.Testpoint;
import org.dromara.hm.domain.TestpointData;
import org.dromara.hm.domain.bo.TestpointBo;
import org.dromara.hm.domain.vo.TestpointVo;
import org.dromara.hm.dto.IndexFileDto;
import org.dromara.hm.dto.TransformedIndexDataDto;
import org.dromara.hm.service.IEquipmentService;
import org.dromara.hm.service.ITestpointDataService;
import org.dromara.hm.service.ITestpointService;
import org.dromara.hm.utils.JsonTransformUtils;
import org.dromara.hm.utils.ZdsUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SD400MP测点数据同步任务
 *
 * @author Mashir0
 */
@RequiredArgsConstructor
@Component
@Slf4j
@JobExecutor(name = "TestpointDataExecutor")
public class TestpointDataExecutor {

    private final ITestpointService testPointService;

    private final ITestpointDataService testPointDataService;

    // 添加并发控制机制，防止同一数据点重复处理
    private final ConcurrentHashMap<String, Object> dataPointLocks = new ConcurrentHashMap<>();

    @Value("${sync.default-start-time}")
    private String defaultStartTime;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        List<Testpoint> testpoints = testPointService.list();
        for (Testpoint testpoint : testpoints) {
            try {
                syncTestpointData(testpoint);
            } catch (Exception e) {
                log.error("同步测点 {} 的数据时发生异常", testpoint.getKksCode(), e);
            }
        }
        return ExecuteResult.success();
    }
    private void syncTestpointData(Testpoint testpoint) throws Exception {
        String kksCode = testpoint.getKksCode();
        LocalDateTime parse = LocalDateTime.parse(defaultStartTime, FORMATTER);
        TestpointData lastData = getLastTestpointDataSafely(kksCode, parse);

        LocalDateTime fromTime;
        if (lastData != null) {
            fromTime = lastData.getAcquisitionTime().plusSeconds(1);
        } else {
            fromTime = parse;
        }
        LocalDateTime toTime = LocalDateTime.now();
        String from = fromTime.format(FORMATTER);
        String to = toTime.format(FORMATTER);

        // 2. 调用index接口获取数据点
        JSONObject indexResponse = SD400MPUtils.index(kksCode, from, to);
        if (indexResponse == null || !indexResponse.containsKey("data")) {
            log.warn("获取KKS编码 '{}' 的数据索引失败。", kksCode);
            return;
        }
        JSONObject data = indexResponse.getJSONObject("data");
        String SD400APtestpointId = data.getJSONObject("id").getStr("id");
        JSONArray times = data.getJSONArray("time");
        if (times == null || times.isEmpty()) {
            log.info("KKS编码 '{}' 在时间范围 [{} - {}] 内没有新数据。", kksCode, from, to);
            return;
        }

        // 3. 遍历数据点并获取存储
        for (int i = 0; i < times.size(); i++) {
            String timeStr = times.getStr(i);
            processDataPoint(testpoint, kksCode, SD400APtestpointId, timeStr);
        }
    }


    private void processDataPoint(Testpoint testpoint, String kksCode, String SD400APtestpointId, String timeStr) throws Exception {
        LocalDateTime acquisitionTime = LocalDateTime.parse(timeStr, FORMATTER);
        if (!acquisitionTime.isBefore(DateUtil.parseLocalDateTime("2024-03-01 00:00:00"))) {
            return;
        }
        // 为每个数据点创建唯一锁键，防止并发处理同一数据点
        String lockKey = kksCode + "#" + timeStr;
        Object lock = dataPointLocks.computeIfAbsent(lockKey, k -> new Object());
        // 使用锁保证同一数据点的处理是原子性的
        synchronized (lock) {
            try {
                // 在锁内再次检查数据是否已存在，防止重复插入
                long count = testPointDataService.count(new LambdaQueryWrapper<TestpointData>()
                    .eq(TestpointData::getKksCode, testpoint.getKksCode())
                    .eq(TestpointData::getAcquisitionTime, acquisitionTime)
                );
                if (count > 0) {
                    log.info("数据已存在,跳过:KksCode={}, Time={}", testpoint.getKksCode(), timeStr);
                    return;
                }

                // 执行实际的数据处理逻辑
                processDataPointInternal(testpoint, kksCode, SD400APtestpointId, timeStr, acquisitionTime);

            } finally {
                // 处理完成后移除锁，避免内存泄漏
                dataPointLocks.remove(lockKey);
            }
        }
    }

    /**
     * 实际的数据处理逻辑，从原processDataPoint方法中提取
     */
    private void processDataPointInternal(Testpoint testpoint, String kksCode, String SD400APtestpointId, String timeStr, LocalDateTime acquisitionTime) throws Exception {
        // 4. 获取并解压dataset
        byte[] dataset = SD400MPUtils.dataset(SD400APtestpointId, timeStr);
        if (dataset == null || dataset.length == 0) {
            log.warn("获取数据集失败: KksCode={}, Time={}", testpoint.getKksCode(), timeStr);
            return;
        }

        Map<String, byte[]> unzippedFiles = ZdsUtils.unzip(dataset);
        byte[] indexJsonBytes = unzippedFiles.get(ZdsFileConstants.INDEX_JSON);

        if (indexJsonBytes == null) {
            log.warn("解压后的文件中缺少必需文件: " + ZdsFileConstants.INDEX_JSON);
            return;
        }

        // 5. 解析index.json并存储
        String indexJsonStr = new String(indexJsonBytes, StandardCharsets.UTF_8);
        IndexFileDto indexFile = JSONUtil.toBean(indexJsonStr, IndexFileDto.class);
        TransformedIndexDataDto transformedIndexJson = JsonTransformUtils.transformIndexJson(indexFile);

        TestpointData newData = new TestpointData();
        newData.setKksCode(kksCode);
        newData.setAcquisitionTime(acquisitionTime);
        newData.setCreateTime(new Date());
        newData.setZds(dataset);
        newData.setFrequency(transformedIndexJson.getF());
        newData.setPulseCount(transformedIndexJson.getCnt());
        newData.setSt(transformedIndexJson.getSt());
        newData.setMagnitude(transformedIndexJson.getMag());
        newData.setPdtypeSite(transformedIndexJson.getPdType());
        newData.setPdexpertSite(transformedIndexJson.getPdTypeJson().getBytes(StandardCharsets.UTF_8));

        testpoint.setLastMagnitude(transformedIndexJson.getMag());
        testpoint.setLastSt(transformedIndexJson.getSt());
        testpoint.setLastPdtypeSite(transformedIndexJson.getPdType());
        testpoint.setLastPdtypePlatform(null);
        testpoint.setLastAlarmType(0);
        testpoint.setLastAcquisitionTime(acquisitionTime);



        //平台诊断流程
        JSONObject pdexpertJson = performSelfDiagnosis(dataset);

        //自计算报警状态
        //自诊断或者zds中st不是0 那就进入报警流程
        boolean pdexpertFlag = pdexpertJson.containsKey("data");
        boolean ignore = false;
        if (transformedIndexJson.getSt()!= 0 || pdexpertFlag) {
            if(pdexpertFlag) {
                JSONObject data = pdexpertJson.getJSONObject("data");
                newData.setPdtypePlatform(data.getStr("className"));
                testpoint.setLastPdtypeSite(data.getStr("className"));
                newData.setPdexpertPlatform(pdexpertJson.toString().getBytes(StandardCharsets.UTF_8));
            }
            //1. 局放事件
            // (1) 局放类型pdtype（corona、floating、insulation、particle、external、unknown），计录为1个放电事件
            if ( pdexpertFlag ) {
                newData.setAlarmType(1);
                testpoint.setLastAlarmType(1);
            }
            // 局放幅值mag超过"特高频忽略阈值（报警设置界面的值）, 不计算报警。
            if (testpoint.getUhfIgnoreThreshold().compareTo(transformedIndexJson.getMag()) < 0){

                // (2) 当放电事件的局放mag值超过报警设置界面中的"特高频突变幅值"，则输出"突发型局放报警"
                if (testpoint.getUhfMutationThreshold().compareTo(transformedIndexJson.getMag()) < 0 && pdexpertFlag) {
                    newData.setAlarmType(2);
                    testpoint.setLastAlarmType(2);
                }
                Integer eventCountThresholdPeriod = testpoint.getEventCountThresholdPeriod();
                BigDecimal dischargeEventRatioThreshold = testpoint.getDischargeEventRatioThreshold();
                DateTime nowDate = DateUtil.date();
                DateTime dateBefore = DateUtil.offsetHour(nowDate, -eventCountThresholdPeriod);
                // (3) 按报警设置界面中的"事件数阈值周期（小时）"滚动计算，
                //     当（放电事件数之和）/（事件数阈值周期（小时）期间的数据条数）≥ 报警设置界面中的"放电事件数比例阈值%，
                //     且最后一条数据的局放幅值≥报警设置界面中的"特高频注意阈值"，则输出"持续性一级报警"； 分子是（1）
                List<TestpointData> list = testPointDataService.list(new LambdaQueryWrapper<TestpointData>()
                    .between(TestpointData::getAcquisitionTime, dateBefore, nowDate)
                    .ne(TestpointData::getAlarmType, 0)
                    .orderByDesc(TestpointData::getAcquisitionTime)
                );
                if (  !list.isEmpty()) {
                    if (new BigDecimal(list.size()).divide(new BigDecimal(eventCountThresholdPeriod), 2, RoundingMode.HALF_UP).compareTo(dischargeEventRatioThreshold) >= 0) {
                        if (list.get(0).getMagnitude().compareTo(testpoint.getUhfLevel1AlarmThreshold()) >= 0) {
                            newData.setAlarmType(3);
                            testpoint.setLastAlarmType(3);
                        }
                        if (list.get(0).getMagnitude().compareTo(testpoint.getUhfLevel2AlarmThreshold()) >= 0) {
                            newData.setAlarmType(4);
                            testpoint.setLastAlarmType(4);
                        }
                        if (list.get(0).getMagnitude().compareTo(testpoint.getUhfLevel3AlarmThreshold()) >= 0) {
                            newData.setAlarmType(5);
                            testpoint.setLastAlarmType(5);
                        }
                    }
                }
            }
        }
        testPointDataService.save(newData);
        testPointService.updateById(testpoint);
        log.info("成功同步并存储新数据: KksCode={}, Time={}", testpoint.getKksCode(), timeStr);
    }

    private JSONObject performSelfDiagnosis(byte[] dataset) throws IOException {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("dataset-", ".zds");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(dataset);
            }

            HttpResponse pdExpertHttpResponse = SD400MPUtils.pdexpert(tempFile);
            String pdExpertBody = pdExpertHttpResponse.body();
            return JSONUtil.parseObj(pdExpertBody);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private TestpointData getLastTestpointDataSafely(String kksCode, LocalDateTime defaultStartTime) {
        try {
            // 优化查询策略：首先查询最近3个月的数据，避免大范围查询创建过多分表
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            LocalDateTime conservativeStartTime = threeMonthsAgo.isAfter(defaultStartTime) ? threeMonthsAgo : defaultStartTime;

            return testPointDataService.getOne(
                new LambdaQueryWrapper<TestpointData>()
                    .eq(TestpointData::getKksCode, kksCode)
                    .between(TestpointData::getAcquisitionTime, conservativeStartTime.format(FORMATTER), DateUtil.now())
                    .orderByDesc(TestpointData::getAcquisitionTime)
                    .last("limit 1"), false);
        } catch (Exception e) {
            log.warn("使用保守查询获取测点 {} 最新数据时发生异常，尝试使用逐月查询策略: {}", kksCode, e.getMessage());

            try {
                // 如果仍然异常，尝试逐月倒序查询，找到最新的有数据的月份
                LocalDateTime currentMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

                for (int i = 0; i < 12; i++) { // 最多查询12个月
                    LocalDateTime monthStart = currentMonth.minusMonths(i);
                    LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

                    // 确保不超过默认开始时间
                    if (monthEnd.isBefore(defaultStartTime)) {
                        break;
                    }

                    LocalDateTime actualStart = monthStart.isBefore(defaultStartTime) ? defaultStartTime : monthStart;

                    TestpointData monthData = testPointDataService.getOne(
                        new LambdaQueryWrapper<TestpointData>()
                            .eq(TestpointData::getKksCode, kksCode)
                            .between(TestpointData::getAcquisitionTime, actualStart.format(FORMATTER), monthEnd.format(FORMATTER))
                            .orderByDesc(TestpointData::getAcquisitionTime)
                            .last("limit 1"), false);

                    if (monthData != null) {
                        log.info("通过逐月查询找到测点 {} 在 {} 月份的最新数据", kksCode, monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                        return monthData;
                    }
                }

                log.info("测点 {} 在过去12个月中未找到任何数据，将从默认开始时间开始同步", kksCode);
                return null;
            } catch (Exception e3) {
                log.error("使用逐月查询策略获取测点 {} 最新数据时发生异常，将从默认开始时间开始同步: {}", kksCode, e3.getMessage());
                return null;
            }
        }
    }

}
