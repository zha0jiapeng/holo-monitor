package org.dromara.job.sd400mp;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.constant.Tag;
import org.dromara.hm.service.ITestpointService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SD400MP测点数据同步任务
 *
 * @author Mashir0
 */
@RequiredArgsConstructor
@Component
@Slf4j
@JobExecutor(name = "TestpointExecutor")
public class TestpointExecutor {

    private final ITestpointService testPointService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        SyncResult syncResult = new SyncResult();

        try {
            JSONObject entries = SD400MPUtils.equipmentList(null, false);
            JSONArray data = entries.getJSONArray("data");
            for (Object equipment : data) {
                processSingleEquipment((JSONObject)equipment, syncResult);
            }

            String resultMessage = String.format("测点数据同步完成，共处理 %d 个设备，同步 %d 个测点，成功 %d 个，失败 %d 个",
                data.size(), syncResult.totalTestPoints, syncResult.successCount, syncResult.errorCount);

            log.info(resultMessage);
            return ExecuteResult.success(resultMessage);

        } catch (Exception e) {
            log.error("测点数据同步任务执行失败", e);
            return ExecuteResult.failure("测点数据同步任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 处理单个设备的所有测点
     */
    private void processSingleEquipment(JSONObject equipment, SyncResult syncResult) {
        try {
            log.info("开始处理设备 [{}] 的测点数据", equipment.get("name"));

            JSONObject testPointListResponse = SD400MPUtils.testPointList(Long.valueOf(equipment.get("id").toString()));
            if (!isSuccessResponse(testPointListResponse)) {
                log.warn("设备 [{}] 获取测点列表失败，响应码: {}",
                    equipment.get("name"), testPointListResponse.getInt("code"));
                return;
            }

            JSONArray testPoints = testPointListResponse.getJSONArray("data");
            syncResult.totalTestPoints += testPoints.size();
            log.info("设备 [{}] 共有 {} 个测点",  equipment.get("name"), testPoints.size());

            for (Object testPointObj : testPoints) {
                processSingleTestPoint((JSONObject) testPointObj, equipment, syncResult);
            }

        } catch (Exception e) {
            log.error("处理设备 [{}] 时发生异常: {}",  equipment.get("name"), e.getMessage(), e);
        }
    }

    /**
     * 处理单个测点
     */
    private void processSingleTestPoint(JSONObject testPoint, JSONObject equipment, SyncResult syncResult) {
        try {
            String testPointId = testPoint.getStr("id");
            if (testPointId == null) {
                log.warn("测点缺少ID，跳过处理");
                syncResult.errorCount++;
                return;
            }

            // 获取测点的监测类型(mt)
            int mt = fetchTestPointMt(testPointId);
            testPoint.set("mt", mt);

            log.debug("测点 [{}] 的 mt 值: {}", testPointId, mt);

            // 处理测点数据
            boolean result = processTestPoint(testPoint);
            if (result) {
                syncResult.successCount++;
                log.debug("测点 [{}] 处理成功", testPointId);
            } else {
                syncResult.errorCount++;
                log.warn("测点 [{}] 处理失败", testPointId);
            }

        } catch (Exception e) {
            syncResult.errorCount++;
            log.error("处理测点时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取测点的监测类型(mt)值
     */
    private int fetchTestPointMt(String testPointId) {
        try {
            List<String> tags = new ArrayList<>();
            tags.add(Tag.MT);
            JSONObject dataResponse = SD400MPUtils.data(Long.valueOf(testPointId), tags, null);
            if (!isSuccessResponse(dataResponse)) {
                log.warn("获取测点 [{}] 数据失败，响应码: {}", testPointId, dataResponse.getInt("code"));
                return -1;
            }

            return extractMtFromResponse(dataResponse)
                .orElseGet(() -> {
                    log.debug("测点 [{}] 未找到 mt 值，使用默认值 -1", testPointId);
                    return -1;
                });

        } catch (Exception e) {
            log.error("获取测点 [{}] 的 mt 值时发生异常: {}", testPointId, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 从响应数据中提取mt值
     */
    private Optional<Integer> extractMtFromResponse(JSONObject dataResponse) {
        try {
            JSONObject data = dataResponse.getJSONObject("data");
            if (data == null) {
                return Optional.empty();
            }

            JSONArray groups = data.getJSONArray("groups");
            if (groups == null) {
                return Optional.empty();
            }

            for (Object groupObj : groups) {
                JSONObject group = (JSONObject) groupObj;
                JSONArray online = group.getJSONArray("online");
                if (online == null) {
                    continue;
                }

                for (Object itemObj : online) {
                    Map<String, Object> item = (Map<String, Object>) itemObj;
                    if (Tag.MT.equals(item.get("key"))) {
                        Object val = item.get("val");
                        if (val != null) {
                            return Optional.of(Integer.valueOf(val.toString()));
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("提取 mt 值时发生异常: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * 检查响应是否成功
     */
    private boolean isSuccessResponse(JSONObject response) {
        return response != null && response.getInt("code") == 200;
    }

    /**
     * 处理单个测点数据，使用独立事务
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean processTestPoint(JSONObject pointData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(pointData.toString());
            return testPointService.importFromJson(jsonNode);
        } catch (Exception e) {
            log.error("处理测点数据失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 同步结果统计
     */
    private static class SyncResult {
        int totalTestPoints = 0;
        int successCount = 0;
        int errorCount = 0;
    }
}
