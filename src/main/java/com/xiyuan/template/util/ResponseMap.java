package com.xiyuan.template.util;

/**
 * Created by xiyuan_fengyu on 2018/4/19 10:23.
 */
public class ResponseMap {

    public boolean success;

    public String message;

    public Object data;

    public String error;

    public ResponseMap(boolean success, String message, Object data, String error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    public ResponseMap() {
        this.success = false;
        this.message = null;
        this.data = null;
        this.error = null;
    }

}
