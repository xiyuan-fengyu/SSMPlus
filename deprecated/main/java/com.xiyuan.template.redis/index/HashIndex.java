package com.xiyuan.template.redis.index;

/**
 * Created by xiyuan_fengyu on 2017/12/4 14:13.
 */
public class HashIndex extends RedisIndex {
    protected HashIndex(String index, String type, String expire) {
        super(index, type, expire);
    }
}
