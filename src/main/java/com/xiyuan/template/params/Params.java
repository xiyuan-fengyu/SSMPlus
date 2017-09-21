package com.xiyuan.template.params;

import com.xiyuan.template.params.checker.Checker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;

public class Params {

    private static final HashMap<Class, Checker> checkers = new HashMap<>();

    public String check() {
        Class clazz = this.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = null;
            try {
                value = field.get(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            Annotation[] annos = field.getAnnotations();
            if (annos != null && annos.length > 0) {
                for (Annotation anno : annos) {
                    Class annoType = anno.annotationType();
                    Checker checker = checkers.get(annoType);
                    if (checker == null) {
                        try {
                            Class<Checker> checkerType = (Class<Checker>) annoType.getMethod("checker").invoke(anno);
                            checker = checkerType.newInstance();
                            checkers.put(annoType, checker);
                        } catch (Exception e) {
//                            e.printStackTrace();
                        }
                    }
                    if (checker != null && !checker.valid(anno, value)) {
                        try {
                            return (String) annoType.getMethod("error").invoke(anno);
                        } catch (Exception e) {
//                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

}
