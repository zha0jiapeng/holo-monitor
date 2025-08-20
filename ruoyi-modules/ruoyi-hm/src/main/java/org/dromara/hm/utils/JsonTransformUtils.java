package org.dromara.hm.utils;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hm.dto.IndexFileDto;
import org.dromara.hm.dto.JsonNodeDto;
import org.dromara.hm.dto.TransformedIndexDataDto;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class JsonTransformUtils {

    public static TransformedIndexDataDto transformIndexJson(IndexFileDto indexFile) {
        TransformedIndexDataDto transformedData = new TransformedIndexDataDto();
        if (indexFile == null || indexFile.getChildren() == null) {
            return transformedData;
        }

        // Find 'mont' node
        Optional<JsonNodeDto> montNodeOpt = indexFile.getChildren().stream()
                .filter(node -> "mont".equals(node.getId()))
                .findFirst();
        if (!montNodeOpt.isPresent()) {
            return transformedData;
        }
        JsonNodeDto montNode = montNodeOpt.get();

        // Get devid from mont's children
        montNode.getChildren().stream()
                .filter(node -> "devid".equals(node.getId()))
                .findFirst()
                .ifPresent(devIdNode -> transformedData.setDevid(devIdNode.getVal()));

        // Find 'time' node from root children
        indexFile.getChildren().stream()
                .filter(node -> "time".equals(node.getId()))
                .findFirst()
                .ifPresent(timeNode -> {
                    String timeStr = timeNode.getVal();
                    if (timeStr != null) {
                        try {
                            // 解析带时区的时间字符串
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'");
                            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                            Date date = sdf.parse(timeStr);

                            // 转换为北京时间
                            SimpleDateFormat bjSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            bjSdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
                            transformedData.setTime(bjSdf.format(date));
                        } catch (ParseException e) {
                            log.error("Error parsing time: " + timeStr, e);
                        }
                    }
                });

        // Find 'pd' node from mont's children
        montNode.getChildren().stream()
                .filter(node -> "pd".equals(node.getId()))
                .findFirst()
                .ifPresent(pdNode -> {
                    if (pdNode.getChildren() != null) {
                        // map for faster lookup
                        Map<String, String> pdData = pdNode.getChildren().stream()
                                .filter(node -> node.getVal() != null)
                                .collect(Collectors.toMap(JsonNodeDto::getId, JsonNodeDto::getVal, (v1, v2) -> v1));

                        transformedData.setF(new BigDecimal(pdData.get("f").toString()));
                        transformedData.setMag(new BigDecimal(pdData.get("mag").toString()));
                        transformedData.setCnt(new BigDecimal(pdData.get("cnt").toString()));
                        transformedData.setSt(Integer.parseInt(pdData.get("st").toString()));
                        transformedData.setMagAv(new BigDecimal(pdData.get("magAv").toString()));

                        // Handle 'pdtype' which is a nested structure
                        pdNode.getChildren().stream()
                                .filter(node -> "pdtype".equals(node.getId()))
                                .findFirst()
                                .ifPresent(pdTypeNode -> {
                                    if (pdTypeNode.getChildren() != null) {
                                        Map<String, String> pdTypeData = pdTypeNode.getChildren().stream()
                                                .filter(node -> node.getVal() != null)
                                                .collect(Collectors.toMap(JsonNodeDto::getId, JsonNodeDto::getVal));

                                        transformedData.setPdTypeJson(JSONUtil.toJsonStr(pdTypeData));
                                        // 创建类型与数值的映射
                                        Map<String, BigDecimal> typeValueMap = new LinkedHashMap<>();
                                        typeValueMap.put("external", new BigDecimal(pdTypeData.getOrDefault("external", "0")));
                                        typeValueMap.put("unknown", new BigDecimal(pdTypeData.getOrDefault("unknown", "0")));
                                        typeValueMap.put("floating", new BigDecimal(pdTypeData.getOrDefault("floating", "0")));
                                        typeValueMap.put("corona", new BigDecimal(pdTypeData.getOrDefault("corona", "0")));
                                        typeValueMap.put("insulation", new BigDecimal(pdTypeData.getOrDefault("insulation", "0")));
                                        typeValueMap.put("particle", new BigDecimal(pdTypeData.getOrDefault("particle", "0")));

                                        // 设置各个类型的数值
                                        transformedData.setExternal(typeValueMap.get("external"));
                                        transformedData.setUnknown(typeValueMap.get("unknown"));
                                        transformedData.setFloating(typeValueMap.get("floating"));
                                        transformedData.setCorona(typeValueMap.get("corona"));
                                        transformedData.setInsulation(typeValueMap.get("insulation"));
                                        transformedData.setParticle(typeValueMap.get("particle"));

                                        // 找出占比最大的类型
                                        Map.Entry<String, BigDecimal> maxEntry = typeValueMap.entrySet().stream()
                                                .max(Map.Entry.comparingByValue())
                                                .orElse(new AbstractMap.SimpleEntry<>("", BigDecimal.ZERO));

                                        // 赋值给pdType
                                        transformedData.setPdType(maxEntry.getKey());
                                        transformedData.setPdTypeValue(maxEntry.getValue());
                                    }
                                });
                    }
                });

        return transformedData;
    }
}
