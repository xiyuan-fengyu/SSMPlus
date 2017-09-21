package com.xiyuan.template.params.checker;

import com.xiyuan.template.params.annotation.JsExp;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

public class JsExpChecker implements Checker<JsExp> {

    private static final ScriptEngine jsEngine = new ScriptEngineManager().getEngineByName("js");

    @Override
    public boolean valid(JsExp anno, Object value, Object ctx) {
        String exp = anno.exp();
        try {
            SimpleBindings bindings = new SimpleBindings();
            bindings.put(anno.valueName(), value);
            bindings.put(anno.contextName(), ctx);
            Object res = jsEngine.eval(exp, bindings);
            return res != null && !(Boolean.FALSE.equals(res));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
