package com.xiyuan.template.util;

import com.google.gson.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

public class Util {

    public static byte[] readAllBytesFromResource(String resourcePath) {
        try (InputStream in = Util.class.getClassLoader().getResourceAsStream(resourcePath)) {
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            return bytes;
        }
        catch (Exception e) {
//            e.printStackTrace();
        }
        return null;
    }

    public static String readFromResource(String resourcePath, Charset charset) {
        byte[] bytes = readAllBytesFromResource(resourcePath);
        return bytes == null ? null : new String(bytes, charset);
    }

    public static byte[] readAllBytesFromFile(String filePath) {
        try (InputStream in = new FileInputStream(filePath)) {
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            return bytes;
        }
        catch (Exception e) {
//            e.printStackTrace();
        }
        return null;
    }

    public static String readFromFile(String filePath, Charset charset) {
        byte[] bytes = readAllBytesFromFile(filePath);
        return bytes == null ? null : new String(bytes, charset);
    }

    private static final JsonParser jsonParser = new JsonParser();

    public static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    public static final Gson gsonPretty = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static JsonElement toJsonElement(String json) {
        return jsonParser.parse(json);
    }

    public static JsonObject toJsonObject(String json) {
        return jsonParser.parse(json).getAsJsonObject();
    }

    public static JsonArray toJsonArray(String json) {
        return jsonParser.parse(json).getAsJsonArray();
    }

    public static JsonElement readJsonFromResource(String resourcePath, Charset charset) {
        return toJsonElement(readFromResource(resourcePath, charset));
    }

    public static JsonElement readJsonFromFile(String filePath, Charset charset) {
        return toJsonElement(readFromFile(filePath, charset));
    }

    public static boolean writeToFile(String filePath, String content, Charset charsets) {
        File file = new File(filePath);
        if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(content.getBytes(charsets));
                return true;
            }
            catch (Exception e) {
//            e.printStackTrace();
            }
        }
        return false;
    }

}
