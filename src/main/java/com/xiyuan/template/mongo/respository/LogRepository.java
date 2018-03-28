package com.xiyuan.template.mongo.respository;

import com.xiyuan.template.mongo.model.Log;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;

/**
 * Created by xiyuan_fengyu on 2017/12/8 13:58.
 */
public interface LogRepository extends MongoRepository<Log, Long>{

    List<Log> findLogsByUserIdEquals(long userId);

    List<Log> findLogsByCreateTimeBetween(Date start, Date end);

    @Query(value = "{'createTime': {'$gte': ?0, '$lte': ?1}}")
    List<Log> queryByCreateTimeBetween(Date start, Date end);

}
