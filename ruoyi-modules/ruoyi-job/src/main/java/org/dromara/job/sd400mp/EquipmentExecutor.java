package org.dromara.job.sd400mp;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.service.IEquipmentService;
import org.dromara.hm.service.IHierarchyService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    private final IHierarchyService hierarchyService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        log.info("开始执行设备和层级数据同步任务...");
        try {
            // 获取SD400MP数据
            JSONObject response = SD400MPUtils.equipmentList(null, false);
            log.info("获取到SD400MP数据，共{}条记录", response.getJSONArray("data").size());

            // 分离层级和设备数据
            JSONArray dataArray = response.getJSONArray("data");
            List<JSONObject> hierarchyList = new ArrayList<>();
            List<JSONObject> equipmentList = new ArrayList<>();

            for (Object item : dataArray) {
                JSONObject itemObj = (JSONObject) item;
                int type = itemObj.getInt("type", 0);

                if (type == 0 || type == 1) {
                    // 层级数据
                    hierarchyList.add(itemObj);
                } else if (type == 2) {
                    // 设备数据
                    equipmentList.add(itemObj);
                }
            }

            log.info("分离完成：层级{}条，设备{}条", hierarchyList.size(), equipmentList.size());

            // 第一步：同步层级数据
            if (!hierarchyList.isEmpty()) {
                log.info("开始同步层级数据...");
                boolean hierarchyResult = hierarchyService.importFromJson(createJsonData(hierarchyList));
                if (!hierarchyResult) {
                    log.error("层级数据同步失败");
                    return ExecuteResult.failure("层级数据同步失败");
                }
                log.info("层级数据同步成功");
            }

            // 第二步：同步设备数据
            if (!equipmentList.isEmpty()) {
                log.info("开始同步设备数据...");
                boolean equipmentResult = equipmentService.importEquipmentsFromJson(createJsonData(equipmentList));
                if (!equipmentResult) {
                    log.error("设备数据同步失败");
                    return ExecuteResult.failure("设备数据同步失败");
                }
                log.info("设备数据同步成功");
            }

            return ExecuteResult.success("层级和设备数据同步成功");
        } catch (Exception e) {
            log.error("数据同步任务执行失败", e);
            return ExecuteResult.failure("数据同步任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 创建JSON数据格式
     */
    private String createJsonData(List<JSONObject> dataList) {
        JSONObject jsonData = new JSONObject();
        jsonData.putOnce("data", dataList);
        return jsonData.toString();
    }
}
