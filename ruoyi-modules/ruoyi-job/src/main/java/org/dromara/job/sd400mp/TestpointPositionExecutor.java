package org.dromara.job.sd400mp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.domain.Equipment;
import org.dromara.hm.domain.bo.TestPointBo;
import org.dromara.hm.service.IEquipmentService;
import org.dromara.hm.service.ITestPointService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * SD400MP测点数据同步任务
 *
 * @author Mashir0
 */
@RequiredArgsConstructor
@Component
@Slf4j
@JobExecutor(name = "TestpointPositionExecutor")
public class TestpointPositionExecutor {

    private final ITestPointService testPointService;

    private final IEquipmentService equipmentService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {

        try {
            List<Equipment> equipmentList = equipmentService.getEquipmentsByType(null);
            log.info("开始执行测点位置同步任务，共 {} 个设备", equipmentList.size());
            String fileId = null;
            for (Equipment equipment : equipmentList) {
                JSONObject file = SD400MPUtils.file(equipment.getId());
                JSONArray data = file.getJSONArray("data");
                for (Object datum : data) {
                    JSONObject item = (JSONObject)datum;
                    String name = item.getStr("name");
                    if(name.contains(".")){
                        String suffix = name.split("\\.")[1];
                        if(suffix.equals("glblm")){
                            fileId = item.getStr("id");
                        }
                    }
                }
                if(fileId==null) continue;
                List<TestPointBo> testPointList = new ArrayList<>();
                JSONObject modelInfo = SD400MPUtils.modelInfo(Long.valueOf(fileId));
                JSONObject model = modelInfo.getJSONObject("model");
                JSONArray sensors = model.getJSONArray("sensors");
                for (Object sensor : sensors) {
                    JSONObject item = (JSONObject)sensor;
                    JSONObject binding = item.getJSONObject("binding");
                    String testPointId = item.getStr("testPointId");
                    JSONObject pos = binding.getJSONObject("pos");
                    Float x = pos.getFloat("x");
                    Float y = pos.getFloat("y");
                    Float z = pos.getFloat("z");
                    TestPointBo testPoint = new TestPointBo();
                    testPoint.setId(Long.valueOf(testPointId));
                    testPoint.setPositionX(new BigDecimal(x));
                    testPoint.setPositionY(new BigDecimal(y));
                    testPoint.setPositionZ(new BigDecimal(z));
                    testPoint.setUpdateTime(new Date());
                    testPointList.add(testPoint);
                }
                testPointService.updateBatchByBo(testPointList);
            }

            return ExecuteResult.success("");

        } catch (Exception e) {
            log.error("测点数据位置同步任务执行失败", e);
            return ExecuteResult.failure("测点数据位置同步任务执行失败: " + e.getMessage());
        }
    }
}
