package org.dromara.job.sd400mp;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.DataPointBean;
import org.dromara.common.core.utils.sd400mp.MPIDMultipleJson;
import org.dromara.common.core.utils.sd400mp.MPBinaryConverter;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.service.*;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * SD400MP测点数据同步任务
 *
 * @author Mashir0
 */
@RequiredArgsConstructor
@Component
@Slf4j
@JobExecutor(name = "ReportSyncExecutor")
public class ReportSyncExecutor {

    private final IHierarchyService hierarchyService;


    public ExecuteResult jobExecute(JobArgs jobArgs) {
        List<Hierarchy> list = hierarchyService.list(new LambdaQueryWrapper<Hierarchy>().eq(Hierarchy::getTypeId, 19));
        List<String> list1 = list.stream().map(Hierarchy::getCode).toList();
        List<Long> ids = new ArrayList<>();
        for (String kksCode : list1) {
            JSONObject entries = SD400MPUtils.testpointFind(kksCode);
            if(entries.getInt("code") == 200) {
                String id = entries.getJSONObject("data").getStr("id");
                ids.add(Long.valueOf(id));
            }
        }
        MPIDMultipleJson mpidMultipleJson = MPIDMultipleJson.create(ids);
        JSONObject events = SD400MPUtils.events("37", "2025-01-01T05:53:35.224Z", "2025-09-30T05:53:35.224Z", null, true);

        // 解析payload数据示例
        if (events != null && events.getInt("code") == 200) {
            parseEventPayloads(events);
        }

        return ExecuteResult.success();
    }

    /**
     * 解析事件响应中的payload数据
     * @param events 事件响应数据
     */
    private void parseEventPayloads(JSONObject events) {
        try {
            JSONObject data = events.getJSONObject("data");
            if (data == null) {
                log.warn("事件响应数据为空");
                return;
            }

            // 遍历设备
            for (Object eqObj : data.getJSONArray("equipment")) {
                JSONObject equipment = (JSONObject) eqObj;

                // 遍历测点
                for (Object tpObj : equipment.getJSONArray("testpoints")) {
                    JSONObject testpoint = (JSONObject) tpObj;

                    // 遍历标签
                    for (Object tagObj : testpoint.getJSONArray("tags")) {
                        JSONObject tag = (JSONObject) tagObj;

                        // 解析事件payload
                        if (tag.containsKey("events") && tag.getJSONObject("events") != null) {
                            JSONObject eventData = tag.getJSONObject("events");
                            if (eventData.containsKey("payload") && !eventData.getStr("payload").isEmpty()) {
                                String payload = eventData.getStr("payload");

                                // 使用MPBinaryConverter解析payload
                                List<DataPointBean> dataPoints = MPBinaryConverter.dataPointsFromBase64(payload);

                                log.info("测点: {}, 标签: {}, 解析到{}个数据点",
                                    testpoint.getStr("name"),
                                    tag.getStr("tag"),
                                    dataPoints.size());

                                // 处理解析后的数据点
                                processDataPoints(dataPoints);
                            }
                        }

                        // 解析卫星数据payload（如果存在）
                        if (tag.containsKey("satelite") && tag.getJSONObject("satelite") != null) {
                            JSONObject satelite = tag.getJSONObject("satelite");
                            if (satelite.containsKey("events") && satelite.getJSONObject("events") != null) {
                                JSONObject sateliteEvents = satelite.getJSONObject("events");
                                if (sateliteEvents.containsKey("payload") && !sateliteEvents.getStr("payload").isEmpty()) {
                                    String satellitePayload = sateliteEvents.getStr("payload");

                                    // 解析卫星数据payload
                                    List<DataPointBean> satellitePoints = MPBinaryConverter.dataPointsFromBase64(satellitePayload);

                                    log.info("测点ID: {}, 卫星标签: {}, 解析到{}个卫星数据点",
                                        testpoint.getStr("id"),
                                        satelite.getStr("tag"),
                                        satellitePoints.size());

                                    // 处理卫星数据点
                                    processSatelliteDataPoints(satellitePoints);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析事件payload异常", e);
        }
    }

    /**
     * 处理解析后的数据点
     * @param dataPoints 数据点列表
     */
    private void processDataPoints(List<DataPointBean> dataPoints) {
        // 这里可以添加具体的业务逻辑，比如保存到数据库、进行数据分析等
        for (DataPointBean point : dataPoints) {
            log.info("数据点 - 时间: {}, 数值: {}", point.getTime(), point.getValue());
            // TODO: 添加具体的业务处理逻辑
        }
    }

    /**
     * 处理卫星数据点
     * @param satellitePoints 卫星数据点列表
     */
    private void processSatelliteDataPoints(List<DataPointBean> satellitePoints) {
        // 这里可以添加卫星数据的业务逻辑
        for (DataPointBean point : satellitePoints) {
            log.info("卫星数据点 - 时间: {}, 数值: {}", DateUtil.format(point.getTime(), DatePattern.NORM_DATETIME_PATTERN), point.getValue());
            // TODO: 添加卫星数据的业务处理逻辑
        }
    }
}
