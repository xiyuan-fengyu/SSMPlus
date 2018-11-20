package com.xiyuan.template.mybatis.entity;

import com.baomidou.mybatisplus.enums.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author xiyuan
 * @since 2018-11-20
 */
@TableName("tb_log")
public class Log extends Model<Log> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String content;
    private Date time;
    private Integer version;


    public Integer getId() {
        return id;
    }

    public Log setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getContent() {
        return content;
    }

    public Log setContent(String content) {
        this.content = content;
        return this;
    }

    public Date getTime() {
        return time;
    }

    public Log setTime(Date time) {
        this.time = time;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public Log setVersion(Integer version) {
        this.version = version;
        return this;
    }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "Log{" +
        ", id=" + id +
        ", content=" + content +
        ", time=" + time +
        ", version=" + version +
        "}";
    }
}
