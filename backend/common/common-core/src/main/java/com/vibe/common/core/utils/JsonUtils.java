package com.vibe.common.core.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;

import java.util.List;

/**
 * JSON工具类
 */
public class JsonUtils {

    /**
     * 对象转JSON字符串
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj);
    }

    /**
     * JSON字符串转对象
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }

    /**
     * JSON字符串转List
     */
    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseArray(json, clazz);
    }

    /**
     * JSON字符串转泛型对象
     */
    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, typeReference);
    }

    /**
     * 对象转JSONObject
     */
    public static JSONObject toJSONObject(Object obj) {
        if (obj == null) {
            return null;
        }
        return (JSONObject) JSON.toJSON(obj);
    }

    private JsonUtils() {
    }
}
