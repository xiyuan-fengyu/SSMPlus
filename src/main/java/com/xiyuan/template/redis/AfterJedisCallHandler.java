package com.xiyuan.template.redis;

import com.xiyuan.template.redis.annotation.AfterJedisCall;
import com.xiyuan.template.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xiyuan_fengyu on 2018/9/6 15:11.
 */
public class AfterJedisCallHandler {

    private static final Logger logger = LoggerFactory.getLogger(AfterJedisCallHandler.class);

    private ApplicationContext applicationContext;

    private List<Mapping> mappings = new ArrayList<>();

    public AfterJedisCallHandler(ApplicationContext applicationContext) throws Exception {
        this.applicationContext = applicationContext;
        collectMapping();
    }

    private void collectMapping() throws Exception {
        HashSet<Class> classes = ClassUtil.getClasses("", true, true, false);
        for (Class clazz : classes) {
            if (clazz.getDeclaredAnnotation(Component.class) != null) {
                for (Method method : clazz.getDeclaredMethods()) {
                    AfterJedisCall afterJedisCall = method.getDeclaredAnnotation(AfterJedisCall.class);
                    if (afterJedisCall != null) {
                        if (afterJedisCall.argIndexsToMatchAsKey().length != afterJedisCall.keyPatterns().length) {
                            throw new BadParametersForAfterJedisCallAnnotationException(AfterJedisCall.class.getSimpleName() + " at " + method + ": argIndexsToMatchAsKey.length != keyPatterns.length");
                        }
                        else {
                            for (String p : afterJedisCall.keyPatterns()) {
                                if (p == null) {
                                    throw new BadParametersForAfterJedisCallAnnotationException(AfterJedisCall.class.getSimpleName() + " at " + method + ": item of keyPatterns cannot be null");
                                }
                            }
                        }

                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 5
                                && parameterTypes[0] == Jedis.class
                                && parameterTypes[1] == Method.class
                                && parameterTypes[2] == Object[].class
                                && parameterTypes[3] == Object.class
                                && parameterTypes[4] == String[][].class) {
                            Mapping mapping = new Mapping();
                            mapping.method = afterJedisCall.method();
                            mapping.argIndexsToMatchAsKey = afterJedisCall.argIndexsToMatchAsKey();
                            mapping.keyPatterns = new Pattern[afterJedisCall.keyPatterns().length];
                            for (int i = 0, len = mapping.keyPatterns.length; i < len; i++) {
                                mapping.keyPatterns[i] = Pattern.compile(afterJedisCall.keyPatterns()[i]);
                            }
                            mapping.callbackMethod = method;
                            mapping.callbackTarget = applicationContext.getBean(clazz);
                            mappings.add(mapping);
                        }
                        else {
                            throw new BadParameterTypesException(method);
                        }
                    }
                }
            }
        }
    }

    public Object dispatch(Jedis jedis, Method method, Object[] args, Object resultFromJedis) throws Exception {
        String[][] matchs;
        String[] match;
        for (Mapping mapping : mappings) {
            if (mapping.method.equals(method.getName())) {
                boolean allKeyMatch = true;
                int matchKeyNum = mapping.argIndexsToMatchAsKey.length;
                matchs = new String[matchKeyNum][];
                for (int i = 0; i < matchKeyNum; i++) {
                    int argIndex = mapping.argIndexsToMatchAsKey[i];
                    if (argIndex > args.length) {
                        allKeyMatch = false;
                        break;
                    }
                    else {
                        Object arg = args[argIndex];
                        if (arg instanceof String) {
                            Matcher matcher = mapping.keyPatterns[i].matcher((CharSequence) arg);
                            if (matcher.find()) {
                                int groupCount = matcher.groupCount();
                                match = matchs[i] = new String[groupCount + 1];
                                for (int j = 0; j <= groupCount; j++) {
                                    match[j] = matcher.group(j);
                                }
                            }
                            else {
                                allKeyMatch = false;
                                break;
                            }
                        }
                    }
                }
                if (allKeyMatch) {
                    Object callbackResult = mapping.callbackMethod.invoke(mapping.callbackTarget, jedis, method, args, resultFromJedis, matchs);
                    if (callbackResult != null) {
                        return callbackResult;
                    }
                    break;
                }
            }
        }
        return resultFromJedis;
    }

    private static class Mapping {

        private String method;

        private int[] argIndexsToMatchAsKey;

        private Pattern[] keyPatterns;

        private Object callbackTarget;

        private Method callbackMethod;

    }

    private static class BadParameterTypesException extends Exception {
        public BadParameterTypesException(Method method) {
            super("Bad parameter types of method: " + method);
        }
    }

    private static class BadParametersForAfterJedisCallAnnotationException extends Exception {
        public BadParametersForAfterJedisCallAnnotationException(String message) {
            super(message);
        }
    }

}
