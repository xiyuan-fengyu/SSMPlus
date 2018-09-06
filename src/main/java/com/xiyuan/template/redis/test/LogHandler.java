package com.xiyuan.template.redis.test;

import com.xiyuan.template.mybatis.dao.LogDao;
import com.xiyuan.template.mybatis.entity.Log;
import com.xiyuan.template.redis.annotation.AfterJedisCall;
import com.xiyuan.template.util.ObjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by xiyuan_fengyu on 2018/9/6 15:42.
 */
@Component
public class LogHandler {

    @Autowired
    private LogDao logDao;

    @AfterJedisCall(method = "hgetAll", argIndexsToMatchAsKey = 0, keyPatterns = "tb_log:id:(\\d+)")
    public Object getLog(Jedis jedis, Method method, Object[] args, Object resFromJedis, String[][] matchs) {
        if (resFromJedis instanceof Map && ((Map) resFromJedis).size() > 0) {
            return resFromJedis;
        }
        int id = Integer.parseInt(matchs[0][1]);
        Log log = logDao.selectById(id);
        Map<String, String> logMap = ObjectUtil.entityToMap(log);
        jedis.hmset(matchs[0][0], logMap);
        return logMap;
    }

}
