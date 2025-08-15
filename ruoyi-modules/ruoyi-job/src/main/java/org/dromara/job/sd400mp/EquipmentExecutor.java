package org.dromara.job.sd400mp;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.service.IEquipmentService;
import org.dromara.hm.service.ITestPointService;
import org.dromara.hm.domain.vo.EquipmentVo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SD400MP设备测点同步任务
 *
 * @author Mashir0
 */
@RequiredArgsConstructor
@Component
@Slf4j
@JobExecutor(name = "EquipmentExecutor")
public class EquipmentExecutor {

    private final IEquipmentService equipmentService;
    //private final ITestPointService testPointService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        log.info("开始执行设备数据同步任务...");
        try {
            // 获取设备列表数据
            JSONObject equipmentListJson = SD400MPUtils.equipmentList(null, false);
            log.info("设备列表:{}", equipmentListJson);

            // 导入设备数据到数据库
            boolean result = equipmentService.importFromJson(equipmentListJson.toString());

            if (result) {
                log.info("设备数据导入成功");

                return ExecuteResult.success("设备和测点数据导入成功");
            } else {
                log.error("设备数据导入失败");
                return ExecuteResult.failure("设备数据导入失败");
            }
        } catch (Exception e) {
            log.error("设备数据同步任务执行失败", e);
            return ExecuteResult.failure("设备数据同步任务执行失败: " + e.getMessage());
        }
    }
}
