package org.dromara.job.sd400mp;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.service.IEquipmentService;
import org.dromara.hm.service.ITestPointService;
import org.springframework.stereotype.Component;

import java.util.List;

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

    private final ITestPointService testPointService;

    private final IEquipmentService equipmentService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        try {
            List<Equipment> equipmentList = equipmentService.getEquipmentsByType(2);
            int totalTestPoints = 0;

            for (Equipment equipment : equipmentList) {
                try {
                    JSONObject testPointListJson = SD400MPUtils.testPointList(equipment.getId());

                    if (testPointListJson.getInt("code") == 200) {
                        Object dataObj = testPointListJson.get("data");
                        if (dataObj instanceof List) {
                            totalTestPoints += ((List<?>) dataObj).size();
                        }
                        testPointService.importFromJson(testPointListJson.toString());
                    }
                } catch (Exception e) {
                    log.error("处理设备 [{}] 时发生异常: {}", equipment.getName(), e.getMessage());
                }
            }

            return ExecuteResult.success(String.format("测点数据同步完成，共处理 %d 个设备，同步 %d 个测点",
                equipmentList.size(), totalTestPoints));

        } catch (Exception e) {
            log.error("测点数据同步任务执行失败", e);
            return ExecuteResult.failure("测点数据同步任务执行失败: " + e.getMessage());
        }
    }
}
