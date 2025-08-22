package org.dromara.job.sd400mp;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hm.domain.Testpoint;
import org.dromara.hm.service.ITestpointService;
import org.dromara.hm.service.ITestpointOfflineService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 测点离线监控任务
 *
 * @author Mashir0
 */
@RequiredArgsConstructor
@Component
@Slf4j
@JobExecutor(name = "TestpointOfflineExecutor")
public class TestpointOfflineExecutor {

    private final ITestpointService testpointService;
    private final ITestpointOfflineService testpointOfflineService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        log.debug("开始执行测点离线状态监控任务...");

        try {
            List<Testpoint> testpoints = testpointService.list();
            if (testpoints.isEmpty()) {
                log.debug("没有找到测点数据，跳过离线监控");
                return ExecuteResult.success("没有测点数据需要监控");
            }

            int offlineCount = 0;
            int recoveryCount = 0;

            for (Testpoint testpoint : testpoints) {
                try {
                    if (checkAndRecordOfflineStatus(testpoint)) {
                        offlineCount++;
                    }

                    if (checkAndRecordRecoveryStatus(testpoint)) {
                        recoveryCount++;
                    }

                    // 检查并修正离线标识的一致性
                    checkAndFixOfflineFlagConsistency(testpoint);

                } catch (Exception e) {
                    log.error("检查测点 {} 状态时发生异常", testpoint.getKksCode(), e);
                }
            }

            String resultMessage = String.format("离线监控完成 - 新增离线: %d, 新增恢复: %d",
                offlineCount, recoveryCount);

            if (offlineCount > 0 || recoveryCount > 0) {
                log.info(resultMessage);
            } else {
                log.debug(resultMessage);
            }

            return ExecuteResult.success(resultMessage);

        } catch (Exception e) {
            log.error("执行测点离线监控任务时发生异常", e);
            return ExecuteResult.failure("任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 检查并记录测点离线状态
     *
     * @param testpoint 测点信息
     * @return true-新增离线记录, false-未新增记录
     */
    private boolean checkAndRecordOfflineStatus(Testpoint testpoint) {
        Integer offlineThreshold = testpoint.getOfflineJudgmentThreshold();
        if (offlineThreshold == null || offlineThreshold <= 0) {
            return false;
        }

        if (testpointOfflineService.isOfflineRecordExists(testpoint.getKksCode())) {
            return false;
        }

        LocalDateTime lastAcquisitionTime = testpoint.getLastAcquisitionTime();
        boolean isOffline = false;
        LocalDateTime offlineTime = null;

        if (lastAcquisitionTime == null) {
            // 没有任何数据，使用当前时间减去阈值作为离线时间
            offlineTime = LocalDateTime.now().minusHours(offlineThreshold);
            isOffline = true;
            log.debug("测点 {} 已离线，原因: 没有采集时间记录", testpoint.getKksCode());
        } else {
            LocalDateTime thresholdTime = LocalDateTime.now().minusHours(offlineThreshold);

            if (lastAcquisitionTime.isBefore(thresholdTime)) {
                // 使用最后一次采集数据的时间作为离线时间
                offlineTime = lastAcquisitionTime;
                isOffline = true;
                log.debug("测点 {} 已离线，最后采集时间: {}", testpoint.getKksCode(), lastAcquisitionTime);
            }
        }

        if (isOffline) {
            // 记录离线
            testpointOfflineService.recordOffline(testpoint.getEquipmentId(),testpoint.getKksCode(), offlineThreshold, offlineTime);

            // 更新测点的离线标识
            updateTestpointOfflineFlag(testpoint, 1);

            return true;
        }

        return false;
    }

    /**
     * 检查并记录测点恢复状态
     *
     * @param testpoint 测点信息
     * @return true-新增恢复记录, false-未新增记录
     */
    private boolean checkAndRecordRecoveryStatus(Testpoint testpoint) {
        if (!testpointOfflineService.isOfflineRecordExists(testpoint.getKksCode())) {
            return false;
        }

        Integer offlineThreshold = testpoint.getOfflineJudgmentThreshold();
        if (offlineThreshold == null || offlineThreshold <= 0) {
            return false;
        }

        LocalDateTime lastAcquisitionTime = testpoint.getLastAcquisitionTime();
        if (lastAcquisitionTime == null) {
            return false;
        }

        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(offlineThreshold);

        // 如果测点现在在线，说明已经恢复
        if (lastAcquisitionTime.isAfter(thresholdTime)) {
            // 记录恢复
            testpointOfflineService.recordRecovery(testpoint.getKksCode());

            // 更新测点的离线标识为在线
            updateTestpointOfflineFlag(testpoint, 0);

            log.debug("测点 {} 已恢复在线", testpoint.getKksCode());
            return true;
        }

        return false;
    }

    /**
     * 检查并修正离线标识的一致性
     *
     * @param testpoint 测点信息
     */
    private void checkAndFixOfflineFlagConsistency(Testpoint testpoint) {
        try {
            boolean hasOfflineRecord = testpointOfflineService.isOfflineRecordExists(testpoint.getKksCode());
            Integer currentFlag = testpoint.getOfflineFlag();

            // 如果没有离线记录但标识为离线，应该修正为在线
            if (!hasOfflineRecord && currentFlag != null && currentFlag == 1) {
                updateTestpointOfflineFlag(testpoint, 0);
                log.debug("修正测点 {} 离线标识: 无离线记录，标识应为在线", testpoint.getKksCode());
            }
            // 如果有离线记录但标识不是离线，应该修正为离线
            else if (hasOfflineRecord && (currentFlag == null || currentFlag != 1)) {
                updateTestpointOfflineFlag(testpoint, 1);
                log.debug("修正测点 {} 离线标识: 存在离线记录，标识应为离线", testpoint.getKksCode());
            }
        } catch (Exception e) {
            log.error("检查测点 {} 离线标识一致性时发生异常", testpoint.getKksCode(), e);
        }
    }

    /**
     * 更新测点的离线标识
     *
     * @param testpoint 测点信息
     * @param offlineFlag 离线标识：0-在线, 1-离线
     */
    private void updateTestpointOfflineFlag(Testpoint testpoint, Integer offlineFlag) {
        try {
            // 只有当标识发生变化时才更新
            if (!offlineFlag.equals(testpoint.getOfflineFlag())) {
                testpoint.setOfflineFlag(offlineFlag);
                testpointService.updateById(testpoint);

                String status = offlineFlag == 1 ? "离线" : "在线";
                log.debug("更新测点 {} 离线标识为: {}", testpoint.getKksCode(), status);
            }
        } catch (Exception e) {
            log.error("更新测点 {} 离线标识时发生异常", testpoint.getKksCode(), e);
        }
    }
}
