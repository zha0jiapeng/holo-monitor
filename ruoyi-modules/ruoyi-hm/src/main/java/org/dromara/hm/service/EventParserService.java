package org.dromara.hm.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.DataPointBean;
import org.dromara.common.core.utils.sd400mp.MPBinaryConverter;
import org.dromara.common.core.utils.sd400mp.MPIDMultipleJson;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.domain.sd400mp.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Events解析服务，完全对应JavaScript中getEventListAsync方法的功能
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Slf4j
@Service
public class EventParserService {

    /**
     * 解析events响应，返回与JavaScript相同结构的MPEventList
     *
     * @param eventsResponse SD400MPUtils.events方法返回的JSON响应
     * @return 解析后的事件列表对象
     */
    public MPEventList parseEvents(JSONObject eventsResponse) {
        if (eventsResponse == null || eventsResponse.getInt("code") != 200) {
            log.warn("事件响应为空或状态码不是200");
            return null;
        }

        MPEventList result = new MPEventList();

        try {
            // 1. 获取并解析PDE类信息
            parsePdeClasses(result);

            // 2. 从响应中获取设备数据
            JSONObject data = eventsResponse.getJSONObject("data");
            if (data == null) {
                log.warn("事件响应中没有data字段");
                return result;
            }

            JSONArray equipmentArray = data.getJSONArray("equipment");
            if (equipmentArray == null || equipmentArray.isEmpty()) {
                log.warn("事件响应中没有equipment数据");
                return result;
            }

            // 3. 收集所有设备和测点ID
            List<Long> eqList = new ArrayList<>();
            List<Long> tpList = new ArrayList<>();
            
            for (Object equipmentObj : equipmentArray) {
                JSONObject eq = (JSONObject) equipmentObj;
                Long eqId = eq.getLong("id");
                if (eqId != null) {
                    eqList.add(eqId);
                }

                JSONArray testpoints = eq.getJSONArray("testpoints");
                if (testpoints != null) {
                    for (Object testpointObj : testpoints) {
                        JSONObject tp = (JSONObject) testpointObj;
                        Long tpId = tp.getLong("id");
                        if (tpId != null) {
                            tpList.add(tpId);
                        }
                    }
                }
            }

            // 4. 获取设备和测点名称
            if (!eqList.isEmpty() && !tpList.isEmpty()) {
                fetchNamesAsync(result, eqList, tpList);
            }

            // 5. 获取标签映射（从服务器获取真实标签，与JavaScript端保持一致）
            Map<String, MPTag> tagsMap = getRealTagsMapping();

            // 6. 处理每个设备的事件数据
            for (Object equipmentObj : equipmentArray) {
                JSONObject eq = (JSONObject) equipmentObj;
                Long equipmentId = eq.getLong("id");
                JSONArray testpoints = eq.getJSONArray("testpoints");

                if (testpoints != null) {
                    for (Object testpointObj : testpoints) {
                        JSONObject tp = (JSONObject) testpointObj;
                        Long testpointId = tp.getLong("id");
                        
                        processTestpointTags(tp, equipmentId, testpointId, result, tagsMap);
                    }
                }
            }

            log.info("成功解析events，共{}个分组: {}", result.getGroups().size(), result.getGroups().keySet());
            return result;

        } catch (Exception e) {
            log.error("解析events异常", e);
            return null;
        }
    }

    /**
     * 获取并解析PDE类信息
     */
    private void parsePdeClasses(MPEventList result) {
        try {
            // 构建请求参数
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("token", SD400MPUtils.getToken());
            requestMap.put("data", Map.of("culture", "zh-CN")); // 默认文化

            JSONObject classesResponse = SD400MPUtils.postJsonAndCheck("/api/pdeClasses", requestMap);
            if (classesResponse != null && classesResponse.getInt("code") == 200) {
                JSONArray classesData = classesResponse.getJSONArray("data");
                if (classesData != null) {
                    for (Object classObj : classesData) {
                        JSONObject classJson = (JSONObject) classObj;
                        MPPdeClassInfo pdeClass = new MPPdeClassInfo(classJson);
                        result.getPdClasses().put(pdeClass.getIndex(), pdeClass);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取PDE类信息失败", e);
        }
    }

    /**
     * 获取设备和测点名称
     */
    private void fetchNamesAsync(MPEventList result, List<Long> eqList, List<Long> tpList) {
        try {
            // 并行获取设备名称和测点名称
            Map<String, Object> tpRequest = Map.of(
                "token", SD400MPUtils.getToken(),
                "data", MPIDMultipleJson.create(tpList)
            );
            
            Map<String, Object> eqRequest = Map.of(
                "token", SD400MPUtils.getToken(),
                "data", MPIDMultipleJson.create(eqList)
            );

            // 这里应该并行调用，但为了简化，我们串行调用
            JSONObject namesTpResponse = SD400MPUtils.postJsonAndCheck("/api/namestp", tpRequest);
            JSONObject namesEqResponse = SD400MPUtils.postJsonAndCheck("/api/nameseq", eqRequest);

            // 处理设备名称响应
            if (namesEqResponse != null && namesEqResponse.getInt("code") == 200) {
                JSONArray eqNames = namesEqResponse.getJSONArray("data");
                if (eqNames != null) {
                    for (Object nameObj : eqNames) {
                        JSONObject nameJson = (JSONObject) nameObj;
                        Long id = nameJson.getLong("id");
                        String name = nameJson.getStr("name");
                        if (id != null && name != null) {
                            result.getNamesEq().put(id, name);
                        }
                    }
                }
            }

            // 处理测点名称响应
            if (namesTpResponse != null && namesTpResponse.getInt("code") == 200) {
                JSONArray tpNames = namesTpResponse.getJSONArray("data");
                if (tpNames != null) {
                    for (Object nameObj : tpNames) {
                        JSONObject nameJson = (JSONObject) nameObj;
                        Long id = nameJson.getLong("id");
                        String name = nameJson.getStr("name");
                        if (id != null && name != null) {
                            result.getNamesTp().put(id, name);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("获取名称信息失败", e);
        }
    }

    /**
     * 处理测点的标签数据
     */
    private void processTestpointTags(JSONObject testpoint, Long equipmentId, Long testpointId, 
                                    MPEventList result, Map<String, MPTag> tagsMap) {
        JSONArray tags = testpoint.getJSONArray("tags");
        if (tags == null) {
            log.debug("测点{}没有tags数据", testpointId);
            return;
        }

        log.debug("处理测点{}, 共{}个标签", testpointId, tags.size());

        for (Object tagObj : tags) {
            JSONObject tagJson = (JSONObject) tagObj;
            String tagKey = tagJson.getStr("tag");
            
            if (tagKey == null) {
                continue;
            }

            log.debug("处理标签: {}", tagKey);

            MPTag tag = tagsMap.get(tagKey);
            if (tag == null) {
                log.debug("未找到标签定义: {}", tagKey);
                continue; // 如果找不到标签定义，跳过
            }

            // 先检查事件数据是否有效（与JavaScript端逻辑一致）
            JSONObject events = tagJson.getJSONObject("events");
            if (events == null) {
                log.debug("标签{}没有events数据", tagKey);
                continue;
            }

            String payload = events.getStr("payload");
            if (payload == null || payload.trim().isEmpty()) {
                log.debug("标签{}的events payload为空", tagKey);
                continue;
            }

            log.debug("标签{}有有效的events数据，payload长度: {}", tagKey, payload.length());

            // 只有在有有效事件数据时才创建事件分组（与JavaScript端逻辑一致）
            MPEventGroup group = result.getGroups().get(tag.getKey());
            if (group == null) {
                group = new MPEventGroup(tag, result);
                result.getGroups().put(tag.getKey(), group);
                log.debug("创建新的事件分组: {}", tag.getKey());
            }

            // 处理卫星数据（需要主events的payload不为空）
            Map<Long, Double> satelliteMap = new HashMap<>();
            JSONObject satellite = tagJson.getJSONObject("satelite");
            if (satellite != null && satellite.getJSONObject("events") != null && 
                satellite.getJSONObject("events").getStr("payload") != null && 
                !payload.isEmpty()) { // 注意：这里检查的是主events的payload
                processSatelliteData(satellite, group, testpointId, satelliteMap, tagsMap);
            }

            // 解析主要事件数据
            processEventPayload(payload, group, equipmentId, testpointId, satelliteMap);
            log.debug("成功处理标签{}的事件数据，当前group中事件数: {}", tagKey, group.getEvents().size());
        }
    }

    /**
     * 处理卫星数据
     */
    private void processSatelliteData(JSONObject satellite, MPEventGroup group, Long testpointId, 
                                    Map<Long, Double> satelliteMap, Map<String, MPTag> tagsMap) {
        try {
            JSONObject satelliteEvents = satellite.getJSONObject("events");
            if (satelliteEvents == null) {
                return;
            }

            String satellitePayload = satelliteEvents.getStr("payload");
            if (satellitePayload == null || satellitePayload.trim().isEmpty()) {
                return;
            }

            String satelliteTagKey = satellite.getStr("tag");
            if (satelliteTagKey != null) {
                MPTag satelliteTag = tagsMap.get(satelliteTagKey);
                if (satelliteTag != null) {
                    group.setSatelliteTag(satelliteTag);
                    
                    Integer sns = satellite.getInt("sns");
                    Integer unit = satellite.getInt("unit");
                    if (sns != null && unit != null) {
                        group.getDisplaySettings().put(testpointId, MPDisplaySettings.get(sns, unit));
                    }
                }
            }

            // 解析卫星数据点
            List<DataPointBean> satelliteDataPoints = MPBinaryConverter.dataPointsFromBase64(satellitePayload);
            for (DataPointBean point : satelliteDataPoints) {
                satelliteMap.put(point.getTime().getTime(), point.getValue());
            }

        } catch (Exception e) {
            log.error("处理卫星数据失败", e);
        }
    }

    /**
     * 处理事件payload数据
     */
    private void processEventPayload(String payload, MPEventGroup group, Long equipmentId, 
                                   Long testpointId, Map<Long, Double> satelliteMap) {
        try {
            List<DataPointBean> dataPoints = MPBinaryConverter.dataPointsFromBase64(payload);
            MPEvent currentEvent = null;

            for (DataPointBean point : dataPoints) {
                // 如果当前事件存在且结束时间为空，设置结束时间
                if (currentEvent != null && currentEvent.getEnd() == null) {
                    currentEvent.setEnd(point.getTime());
                }

                // 如果当前事件为空或状态发生变化，创建新事件
                if (currentEvent == null || !currentEvent.getState().equals(point.getValue().intValue())) {
                    currentEvent = new MPEvent(group, point.getValue().intValue(), point.getTime(), 
                                             equipmentId, testpointId);
                    group.getEvents().add(currentEvent);

                    // 设置卫星值
                    if (!satelliteMap.isEmpty()) {
                        Double satelliteValue = satelliteMap.get(point.getTime().getTime());
                        if (satelliteValue != null) {
                            currentEvent.setSatelliteValue(satelliteValue);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("处理事件payload失败", e);
        }
    }

    /**
     * 获取真实的标签映射（从服务器获取，与JavaScript端完全一致）
     * 对应JavaScript中通过/api/tagsJson获取的this.tags
     */
    private Map<String, MPTag> getRealTagsMapping() {
        Map<String, MPTag> tagsMap = new HashMap<>();
        
        try {
            log.info("开始获取真实标签映射...");
            // 构建请求参数，对应JavaScript中的this.requestCulture()
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("token", SD400MPUtils.getToken());
            requestMap.put("data", Map.of("culture", "zh-CN")); // 默认文化
            
            log.debug("请求参数: {}", requestMap);
            
            // 调用/api/tagsJson获取真实标签数据
            JSONObject tagsResponse = SD400MPUtils.postJsonAndCheck("/api/tagsJson", requestMap);
            log.debug("标签API响应: {}", tagsResponse != null ? "成功" : "失败");
            if (tagsResponse != null && tagsResponse.getInt("code") == 200) {
                JSONObject data = tagsResponse.getJSONObject("data");
                if (data != null) {
                    JSONArray items = data.getJSONArray("items");
                    if (items != null) {
                        // 对应JavaScript中的tagsResponse.data.items.forEach逻辑
                        for (Object itemObj : items) {
                            JSONObject tagJson = (JSONObject) itemObj;
                            MPTag tag = new MPTag(tagJson); // 从JSON构建MPTag
                            tagsMap.put(tag.getKey(), tag);
                            log.debug("添加标签: key={}, title={}", tag.getKey(), tag.getTitle());
                        }
                    }
                }
            } else {
                log.warn("获取标签映射失败，响应: {}, 使用备用标签映射", tagsResponse);
                Map<String, MPTag> fallbackTags = getTagsMapping();
                log.warn("使用备用标签映射，共{}个标签: {}", fallbackTags.size(), fallbackTags.keySet());
                return fallbackTags; // 如果获取失败，使用备用方法
            }
            
            log.info("成功获取真实标签映射，共{}个标签: {}", tagsMap.size(), tagsMap.keySet());
            return tagsMap;
            
        } catch (Exception e) {
            log.error("获取真实标签映射异常，使用备用标签映射", e);
            Map<String, MPTag> fallbackTags = getTagsMapping();
            log.warn("使用备用标签映射，共{}个标签: {}", fallbackTags.size(), fallbackTags.keySet());
            return fallbackTags; // 如果异常，使用备用方法
        }
    }

    /**
     * 获取标签映射（备用方法）
     * 创建基本的标签映射，对应JavaScript中this.tags的功能
     */
    private Map<String, MPTag> getTagsMapping() {
        Map<String, MPTag> tagsMap = new HashMap<>();
        
        // 创建常用的标签定义
        // 连接状态标签
        MPTag connectionStateTag = new MPTag();
        connectionStateTag.setKey(MPTag.CONNECTION_STATE);
        connectionStateTag.setTitle("连接状态");
        connectionStateTag.setUnits("");
        connectionStateTag.setState(true);
        tagsMap.put(MPTag.CONNECTION_STATE, connectionStateTag);
        
        // 测点状态标签
        MPTag testpointStateTag = new MPTag();
        testpointStateTag.setKey(MPTag.TESTPOINT_STATE);
        testpointStateTag.setTitle("测点状态");
        testpointStateTag.setUnits("");
        testpointStateTag.setState(true);
        tagsMap.put(MPTag.TESTPOINT_STATE, testpointStateTag);
        
        // PD诊断状态类别标签
        MPTag pdDiagnosisTag = new MPTag();
        pdDiagnosisTag.setKey("sys:mont/pd/dia/st/class");
        pdDiagnosisTag.setTitle("PD诊断状态分类");
        pdDiagnosisTag.setUnits("");
        pdDiagnosisTag.setState(true);
        tagsMap.put("sys:mont/pd/dia/st/class", pdDiagnosisTag);
        
        // PD诊断状态幅值标签
        MPTag pdDiagnosisMagTag = new MPTag();
        pdDiagnosisMagTag.setKey("sys:mont/pd/dia/st/mag");
        pdDiagnosisMagTag.setTitle("PD诊断状态幅值");
        pdDiagnosisMagTag.setUnits("");
        pdDiagnosisMagTag.setState(true);
        tagsMap.put("sys:mont/pd/dia/st/mag", pdDiagnosisMagTag);
        
        // PD诊断状态总和标签
        MPTag pdDiagnosisSumTag = new MPTag();
        pdDiagnosisSumTag.setKey("sys:mont/pd/dia/st/sum");
        pdDiagnosisSumTag.setTitle("PD诊断状态总和");
        pdDiagnosisSumTag.setUnits("");
        pdDiagnosisSumTag.setState(true);
        tagsMap.put("sys:mont/pd/dia/st/sum", pdDiagnosisSumTag);
        
        // PD类别枚举标签（卫星标签）
        MPTag pdClassEnumTag = new MPTag();
        pdClassEnumTag.setKey(MPTag.PD_CLASS_ENUM);
        pdClassEnumTag.setTitle("PD类别枚举");
        pdClassEnumTag.setUnits("");
        pdClassEnumTag.setState(false);
        tagsMap.put(MPTag.PD_CLASS_ENUM, pdClassEnumTag);
        
        // 振幅单位标签
        MPTag amplitudeUnitsTag = new MPTag();
        amplitudeUnitsTag.setKey(MPTag.AMPLITUDE_UNITS);
        amplitudeUnitsTag.setTitle("振幅单位");
        amplitudeUnitsTag.setUnits("mV");
        tagsMap.put(MPTag.AMPLITUDE_UNITS, amplitudeUnitsTag);
        
        // 平均振幅标签
        MPTag avgAmplitudeTag = new MPTag();
        avgAmplitudeTag.setKey(MPTag.AVERAGE_AMPLITUDE);
        avgAmplitudeTag.setTitle("平均振幅");
        avgAmplitudeTag.setUnits("mV");
        tagsMap.put(MPTag.AVERAGE_AMPLITUDE, avgAmplitudeTag);
        
        log.warn("使用备用标签映射方法，创建标签映射，共{}个标签: {}", tagsMap.size(), tagsMap.keySet());
        return tagsMap;
    }

}
