package com.xiyuan.template.redis.index;

/**
 * Created by xiyuan_fengyu on 2017/12/4 14:14.
 */
public class ListIndex extends RedisIndex {
    protected ListIndex(String index, String type, String expire) {
        super(index, type, expire);
    }
}
