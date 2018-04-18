package com.xiyuan.template.redis.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/12/5 16:17.
 */
public interface DynamicDao extends BaseMapper<DynamicEntity> {

    List<Object> existedCheck(@Param("table") String table, @Param("pk") String pk, @Param("pkVal") String value);

    LinkedHashMap<String, String> selectByPk(@Param("table") String table, @Param("pk") String pk, @Param("pkVal") String value);

    int selectCountByColVals(@Param("table") String table, @Param("colVals") List<ColVal> colVals);

    List<LinkedHashMap<String, String>> selectByColVals(@Param("table") String table, @Param("colVals") List<ColVal> colVals);

    int insert(@Param("table") String table, @Param("pk") String pk, @Param("pkVal") String value, @Param("colVals") List<ColVal> colVals);

    int update(@Param("table") String table, @Param("pk") String pk, @Param("pkVal") String value, @Param("colVals") List<ColVal> colVals);

}
