local data = cjson.decode(ARGV[1])

local function toNum(obj)
    local num
    if type(obj) == "string" then
        num = ""
        for d in string.gfind(obj, "%d") do num = num .. d end
    else
        num = obj
    end
    num = tonumber(num)
    return num
end

local function strEqual(item1, item2)
    if item1 == nil then
        return item2 == nil
    elseif item2 == nil then
        return false
    end
    return ("" .. item1) == ("" .. item2)
end

local key = data.key
local expire
local result

local keyId = string.match(key, ".+:(.+)")

local fields = {}

if data.method == "hset" then
    local field = data.field
    local value = data.value

    -- 监测indexes的fields中是否包含 field，如果包含，则需要监测 value 是否有变更，然后决定是否需要更新索引
    local changeIndexes = {}
    for i, item in ipairs(data.indexes) do
        if i == 1 then
            if item.expire ~= nil then
                expire = item.expire
            end
        else
            table.insert(changeIndexes, item)
            for j, f in ipairs(item.fields) do
                fields[f] = j
            end
        end
    end

    if #changeIndexes > 0 then
        -- 有除了keyIndex以外的索引，需要检测字段是否更新
        local hmgetCall = { "hmget", key }
        for f, i in pairs(fields) do
            table.insert(hmgetCall, f)
        end

        local oldValues = redis.call(unpack(hmgetCall))
        local oldFieldValues = {}
        local fieldI = 1
        for f, i in pairs(fields) do
            if oldValues[fieldI] ~= false then
                oldFieldValues[f] = oldValues[fieldI]
            end
            fieldI = fieldI + 1
        end

        -- field 的值是否发生变化，如果发生变化，则需要更新相应的索引
        local oldValue = oldFieldValues[field]
        if value ~= oldValue then
            for i, index in ipairs(changeIndexes) do
                -- 如果 oldValue 不为 nil 或 false， 则需要从旧的索引中删除 keyId
                if oldValue ~= nil then
                    local oldIndexKey = index.index
                    local remRequired = true

                    oldIndexKey = string.gsub(oldIndexKey, "${(.-)=(.-)}", function (tempField, tempValue)
                        if field == tempField and strEqual(oldValue, tempValue) == false then
                            remRequired = false
                        end
                        return tempField .. "=" .. tempValue
                    end)

                    if remRequired then
                        for j, f in ipairs(index.fields) do
                            oldIndexKey = string.gsub(oldIndexKey, "${" .. f .. "}", oldFieldValues[f])
                        end

                        local remCall
                        if index.type == "l" then
                            remCall = {"lrem", oldIndexKey, "0", keyId}
                        elseif index.type == "s" or index.type == "z" then
                            remCall = { index.type .. "rem", oldIndexKey, keyId}
                        end

                        if remCall ~= nil then
                            redis.call(unpack(remCall))
                        end
                    end
                end

                -- 如果索引中包含 ${field=value} 这种，则仅符合这种条件时才添加新的索引
                local newIndexKey = index.index
                local requireAdd = true
                newIndexKey = string.gsub(newIndexKey, "${(.-)=(.-)}", function (tempField, tempValue)
                    if field == tempField and strEqual(value, tempValue) == false then
                        requireAdd = false
                    end
                    return tempField .. "=" .. tempValue
                end)

                if requireAdd then
                    oldFieldValues[field] = value

                    -- 添加新的索引
                    for j, f in ipairs(index.fields) do
                        newIndexKey = string.gsub(newIndexKey, "${" .. f .. "}", oldFieldValues[f])
                    end

                    local addIndexCall
                    if index.type == "l" then
                        addIndexCall = {"lpush", newIndexKey, keyId}
                    elseif index.type == "s" then
                        addIndexCall = {"sadd", newIndexKey, keyId}
                    elseif index.type == "z" then
                        local score
                        if index.score == "NOW" then
                            score = data.NOW
                        else
                            score = oldFieldValues[index.score]
                        end

                        score = toNum(score)
                        if score ~= nil then
                            addIndexCall = {"zadd", newIndexKey, score, keyId}
                        end
                    end

                    if addIndexCall ~= nil then
                        redis.call(unpack(addIndexCall))
                        if index.expire ~= nil and index.expire >= 0 then
                            redis.call("expire", newIndexKey, index.expire)
                        end
                    end
                end

            end
        end
    end

    result = redis.call("hset", key, field, value)

elseif data.method == "hmset" then
    local map = data.map

    -- 搜集所有需要获取旧值的字段
    local changeIndexes = {}
    local fieldsCount = 1
    for i, item in ipairs(data.indexes) do
        if i == 1 then
            if item.expire ~= nil then
                expire = item.expire
            end
        else
            table.insert(changeIndexes, item)
            for j, f in ipairs(item.fields) do
                fields[f] = j
                fieldsCount = fieldsCount + 1
            end
        end
    end

    if #changeIndexes > 0 and fieldsCount > 0 then
        -- 获取旧值，并检测字段的值是否更新
        local hmgetCall = { "hmget", key }
        for f, i in pairs(fields) do
            table.insert(hmgetCall, f)
        end

        local oldValues = redis.call(unpack(hmgetCall))
        local oldFieldValues = {}
        local fieldI = 1
        for f, i in pairs(fields) do
            if oldValues[fieldI] ~= false then
                oldFieldValues[f] = oldValues[fieldI]
            end
            fieldI = fieldI + 1
        end

        -- 对 changeIndexes 每一个索引做校验，如果index中有值变化，则需要从旧索引中删除id
        for i, index in ipairs(changeIndexes) do
            local hasFields = false
            local changed = false
            local existedOldNil = false
            for j, f in ipairs(index.fields) do
                hasFields = true
                local oldV = oldFieldValues[f]
                local newV = map[f]
                if strEqual(oldV, newV) == false then
                    changed = true
                end
                if oldV == nil then
                    existedOldNil = true
                end
            end

            if hasFields == false then
                changed = true
            end

            if existedOldNil == false and changed and hasFields then
                -- 旧值中没有空值，且值有变化，则需要删除旧的索引
                local oldIndexKey = index.index
                local remRequired = true

                oldIndexKey = string.gsub(oldIndexKey, "${(.-)=(.-)}", function (tempField, tempValue)
                    if strEqual(oldFieldValues[tempField], tempValue) == false then
                        remRequired = false
                    end
                    return tempField .. "=" .. tempValue
                end)

                if remRequired then
                    for j, f in ipairs(index.fields) do
                        oldIndexKey = string.gsub(oldIndexKey, "${" .. f .. "}", "" .. oldFieldValues[f])
                    end

                    local remCall
                    if index.type == "l" then
                        remCall = {"lrem", oldIndexKey, "0", keyId}
                    elseif index.type == "s" or index.type == "z" then
                        remCall = { index.type .. "rem", oldIndexKey, keyId}
                    end

                    if remCall ~= nil then
                        redis.call(unpack(remCall))
                    end
                end

            end

            if changed or data.forceAddIndex then
                -- 设置新的索引
                local newIndexKey = index.index
                local requireAdd = true

                for f, v in pairs(map) do
                    if v == nil then
                        map[f] = oldFieldValues[f]
                    end
                end

                newIndexKey = string.gsub(newIndexKey, "${(.-)=(.-)}", function (tempField, tempValue)
                    if strEqual(map[tempField], tempValue) == false then
                        requireAdd = false
                    end
                    return tempField .. "=" .. tempValue
                end)

                if requireAdd then
                    for j, f in ipairs(index.fields) do
                        newIndexKey = string.gsub(newIndexKey, "${" .. f .. "}", map[f])
                    end

                    local addIndexCall
                    if index.type == "l" then
                        addIndexCall = {"lpush", newIndexKey, keyId}
                    elseif index.type == "s" then
                        addIndexCall = {"sadd", newIndexKey, keyId}
                    elseif index.type == "z" then
                        local score
                        if index.score == "NOW" then
                            score = data.NOW
                        else
                            score = map[index.score]
                        end

                        score = toNum(score)
                        if score ~= nil then
                            addIndexCall = {"zadd", newIndexKey, score, keyId}
                        end
                    end

                    if addIndexCall ~= nil then
                        redis.call(unpack(addIndexCall))
                        if index.expire ~= nil and index.expire >= 0 then
                            redis.call("expire", newIndexKey, index.expire)
                        end
                    end
                end

            end
        end
    end

    local hmsetCall = {"hmset", key }
    for k, v in pairs(map) do
        table.insert(hmsetCall, k)
        table.insert(hmsetCall, v)
    end
    result = redis.call(unpack(hmsetCall))

end

if expire ~= nil and expire > -1 then
    -- 设置 key 的超时时间
    redis.call("expire", key, expire)
end

return result
