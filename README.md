# SSMPlus

SpringMVC + Spring + Mybatis + Mybatis-plus 项目模板  
下载后用idea打开，然后 Tools -> Save Project As Template 即可保存为项目模板


### com.xiyuan.template.mybatis.MpGenerator  
首先修改 property/database.properties 数据库配置  
然后打开 com.xiyuan.template.mybatis.MpGenerator 按需修改生成器的配置  
然后运行 com.xiyuan.template.mybatis.MpGenerator 的main方法即可生成mybatis相关的文件  
文件生成之后，需要解开 spring/applicationContext.xml 中 mapperLocations 相关的注释   

### com.xiyuan.template.redis.JedisWrapper  
对 Jedis 的常用操作进行封装，可以对已配置的索引进行自动化管理  
在 redis/indexs.json 中设置索引配置
目前支持4种：
```
Hash类型的索引
  {
    "index": "tb_log:id:${id}", //Hash类型的index定义固定：表明:能提供唯一检索的字段PK（例如：主键id）:${字段PK}
    "type": "h", //值固定为 h 或 Hash
    "expire": "604800" //超时时间，字段不存在或为空字符串，则默认为-1，永不超时；支持单位：d(天),h(小时),m(分),s(秒,无单位默认为秒)；支持 * 做乘法运算，例如：3600s * 24 * 7
  }
通过hset，hmset更新redis的时候，会自动刷新expire时间，并自动异步更新数据库
通过hget，hgetall查询redis的时候，如果未找到记录，会自动从数据库加载，然后存入redis，并返回结果  

List类型的索引
  {
    "index": "tb_user:id:${user_id}:tb_log:id", // 末尾必须为 :表明:唯一检索字段 的格式，这个List存储的内容就是 表明:唯一检索字段:* 这种Hash数据中这个字段的值；index中还可以设置其他 Hash数据中包括的字段，例如这里的 ${user_id}}，可以包含多个
    "type": "l", //值固定为 l 或 List
    "expire": "60 * 60 * 24"
  }
  
Set类型的索引
和List类似
    {
      "index": "all:tb_log:id",
      "type": "s" //值固定为 s 或 Set
    }
  
SortedSet类型的索引
和List类似，仅多了一个score字段
    {
      "index": "tb_user:id:${user_id}:tb_log:id",
      "type": "z", //值固定为 z 或 SortedSet
      "expire": "60 * 60 * 24",
      "score": "NOW" //NOW表示使用当前系统时间；也可以使用对应的Hash数据中的其他字段名，但是该字段的值必须能够转化为数字，否则索引无法更新成功
    }
    
List，Set，SortedSet三种索引在更新对应的Hash索引时会自动更新
如果查询这三种索引时，返回集合为空，则会自动根据 index 去数据库查询，并将查询记录插入redis，然后重试一次，最后返回重试的结果
查询的语句的生成方式：
tb_user:id:${user_id}:tb_log:id 生成的查询语句为：
SELECT * FROM tb_log WHERE user_id = ?;

tb_user:id:${user_id}:${state}:tb_log:id 生成的查询语句为：
SELECT * FROM tb_log WHERE user_id = ? AND state = ?;

all:tb_log:id 生成的查询语句为：
SELECT * FROM tb_log;    
这种index会返回全表记录，需要谨慎使用

使用方式参考：
src/test/java/com.xiyuan.JedisWrapperTest

```


### com.xiyuan.template.params.Params  
为参数约束校验提供了统一快捷的方案  
示例： com.xiyuan.template.params.Test  

