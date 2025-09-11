package org.dromara.job.sd400mp;

import cn.hutool.json.JSONObject;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.MPIDMultipleJson;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.service.*;
import org.dromara.hm.domain.sd400mp.MPEventList;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final EventParserService eventParserService;


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
        JSONObject events = SD400MPUtils.events("37", "2025-09-04T15:20:57.869+08:00", "2025-09-11T15:20:57.869+08:00", null, true);

        // 解析events数据，得到与JavaScript相同结构的结果
        if (events != null && events.getInt("code") == 200) {
            MPEventList eventList = eventParserService.parseEvents(events);
            if (eventList != null) {
                log.info("成功解析events，共{}个分组，{}个设备名称，{}个测点名称",
                        eventList.getGroups().size(),
                        eventList.getNamesEq().size(),
                        eventList.getNamesTp().size());

                // 这里可以进一步处理解析后的数据
                processEventList(eventList);
            } else {
                log.error("解析events失败");
            }
        } else {
            log.error("获取events数据失败");
        }

        return ExecuteResult.success();
    }

    /**
     * 处理解析后的事件列表
     *
     * @param eventList 解析后的事件列表
     */
    private void processEventList(MPEventList eventList) {
        log.info("=== 开始处理事件列表 ===");

        // 显示PDE类信息
        if (!eventList.getPdClasses().isEmpty()) {
            log.info("PDE类信息: {}", eventList.getPdClasses().size());
            eventList.getPdClasses().forEach((index, pdeClass) -> {
                log.debug("PDE类 [{}]: {}", index, pdeClass.getName());
            });
        }

        // 遍历所有事件分组
        eventList.getGroups().forEach((key, group) -> {
            log.info("事件分组: {}, 标签: {}, 事件数量: {}",
                    key, group.getTag().getTitle(), group.getEvents().size());

            // 统计每个分组的状态分布
            Map<Integer, Long> stateCount = group.getEvents().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            org.dromara.hm.domain.sd400mp.MPEvent::getState,
                            java.util.stream.Collectors.counting()));

            stateCount.forEach((state, count) -> {
                log.info("  状态 {}: {} 个事件", state, count);
            });

            // 显示前几个事件的详细信息
            group.getEvents().stream().limit(3).forEach(event -> {
                String equipmentName = eventList.getNamesEq().get(event.getEquipmentId());
                String testpointName = eventList.getNamesTp().get(event.getTestpointId());

                log.info("  事件详情 - 设备: {} ({}), 测点: {} ({}), 状态: {}, 开始: {}, 结束: {}, 卫星值: {}",
                        equipmentName, event.getEquipmentId(),
                        testpointName, event.getTestpointId(),
                        event.getState(), event.getStart(), event.getEnd(),
                        event.getSatelliteValue());
            });

            // 如果有卫星标签，显示信息
            if (group.getSatelliteTag() != null) {
                log.info("  卫星标签: {}, 显示设置数量: {}",
                        group.getSatelliteTag().getTitle(),
                        group.getDisplaySettings().size());
            }
        });

        // 统计总体信息
        int totalEvents = eventList.getGroups().values().stream()
                .mapToInt(group -> group.getEvents().size())
                .sum();

        log.info("=== 事件处理完成 - 总分组: {}, 总事件: {}, 设备: {}, 测点: {} ===",
                eventList.getGroups().size(), totalEvents,
                eventList.getNamesEq().size(), eventList.getNamesTp().size());

        // 这里可以添加更多的业务逻辑，比如：
        // 1. 将事件数据保存到数据库
        // 2. 发送事件通知
        // 3. 生成报告
        // 4. 更新统计信息等
    }
}
