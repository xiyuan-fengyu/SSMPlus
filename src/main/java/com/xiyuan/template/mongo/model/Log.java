package com.xiyuan.template.mongo.model;

import com.xiyuan.template.util.Util;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by xiyuan_fengyu on 2017/12/8 11:36.
 */
public class Log implements Serializable {

    private static final long serialVersionUID = -3127825465186000663L;

    @Id
    public long id;

    public long userId;

    public String content;

    public Date createTime;

    public int satus;

    public Log() {
    }

    public Log(long id, long userId, String content, Date createTime, int satus) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.createTime = createTime;
        this.satus = satus;
    }

    @Override
    public String toString() {
        return Util.gsonPretty.toJson(this);
    }
}
