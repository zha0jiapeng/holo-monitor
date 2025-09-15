package org.dromara.job.sd400mp;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@JobExecutor(name = "LocationSyncExecutor")
public class LocationSyncExecutor {

    public static void main(String[] args) {
        JSONObject modelInfo = SD400MPUtils.modelInfo(2l);
        JSONObject model = modelInfo.getJSONObject("model");
        JSONArray sensors = model.getJSONArray("sensors");

        for (Object sensor : sensors) {
            JSONObject item = (JSONObject) sensor;
            JSONObject binding = item.getJSONObject("binding");
            if (binding == null) {
                continue;
            }

            String testPointId = item.getStr("testPointId");
            if (testPointId == null) {
                continue;
            }

            JSONObject pos = binding.getJSONObject("pos");
            if (pos == null) {
                continue;
            }

            try {
                Float x = pos.getFloat("x");
                Float y = pos.getFloat("y");
                Float z = pos.getFloat("z");

//                TestpointBo testPoint = new TestpointBo();
//                testPoint.setId(Long.valueOf(testPointId));
//                testPoint.setPositionX(x != null ? new BigDecimal(x) : null);
//                testPoint.setPositionY(y != null ? new BigDecimal(y) : null);
//                testPoint.setPositionZ(z != null ? new BigDecimal(z) : null);
//                testPoint.setUpdateTime(new Date());
//                testPoint.setPositionSource(PositionSourceEnum.SD400MP.getCode());
//                testPointList.add(testPoint);

            } catch (Exception e) {
                log.warn("处理传感器[{}]位置信息失败: {}", testPointId, e.getMessage());
            }
        }
    }
}
