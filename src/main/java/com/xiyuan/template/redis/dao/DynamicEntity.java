package com.xiyuan.template.redis.dao;

import com.baomidou.mybatisplus.activerecord.Model;

import java.io.Serializable;

/**
 * Created by xiyuan_fengyu on 2017/12/5 16:33.
 */
public class DynamicEntity extends Model<DynamicEntity> {

    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    protected Serializable pkVal() {
        return id;
    }
}
