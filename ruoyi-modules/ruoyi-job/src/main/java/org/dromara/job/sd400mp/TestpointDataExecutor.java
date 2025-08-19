package org.dromara.job.sd400mp;

import cn.hutool.json.JSONObject;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
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
@JobExecutor(name = "TestpointDataExecutor")
public class TestpointDataExecutor {

    private final ITestPointDataService testPointService;

    private final IEquipmentService equipmentService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
            return ExecuteResult.success();
//        }
    }
}
