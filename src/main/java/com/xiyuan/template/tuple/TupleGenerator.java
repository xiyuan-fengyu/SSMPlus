package com.xiyuan.template.tuple;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TupleGenerator {

    public static final int fromIndex = 2;

    public static final int toIndex = 5;

    public static final boolean mutable = true;

    public static final String basePackage = "com.xiyuan.template.tuple";

    private static final String javaPath = "src/main/java";

    private static final Charset charset = StandardCharsets.UTF_8;

    public static void main(String[] args) {
        String tempProjectDir = new File(".").getAbsolutePath().replace('\\', '/');
        tempProjectDir = tempProjectDir.substring(0, tempProjectDir.length() - 1);
        final String curProjectDir = tempProjectDir;

        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        Template tupleNVm = ve.getTemplate("template/tupleN.vm");

        for (int i = fromIndex; i <= toIndex; i++) {
            VelocityContext context = new VelocityContext();
            context.put("packageStr", basePackage == null || basePackage.equals("") ? "" : "package " + basePackage + ";");
            context.put("tNum", i);
            context.put("mutable", mutable);

            StringWriter writer = new StringWriter();
            tupleNVm.merge(context, writer);


            File tupleNFile = new File(curProjectDir + javaPath + "/" + ((basePackage == null || basePackage.equals("") ? "" : basePackage + ".") + "Tuple" + (i == 2 ? "" : "" + i)).replace('.', '/') + ".java");
            if (tupleNFile.getParentFile().exists() || tupleNFile.getParentFile().mkdirs()) {
                try (FileOutputStream out = new FileOutputStream(tupleNFile)) {
                    out.write(writer.toString().getBytes(charset));
                }
                catch (Exception e) {
                    System.err.println("文件保存失败：" + tupleNFile);
                    e.printStackTrace();
                }
            }
            else {
                System.err.println("文件夹创建失败：" + tupleNFile.getParentFile());
            }
        }
    }

}
