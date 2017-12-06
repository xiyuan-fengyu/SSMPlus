--local tempJson = cjson.decode('{"id": 123, "name": "xiyuan"}')
--local id = tempJson.id
--local res = redis.call("get", "test")
--if (res == "") then
--    res = "null"
--end
--redis.call("hmset", "", "")
--return res

--local tempArgs = {"hmset", "test", "id", "123", "name", "xiyuan"}
--local temp = redis.call(unpack(tempArgs))
--return temp


--local data = cjson.decode('{"NOW":1512453448514,"method":"hset","key":"tb_log:id:0","field":"user_id","value":"13","indexes":[{"index":"tb_log:id:${id}","fields":["id"],"type":"h","expire":604800000},{"index":"tb_user:id:${user_id}:tb_log:id","fields":["user_id"],"type":"z","expire":86400000,"score":"create_time"}]}')
--local data = cjson.decode('{"NOW":1512466152564,"method":"hmset","key":"tb_log:id:1","map":{"create_time":"2017-12-05 17:26:00","user_id":"18","id":"1","content":"hmset test; db test; 1512466152549"},"indexes":[{"index":"tb_log:id:${id}","fields":["id"],"type":"h","expire":604800000},{"index":"tb_user:id:${user_id}:tb_log:id","fields":["user_id"],"type":"z","expire":86400000,"score":"NOW"}]}')
--local data = cjson.decode('{"NOW":1512526749784,"method":"hmset","key":"tb_log:id:0","map":{"create_time":"2017-12-05 17:26:00","user_id":"10","id":"0","content":"hmset test; db test; 1512526749773"},"indexes":[{"index":"tb_log:id:${id}","fields":["id"],"type":"h","expire":604800000},{"index":"tb_user:id:${user_id}:tb_log:id","fields":["user_id"],"type":"z","expire":86400000,"score":"NOW"},{"index":"all:tb_log:id","fields":[],"type":"z","expire":-1,"score":"create_time"}]}')



--local temp = "2017-12-06 10:34:45"
--local num = ""
--for d in string.gfind(temp, "%d") do num = num .. d end
--return temp

--local temp = 123
--temp = tonumber(temp)
--return temp

--local function toNum(obj)
--    local num
--        if type(obj) == "string" then
--            num = ""
--            for d in string.gfind(obj, "%d") do num = num .. d end
--        else
--            num = obj
--        end
--        num = tonumber(num)
--    return num
--end
--
--local temp = toNum("2017-12-06 10:34:45")
--return temp