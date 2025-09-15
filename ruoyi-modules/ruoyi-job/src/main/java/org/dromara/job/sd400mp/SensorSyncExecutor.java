package org.dromara.job.sd400mp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.sd400mp.SD400MPUtils;
import org.dromara.hm.domain.HierarchyData;
import org.dromara.hm.domain.HierarchyProperty;
import org.dromara.hm.domain.HierarchyTypeProperty;
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import org.dromara.hm.domain.bo.HierarchyDataBo;
import org.dromara.hm.domain.bo.HierarchyPropertyBo;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;
import org.dromara.hm.domain.vo.HierarchyVo;
import org.dromara.hm.enums.DataTypeEnum;
import org.dromara.hm.service.*;
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
    private final IHierarchyTypePropertyDictService hierarchyTypePropertyDictService;
    private final IHierarchyDataService hierarchyDataService;
    private final IHierarchyPropertyService hierarchyPropertyService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        log.info("开始执行传感器同步任务");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 调用获取最底层且具有1005属性的层级列表
        List<HierarchyVo> bottomLevelHierarchies = hierarchyService.getBottomLevelWithConfiguration();
        for (HierarchyVo hierarchyVo : bottomLevelHierarchies) {
            List<HierarchyPropertyVo> properties = hierarchyVo.getProperties();
            for (HierarchyPropertyVo property : properties) {
                HierarchyTypePropertyVo hierarchyTypePropertyVo = hierarchyTypePropertyService.queryById(property.getTypePropertyId());
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
                    Object online = tagResponse.getByPath("data.groups[0].online");
                    if(online==null) continue;
                    JSONArray onlines = (JSONArray) online;
                    for (Object o : onlines) {
                        JSONObject item = (JSONObject) o;
                        String key = item.getStr("key");
                        HierarchyTypePropertyDict dictt = hierarchyTypePropertyDictService.getOne(new LambdaQueryWrapper<HierarchyTypePropertyDict>().eq(HierarchyTypePropertyDict::getDictKey, key));
                        if(dictt!=null){
                            HierarchyTypeProperty reportStt = hierarchyTypePropertyService.getOne(
                                new LambdaQueryWrapper<HierarchyTypeProperty>()
                                    .eq(HierarchyTypeProperty::getTypeId, hierarchyVo.getTypeId())
                                    .eq(HierarchyTypeProperty::getPropertyDictId,dictt.getId()));
                            if(reportStt ==null) continue;
                            HierarchyProperty hp = hierarchyPropertyService.getOne(
                                new LambdaQueryWrapper<HierarchyProperty>()
                                    .eq(HierarchyProperty::getTypePropertyId, reportStt.getId()).eq(HierarchyProperty::getHierarchyId, hierarchyVo.getId()));
                            if(hp ==null)
                                hp = new  HierarchyProperty();
                            hp.setHierarchyId(hierarchyVo.getId());
                            hp.setTypePropertyId(reportStt.getId());
                            hp.setScope(0);
                            hp.setPropertyValue(item.get("val").toString());
                            hierarchyPropertyService.saveOrUpdate(hp);
                        }
                        if(key.equals("sys:st")){
                            LocalDateTime parse = LocalDateTime.parse(item.getStr("dt"));
                            HierarchyTypePropertyDict reportTime = hierarchyTypePropertyDictService.getOne(new LambdaQueryWrapper<HierarchyTypePropertyDict>().eq(HierarchyTypePropertyDict::getDictKey, "report_time"));
                            HierarchyTypeProperty reportTimee = hierarchyTypePropertyService.getOne(new LambdaQueryWrapper<HierarchyTypeProperty>().eq(HierarchyTypeProperty::getTypeId, hierarchyVo.getTypeId()).eq(HierarchyTypeProperty::getPropertyDictId,reportTime.getId()));
                            HierarchyProperty hpp = hierarchyPropertyService.getOne(
                                new LambdaQueryWrapper<HierarchyProperty>()
                                    .eq(HierarchyProperty::getTypePropertyId, reportTimee.getId()).eq(HierarchyProperty::getHierarchyId, hierarchyVo.getId()));
                            if(hpp ==null) hpp = new  HierarchyProperty();
                            hpp.setHierarchyId(hierarchyVo.getId());
                            hpp.setTypePropertyId(reportTimee.getId());
                            hpp.setScope(0);
                            hpp.setPropertyValue(parse.format(dateTimeFormatter));
                            hierarchyPropertyService.saveOrUpdate(hpp);
                        }
                    }
                }
            }
        }
        return ExecuteResult.success();
    }

    private void extracted(HierarchyVo hierarchyVo,String dictKey,String value) {
        HierarchyTypePropertyDict reportTimeDict = hierarchyTypePropertyDictService.getOne(new LambdaQueryWrapper<HierarchyTypePropertyDict>().eq(HierarchyTypePropertyDict::getDictKey, dictKey));
        HierarchyTypeProperty one = hierarchyTypePropertyService.getOne(
            new LambdaQueryWrapper<HierarchyTypeProperty>()
                .eq(HierarchyTypeProperty::getPropertyDictId, reportTimeDict.getId())
                .eq(HierarchyTypeProperty::getTypeId, hierarchyVo.getTypeId())
        );
        HierarchyPropertyBo hierarchyProperty = new HierarchyPropertyBo();
        hierarchyProperty.setPropertyValue(value);
        hierarchyProperty.setScope(0);
        hierarchyProperty.setHierarchyId(hierarchyVo.getId());
        hierarchyProperty.setTypePropertyId(one.getId());
        hierarchyPropertyService.insertByBo(hierarchyProperty);
    }
}
