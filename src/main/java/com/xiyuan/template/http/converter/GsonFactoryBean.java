package com.xiyuan.template.http.converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

public class GsonFactoryBean implements FactoryBean<Gson>, InitializingBean {

    private Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .disableHtmlEscaping()
            .serializeSpecialFloatingPointValues()
            .create();

    @Nullable
    public Gson getObject() {
        return this.gson;
    }

    public Class<?> getObjectType() {
        return Gson.class;
    }

    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

}
