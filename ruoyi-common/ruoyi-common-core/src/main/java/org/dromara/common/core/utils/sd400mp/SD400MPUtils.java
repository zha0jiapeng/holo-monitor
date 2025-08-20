package org.dromara.common.core.utils.sd400mp;

import cn.hutool.core.date.DatePattern;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;

import java.io.*;
import java.util.*;

@Slf4j
public class SD400MPUtils {
    private static final String URI = GlbProperties.uri;
    private static final String USERNAME = GlbProperties.username;
    private static final String PASSWORD = GlbProperties.password;

    // 本地令牌缓存
    private static volatile String tokenCache = null;
    private static volatile long tokenExpireTime = 0;
    private static final long TOKEN_CACHE_DURATION = 10 * 60 * 1000; // 10分钟，单位毫秒

    /**
     * 获取认证令牌
     * 首先从本地缓存中获取，如果不存在或已过期则通过API请求新的令牌
     *
     * @return 认证令牌字符串
     */
    public static String getToken() {
        // 检查本地缓存中的令牌是否有效
        long currentTime = System.currentTimeMillis();
        if (tokenCache != null && currentTime < tokenExpireTime) {
            return tokenCache;
        }

        // 同步块确保多线程环境下只有一个线程获取令牌
        synchronized (SD400MPUtils.class) {
            // 双重检查，防止多个线程同时请求令牌
            if (tokenCache != null && System.currentTimeMillis() < tokenExpireTime) {
                return tokenCache;
            }

            Map<String, Object> requestMap = new HashMap<>(2);
            requestMap.put("user", USERNAME);
            requestMap.put("password", PASSWORD);
            try {
                // 发送HTTP请求获取令牌
                HttpResponse response = HttpUtil.createPost(URI + "/api/auth")
                        .contentType("application/json")
                        .body(JSONUtil.toJsonStr(requestMap))
                        .execute();

                String body = response.body();
                if (body == null || body.isEmpty()) {
                    throw new RuntimeException("获取GLB令牌失败:响应内容为空");
                }

                // 解析响应获取令牌
                Map<String, Object> tokenResult = JSONUtil.toBean(body, Map.class);
                if (tokenResult == null || !tokenResult.containsKey("token")) {
                    throw new RuntimeException("获取GLB令牌失败:响应格式错误");
                }

                Object token = tokenResult.get("token");
                if (token == null) {
                    throw new RuntimeException("获取GLB令牌失败:令牌为空");
                }

                // 将令牌存入本地缓存
                tokenCache = token.toString();
                tokenExpireTime = System.currentTimeMillis() + TOKEN_CACHE_DURATION;

                // log.info("成功获取并缓存GLB令牌，有效期至: {}", new Date(tokenExpireTime));
                return tokenCache;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("获取GLB令牌失败:" + e.getMessage(), e);
            }
        }
    }

    /**
     * 根据时间范围查询索引信息
     * @param uniqueId 唯一标识
     * @param from 开始时间
     * @param to 结束时间
     * @return 包含id和times的Map,查询失败返回null
     */
    public static JSONObject index(String uniqueId, String from, String to) {
        try {
            // 构建请求参数
            Map<String, Object> requestMap = new HashMap<>(2);
            requestMap.put("token", getToken());

            Map<String, Object> dataMap = new HashMap<>(4);
            dataMap.put("uniqueId", uniqueId);
            dataMap.put("from", from);
            dataMap.put("to", to);
            requestMap.put("data", dataMap);

            // 发送请求
            String responseBody = HttpUtil.createPost(URI + "/api/index")
                    .body(JSONUtil.toJsonStr(requestMap),ContentType.JSON.toString())
                    .execute()
                    .body();

            // 解析响应
            JSONObject response = JSONUtil.parseObj(responseBody);
            if (response == null) {
                log.error("请求索引接口返回数据为空");
                return null;
            }

            Integer code = response.getInt("code");
            if (code == 200) {
//                 Map<String, Object> data = response.getObject("data", Map.class);
//                // Map<String, Object> idResult = (Map<String, Object>) data.get("id");
//
//                // Map<String, Object> result = new HashMap<>(2);
//                // result.put("id", idResult.get("id"));
//                // result.put("times", data.get("time"));
//                System.out.println(data.get("time"));
                return response;
            } else {
                log.warn("查询索引失败 - uniqueId:{}, from:{}, to:{}, response:{}",
                    uniqueId, from, to, response.toString());
                return null;
            }
        } catch (Exception e) {
            log.error("查询索引异常 - uniqueId:{}, from:{}, to:{}", uniqueId, from, to, e);
            return null;
        }
    }

    /**
     * 获取数据集
     * @param id 数据ID
     * @param time 时间
     * @return HTTP响应
     * @throws IOException IO异常
     */
    public static byte[] dataset(Object id, String time) throws IOException {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());

        Map<String, Object> dataMap = new HashMap<>(2);
        dataMap.put("id", id);
        dataMap.put("time", time);
        map.put("data", dataMap);
        HttpResponse execute = HttpUtil.createPost(URI + "/api/dataset")
                        .body(JSONUtil.toJsonStr(map),ContentType.JSON.toString())
                        .execute();
        if(execute.getStatus() == 200 && execute.header(HttpHeaders.CONTENT_TYPE).contains(ContentType.OCTET_STREAM.getValue())){
            return execute.bodyBytes();
        }
        return null;
    }

    /**
     * 上传文件到PDExpert
     * @param file 要上传的文件
     * @return HTTP响应
     */
    public static HttpResponse pdexpert(File file) {
        Map<String, Object> map = new HashMap<>(2);
       // map.put("token", getToken());
        map.put("file", file);

        return HttpUtil.createPost(URI + "/api/pdexpert")
                .form(map)
                .execute();
    }
    /**
     * 上传PDES文件
     * @param file 要上传的文件
     * @return HTTP响应
     */
    public static HttpResponse uploadPdesFile(File file) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());
        map.put("file", file);

        return HttpUtil.createPost(URI + "/api/pdexpertUpdate")
                .form(map)
                .execute();
    }

    public static JSONObject equipmentList(String equipmentId,boolean flag) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());
        Map<String, Object> dataMap = new HashMap<>(4);
        if(equipmentId != null){
            dataMap.put("id", equipmentId);
        }
        dataMap.put("needChildren", flag);
        dataMap.put("needAllData", true);
        // dataMap.put("fullscope", false);
        map.put("data",dataMap);

        String body = HttpUtil
                .createPost(URI + "/api/equipment")
                .body(JSONUtil.toJsonStr(map))
                .execute().body();
        return JSONUtil.parseObj(body);

    }


    public static JSONObject testPointList(Long equipmentId) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());
        Map<String, Object> dataMap = new HashMap<>(4);
        dataMap.put("id", equipmentId);
        dataMap.put("needChildren", false);
        dataMap.put("needAllData", true);
        dataMap.put("fullscope", false);
        map.put("data",dataMap);
        System.out.println(JSONUtil.toJsonStr(map));
        String body = HttpUtil.createPost(URI + "/api/testpoint")
                .body(JSONUtil.toJsonStr(map))
                .execute().body();
        return JSONUtil.parseObj(body);
    }

    public static JSONObject data(Long testpointId, List<String> tags) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());
        Map<String, Object> dataMap = new HashMap<>(2);
        Map<String, Object> idMap = new HashMap<>(2);
        List<Map<String, Object>> list = new ArrayList<>();
        idMap.put("id", testpointId);
        list.add(idMap);
        dataMap.put("testpoints", list);
        dataMap.put("include", tags);
        map.put("data", dataMap);
        String body = HttpUtil.createPost(URI + "/api/data")
            .body(JSONUtil.toJsonStr(map))
            .execute().body();
        return JSONUtil.parseObj(body);
    }

    public static JSONObject file(Long equipmentId) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());
        Map<String, Object> dataMap = new HashMap<>(2);
        dataMap.put("id", equipmentId);
        dataMap.put("needChildren", true);
        map.put("data", dataMap);
        String body = HttpUtil.createPost(URI + "/api/file")
            .body(JSONUtil.toJsonStr(map))
            .execute().body();
        return JSONUtil.parseObj(body);
    }

    public static JSONObject modelInfo(Long fileId) {
        String body = HttpUtil.createGet(URI + "/api/location/modelInfo?id="+fileId)
            .header(Header.AUTHORIZATION,"GlbToken "+getToken())
            .execute().body();
        return JSONUtil.parseObj(body);
    }

    /**
     * 位置比较
     * @return HTTP响应
     */
    public static JSONObject locationCompare(Map<String,Object> map) {
        String body = HttpUtil.createPost(URI + "/api/location/compare")
                .body(JSONUtil.toJsonStr(map))
                .execute().body();
        return JSONUtil.parseObj(body);
    }


    public static JSONObject single(Map<String,Object> map) {
        String body = HttpUtil.createPost(URI + "/api/single")
                .body(JSONUtil.toJsonStr(map))
                .execute().body();
        return JSONUtil.parseObj(body);
    }

    public static JSONObject testpointFind(Map<String,Object> map) {
        map.put("token", getToken());
        String body = HttpUtil.createPost(URI + "/api/testpointFind")
                .body(JSONUtil.toJsonStr(map))
                .execute().body();
        return JSONUtil.parseObj(body);
    }



}
