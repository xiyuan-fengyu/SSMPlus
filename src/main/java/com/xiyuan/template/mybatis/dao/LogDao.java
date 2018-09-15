package com.xiyuan.template.mybatis.dao;

import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.xiyuan.template.mybatis.entity.Log;
import com.xiyuan.template.mybatis.mapper.LogMapper;

import java.util.List;

public interface LogDao extends LogMapper {

    List<Log> selectPage(Pagination page);

}