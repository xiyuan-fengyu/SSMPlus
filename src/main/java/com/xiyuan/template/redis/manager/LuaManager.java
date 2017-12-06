package com.xiyuan.template.redis.manager;

import com.xiyuan.template.util.Util;

import java.nio.charset.StandardCharsets;

/**
 * Created by xiyuan_fengyu on 2017/12/4 18:47.
 */
public class LuaManager {

    public final String hUpdate = Util.readFromResource("redis/lua/hUpdate.lua", StandardCharsets.UTF_8);

}
