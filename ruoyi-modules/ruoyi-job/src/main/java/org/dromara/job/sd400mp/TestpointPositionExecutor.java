//package org.dromara.job.sd400mp;
//
//import cn.hutool.json.JSONArray;
//import cn.hutool.json.JSONObject;
//import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
//import com.aizuda.snailjob.client.job.core.dto.JobArgs;
//import com.aizuda.snailjob.client.model.ExecuteResult;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
//import org.dromara.hm.domain.Equipment;
//import org.dromara.hm.domain.bo.HierarchyBo;
//import org.dromara.hm.domain.bo.TestpointBo;
//import org.dromara.hm.domain.vo.HierarchyVo;
//import org.dromara.hm.enums.PositionSourceEnum;
//import org.dromara.hm.service.IHierarchyService;
//import org.dromara.hm.service.ITestpointService;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.util.*;
//
///**
// * SD400MP测点数据同步任务
// *
// * @author Mashir0
// */
//@RequiredArgsConstructor
//@Component
//@Slf4j
//@JobExecutor(name = "TestpointPositionExecutor")
//public class TestpointPositionExecutor {
//
//    private final ITestpointService testPointService;
//
//    private final IHierarchyService hierarchyService;
//
//    public ExecuteResult jobExecute(JobArgs jobArgs) {
//
//        try {
//            // 1. 获取所有设备
//            List<Equipment> equipmentList = equipmentService.getEquipmentsByType(null);
//            log.info("开始执行测点位置同步任务，共 {} 个设备", equipmentList.size());
//
//            // 2. 获取所有层级
//            List<HierarchyVo> hierarchyList = hierarchyService.queryList(new HierarchyBo());
//            log.info("获取到 {} 个层级", hierarchyList.size());
//
//            // 3. 收集所有ID（设备ID + 层级ID）
//            Set<Long> allIds = new HashSet<>();
//            equipmentList.forEach(equipment -> allIds.add(equipment.getId()));
//            hierarchyList.forEach(hierarchy -> allIds.add(hierarchy.getId()));
//            log.info("总共需要处理的ID数量: {}", allIds.size());
//
//            // 4. 批量调用SD400MP API获取文件信息
//            Map<Long, String> idToGlblmFileIdMap = new HashMap<>();
//            for (Long id : allIds) {
//                try {
//                    JSONObject fileResponse = SD400MPUtils.file(id);
//                    if (fileResponse != null) {
//                        JSONArray data = fileResponse.getJSONArray("data");
//                        if (data != null) {
//                            for (Object datum : data) {
//                                JSONObject item = (JSONObject) datum;
//                                String name = item.getStr("name");
//                                if (name != null && name.contains(".")) {
//                                    String suffix = name.split("\\.")[1];
//                                    if ("glblm".equals(suffix)) {
//                                        String fileId = item.getStr("id");
//                                        if (fileId != null) {
//                                            idToGlblmFileIdMap.put(id, fileId);
//                                            log.debug("ID[{}]找到GLBLM文件[{}]", id, fileId);
//                                           // break; // 每个ID只需要一个GLBLM文件
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    log.warn("获取ID[{}]的文件信息失败: {}", id, e.getMessage());
//                }
//            }
//
//            log.info("共找到 {} 个GLBLM文件", idToGlblmFileIdMap.size());
//
//            // 5. 循环处理每个找到的GLBLM文件
//            List<TestpointBo> allTestpointUpdates = new ArrayList<>();
//            for (Map.Entry<Long, String> entry : idToGlblmFileIdMap.entrySet()) {
//                Long sourceId = entry.getKey();
//                String fileId = entry.getValue();
//
//                try {
//                    log.info("开始处理文件[{}] (来自ID[{}])", fileId, sourceId);
//
//                    // 获取模型信息
//                    JSONObject modelInfo = SD400MPUtils.modelInfo(Long.valueOf(fileId));
//                    if (modelInfo == null) {
//                        log.warn("获取文件[{}]的模型信息失败", fileId);
//                        continue;
//                    }
//
//                    JSONObject model = modelInfo.getJSONObject("model");
//                    if (model == null) {
//                        log.warn("文件[{}]的模型信息为空", fileId);
//                        continue;
//                    }
//
//                    JSONArray sensors = model.getJSONArray("sensors");
//                    if (sensors == null || sensors.isEmpty()) {
//                        log.warn("文件[{}]没有传感器数据", fileId);
//                        continue;
//                    }
//
//                    // 处理每个传感器
//                    List<TestpointBo> testPointList = new ArrayList<>();
//                    for (Object sensor : sensors) {
//                        JSONObject item = (JSONObject) sensor;
//                        JSONObject binding = item.getJSONObject("binding");
//                        if (binding == null) {
//                            continue;
//                        }
//
//                        String testPointId = item.getStr("testPointId");
//                        if (testPointId == null) {
//                            continue;
//                        }
//
//                        JSONObject pos = binding.getJSONObject("pos");
//                        if (pos == null) {
//                            continue;
//                        }
//
//                        try {
//                            Float x = pos.getFloat("x");
//                            Float y = pos.getFloat("y");
//                            Float z = pos.getFloat("z");
//
//                            TestpointBo testPoint = new TestpointBo();
//                            testPoint.setId(Long.valueOf(testPointId));
//                            testPoint.setPositionX(x != null ? new BigDecimal(x) : null);
//                            testPoint.setPositionY(y != null ? new BigDecimal(y) : null);
//                            testPoint.setPositionZ(z != null ? new BigDecimal(z) : null);
//                            testPoint.setUpdateTime(new Date());
//                            testPoint.setPositionSource(PositionSourceEnum.SD400MP.getCode());
//                            testPointList.add(testPoint);
//
//                        } catch (Exception e) {
//                            log.warn("处理传感器[{}]位置信息失败: {}", testPointId, e.getMessage());
//                        }
//                    }
//
//                    if (!testPointList.isEmpty()) {
//                        // 立即批量更新当前文件的测点
//                        testPointService.updateBatchByBo(testPointList);
//                        allTestpointUpdates.addAll(testPointList);
//                        log.info("文件[{}]处理完成，更新了 {} 个测点位置", fileId, testPointList.size());
//                    }
//
//                } catch (Exception e) {
//                    log.error("处理文件[{}]时发生错误: {}", fileId, e.getMessage(), e);
//                }
//            }
//
//            log.info("测点位置同步任务完成，总共处理了 {} 个文件，更新了 {} 个测点位置",
//                idToGlblmFileIdMap.size(), allTestpointUpdates.size());
//
//            return ExecuteResult.success("");
//
//        } catch (Exception e) {
//            log.error("测点数据位置同步任务执行失败", e);
//            return ExecuteResult.failure("测点数据位置同步任务执行失败: " + e.getMessage());
//        }
//    }
//}
