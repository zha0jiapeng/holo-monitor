package org.dromara.job.sd400mp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.domain.bo.HierarchyDataBo;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.enums.DataTypeEnum;
import org.dromara.hm.service.IHierarchyDataService;
import org.dromara.hm.service.IHierarchyService;
import org.dromara.hm.service.IHierarchyTypePropertyService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
@JobExecutor(name = "SensorSyncExecutor")
public class SensorSyncExecutor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final IHierarchyService hierarchyService;
    private final IHierarchyTypePropertyService hierarchyTypePropertyService;
    private final IHierarchyDataService hierarchyDataService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        log.info("开始执行传感器同步任务");

        // 调用获取最底层且具有1005属性的层级列表
        List<HierarchyVo> bottomLevelHierarchies = hierarchyService.getBottomLevelWithConfiguration();
        for (HierarchyVo hierarchyVo : bottomLevelHierarchies) {
            List<HierarchyPropertyVo> properties = hierarchyVo.getProperties();
            for (HierarchyPropertyVo property : properties) {
                HierarchyTypePropertyVo hierarchyTypePropertyVo = hierarchyTypePropertyService.queryById(Long.valueOf( property.getTypePropertyId()));
                HierarchyTypePropertyDictVo dict = hierarchyTypePropertyVo.getDict();
                if(dict.getDataType().equals(DataTypeEnum.CONFIGURATION.getCode())){
                    String propertyValue = property.getPropertyValue();
                    JSONArray confJson = JSONUtil.parseArray(propertyValue);
                    List<String> allTags = new ArrayList<>();
                    Map<String,String> mapp = new HashMap<>();
                    for (Object o : confJson) {
                        JSONObject item = (JSONObject) o;
                        mapp.put(item.get("value").toString(), item.get("cn").toString());
                        String tag = item.getStr("value");
                        allTags.add(tag);
                    }
                    JSONObject entries = SD400MPUtils.testpointFind(hierarchyVo.getCode());
                    if(entries.get("code")==null || Integer.parseInt(entries.get("code").toString()) != 200){
                        continue;
                    }
                    JSONObject data = entries.getJSONObject("data");
                    String id = data.getStr("id");
                    JSONObject tagResponse = SD400MPUtils.data(Long.valueOf(id), allTags, null);
                    System.out.println(tagResponse);
                    Object online = tagResponse.getByPath("data.groups[0].online");
                    if(online==null) continue;
                    JSONArray onlines = (JSONArray) online;
                    for (Object o : onlines) {
                        JSONObject item = (JSONObject) o;
                        HierarchyDataBo hierarchyData = new HierarchyDataBo();
                        hierarchyData.setHierarchyId(Long.valueOf(hierarchyVo.getId()));
                        hierarchyData.setTime(LocalDateTime.parse(item.getStr("dt")));
                        hierarchyData.setValue(new BigDecimal(item.getStr("val")));
                        hierarchyData.setTag(item.getStr("key"));
                        hierarchyData.setName( mapp.get(item.getStr("tag")));
                        hierarchyDataService.insertByBo(hierarchyData);
                    }
//                    LocalDateTime toTime = LocalDateTime.now();
//                    String from = toTime.minusHours(1).format(FORMATTER);
//                    String to = toTime.format(FORMATTER);
//                    JSONObject index = SD400MPUtils.index(hierarchyVo.getCode(), from, to);
//                    System.out.println(index);
//                    Integer code = index.getInt("code");
//                    if (code == 200) {
//                        JSONObject data = index.getJSONObject("data");
//                        String id = data.getJSONObject("id").getStr("id");
//                        JSONArray times = data.getJSONArray("time");
//                        for (Object timeObj : times) {
//                            String timeStr = (String) timeObj;
//                            JSONObject tagResponse = SD400MPUtils.data(Long.valueOf(id), allTags,timeStr);
//                            System.out.println(tagResponse);
//                            Object online = tagResponse.getByPath("data.groups[0].online");
//                            if(online==null) continue;
//                            JSONArray onlines = (JSONArray) online;
//                            for (Object o : onlines) {
//                                JSONObject item = (JSONObject) o;
//                                HierarchyDataBo hierarchyData = new HierarchyDataBo();
//                                hierarchyData.setHierarchyId(Long.valueOf(hierarchyVo.getId()));
//                                hierarchyData.setTime(LocalDateTime.parse(item.getStr("dt")));
//                                hierarchyData.setValue(new BigDecimal(item.getStr("val")));
//                                hierarchyData.setTag(item.getStr("key"));
//                                hierarchyData.setName( mapp.get(item.getStr("tag")));
//                                hierarchyDataService.insertByBo(hierarchyData);
//                            }
//
//                        }
//                    }
                }
            }
        }
        return ExecuteResult.success();
    }
}
