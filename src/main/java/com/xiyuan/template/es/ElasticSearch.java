package com.xiyuan.template.es;

import com.xiyuan.template.util.JsonTemplate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by xiyuan_fengyu on 2018/6/7 18:09.
 */
@SuppressWarnings("unchecked")
public class ElasticSearch {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearch.class);

    private CloseableHttpClient httpclient;

    private boolean running = true;

    private String esServer;

    private String charset;

    private boolean print;

    public boolean isRunning() {
        return running;
    }

    public ElasticSearch(String esServer, String charset, boolean print) {
        if (esServer.endsWith("/")) esServer = esServer.substring(0, esServer.length() - 1);
        this.esServer = esServer;
        this.charset = charset;
        this.print = print;
        this.httpclient = HttpClients.createDefault();
    }

    public List<Map> eval(String jsonStr, Object ...params) {
        return evalObj(JsonTemplate.parseTemplate(jsonStr, params));
    }

    public List<Map> evalResource(String resource, Object ...params) {
        return evalObj(JsonTemplate.parseResourceTemplate(resource, params));
    }

    private List<Map> evalObj(Object obj) {
        List<Map> res = new ArrayList<>();
        if (obj instanceof Map) {
            res.add(eval((Map) obj));
        }
        else if (obj instanceof List) {
            for (Object o : ((List) obj)) {
                res.add(eval((Map) o));
            }
        }
        return res;
    }

    private Map eval(Map job) {
        Optional optional = job.keySet().stream().filter(key -> key.toString().toUpperCase().matches("GET|POST|PUT|DELETE")).findFirst();
        String method = optional.isPresent() ? (String) optional.get() : null;

        StringBuffer printBuffer = print ? new StringBuffer() : null;
        if (print) printBuffer.append(method).append(" ");

        String path = (String) job.get(method);
        if (print) printBuffer.append(path).append("\n");
        if (!path.startsWith("http")) {
            if (path.startsWith("/")) path = this.esServer + path;
            else path = this.esServer + "/" + path;
        }

        Object data = job.get("data");
        StringBuffer dataStrBuilder = null;
        if (data != null) {
            dataStrBuilder = new StringBuffer();
            if (data instanceof List) {
                for (Object o : ((List) data)) {
                    String oJson = JsonTemplate.gson.toJson(o);
                    dataStrBuilder.append(oJson).append('\n');
                    if (print) printBuffer.append(oJson).append("\n");
                }
            }
            else {
                String dataJson = JsonTemplate.gsonPretty.toJson(data);
                dataStrBuilder.append(dataJson).append('\n');
                if (print) printBuffer.append(dataJson).append("\n");
            }
        }

        if (print) logger.info("\n" + printBuffer.toString());

        HttpUriRequest httpUriRequest;
        if ("DELETE".equalsIgnoreCase(method)) {
            httpUriRequest = new HttpDelete(path);
        }
        else if ("PUT".equalsIgnoreCase(method)) {
            httpUriRequest = new HttpPut(path);
        }
        else if ("GET".equalsIgnoreCase(method) && data == null) {
            httpUriRequest = new HttpGet(path);
        }
        else httpUriRequest = new HttpPost(path);

        httpUriRequest.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        try {
            if (dataStrBuilder != null && httpUriRequest instanceof HttpEntityEnclosingRequestBase) {
                ((HttpEntityEnclosingRequestBase) httpUriRequest).setEntity(new StringEntity(dataStrBuilder.toString(), this.charset));
            }

            CloseableHttpResponse res = httpclient.execute(httpUriRequest);
            HttpEntity resEntity = res.getEntity();
            String resStr = EntityUtils.toString(resEntity);
            EntityUtils.consume(resEntity);
            return JsonTemplate.gson.fromJson(resStr, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap();
        }
    }

    public void shutdown() {
        running = false;

        try {
            this.httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
