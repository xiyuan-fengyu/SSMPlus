package com.xiyuan.template.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by xiyuan_fengyu on 2018/4/19 10:07.
 */
public class ResponseUtil {

    private static Map<String, Object> create(boolean success, String message, Object data, String error) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", success);
        res.put("message", message);
        if (data != null) res.put("data", data);
        if (error != null) res.put("error", error);
        return res;
    }

    public static Map<String, Object> success(String message, Object data) {
        return create(true, message, data, null);
    }

    public static Map<String, Object> success(String message) {
        return create(true, message, null, null);
    }

    public static Map<String, Object> success() {
        return create(true, "ok", null, null);
    }

    public static Map<String, Object> fail(String message, String error) {
        return create(false, message, null, error);
    }

    public static Map<String, Object> fail(String message) {
        return create(false, message, null, null);
    }

}
