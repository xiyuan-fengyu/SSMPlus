package com.xiyuan.template.redis.index;

/**
 * Created by xiyuan_fengyu on 2017/12/4 14:14.
 */
public class SortedSetIndex extends RedisIndex {

    public final String score;

    protected SortedSetIndex(String index, String type, String expire, String score) {
        super(index, type, expire);
        this.score = score == null || "".equals(score) ? "NOW" : score;
        config.addProperty("score", this.score);

        if (!this.score.equals("NOW") && !this.fields.contains(this.score)) {
            this.fields.add(this.score);
            config.get("fields").getAsJsonArray().add(this.score);
        }
    }

}
