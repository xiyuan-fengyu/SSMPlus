package com.xiyuan.template.util;

import com.baomidou.mybatisplus.annotations.TableField;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xiyuan_fengyu on 2016/8/26.
 */
public class ClassUtil {

	private static Map<Class<?>, ClassAndMethod> classNameAndMethod = new HashMap<>();

	private static class ClassAndMethod {
		private Class<?> clazz;
		private Method method;

		public ClassAndMethod(Class<?> clazz, Method method) {
			this.clazz = clazz;
			this.method = method;
		}
	}

	static {
		try {
			{
				Method method = Byte.class.getMethod("parseByte", String.class);
				ClassAndMethod classAndMethod = new ClassAndMethod(Byte.class, method);
				classNameAndMethod.put(Byte.class, classAndMethod);
				classNameAndMethod.put(byte.class, classAndMethod);
			}

			{
				Method method = Boolean.class.getMethod("parseBoolean", String.class);
				ClassAndMethod classAndMethod = new ClassAndMethod(Boolean.class, method);
				classNameAndMethod.put(Boolean.class, classAndMethod);
				classNameAndMethod.put(boolean.class, classAndMethod);
			}

			{
				Method method = Short.class.getMethod("parseShort", String.class);
				ClassAndMethod classAndMethod = new ClassAndMethod(Short.class, method);
				classNameAndMethod.put(Short.class, classAndMethod);
				classNameAndMethod.put(short.class, classAndMethod);
			}

			{
				Method method = Integer.class.getMethod("parseInt", String.class);
				ClassAndMethod classAndMethod = new ClassAndMethod(Integer.class, method);
				classNameAndMethod.put(Integer.class, classAndMethod);
				classNameAndMethod.put(int.class, classAndMethod);
			}

			{
				Method method = Long.class.getMethod("parseLong", String.class);
				ClassAndMethod classAndMethod = new ClassAndMethod(Long.class, method);
				classNameAndMethod.put(Long.class, classAndMethod);
				classNameAndMethod.put(long.class, classAndMethod);
			}

			{
				Method method = Float.class.getMethod("parseFloat", String.class);
				ClassAndMethod classAndMethod = new ClassAndMethod(Float.class, method);
				classNameAndMethod.put(Float.class, classAndMethod);
				classNameAndMethod.put(float.class, classAndMethod);
			}

			{
				Method method = Double.class.getMethod("parseDouble", String.class);
				ClassAndMethod classAndMethod = new ClassAndMethod(Double.class, method);
				classNameAndMethod.put(Double.class, classAndMethod);
				classNameAndMethod.put(double.class, classAndMethod);
			}

		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	private static Pattern datePattern = Pattern.compile("(\\d{4,4})?[-_\\/]?(\\d{1,2})?[-_\\/]?(\\d{1,2})?[ ,_]?(\\d{1,2})?[:]?(\\d{1,2})?[:]?(\\d{1,2})?");

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static <T> void setValue(T instance, Field field, Object value) {
		if (value == null) {
			return;
		}
		try {
			field.setAccessible(true);
			Class<?> fieldType = field.getType();
			if (fieldType == String.class) {
				field.set(instance, value);
			}
			else if (fieldType == char.class || fieldType == Character.class) {
				String strVal = value.toString();
				field.set(instance, strVal.isEmpty() ? '\0' : strVal.charAt(0));
			}
			else if (fieldType == Date.class) {
				String strVal = value.toString();
				if (strVal.matches("\\d{13,13}")) {
					field.set(instance, new Date(Long.parseLong(strVal)));
				}
				else {
					field.set(instance, parseDate(strVal));
				}
			}
			else {
				ClassAndMethod classAndMethod = classNameAndMethod.get(fieldType);
				if (classAndMethod != null) {
					field.set(instance, classAndMethod.method.invoke(classAndMethod.clazz, value));
				}
			}

		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private static Date parseDate(String value) {
		Matcher dateMatcher = datePattern.matcher(value);
		if (dateMatcher.find()) {
			String dateStr = "";
			String groupItem = dateMatcher.group(1);
			if (groupItem == null) {
				dateStr += "1997";
			}
			else if (groupItem.length() == 1) {
				dateStr += "0" + groupItem;
			}
			else {
				dateStr += groupItem;
			}
			dateStr += "-";

			groupItem = dateMatcher.group(2);
			if (groupItem == null) {
				dateStr += "01";
			}
			else if (groupItem.length() == 1) {
				dateStr += "0" + groupItem;
			}
			else {
				dateStr += groupItem;
			}
			dateStr += "-";

			groupItem = dateMatcher.group(3);
			if (groupItem == null) {
				dateStr += "01";
			}
			else if (groupItem.length() == 1) {
				dateStr += "0" + groupItem;
			}
			else {
				dateStr += groupItem;
			}
			dateStr += " ";

			groupItem = dateMatcher.group(4);
			if (groupItem == null) {
				dateStr += "00";
			}
			else if (groupItem.length() == 1) {
				dateStr += "0" + groupItem;
			}
			else {
				dateStr += groupItem;
			}
			dateStr += ":";

			groupItem = dateMatcher.group(5);
			if (groupItem == null) {
				dateStr += "00";
			}
			else if (groupItem.length() == 1) {
				dateStr += "0" + groupItem;
			}
			else {
				dateStr += groupItem;
			}
			dateStr += ":";

			groupItem = dateMatcher.group(6);
			if (groupItem == null) {
				dateStr += "00";
			}
			else if (groupItem.length() == 1) {
				dateStr += "0" + groupItem;
			}
			else {
				dateStr += groupItem;
			}

			try {
				return dateFormat.parse(dateStr);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static final ConcurrentHashMap<Class, Map<String, Field>> fieldsCache = new ConcurrentHashMap<>();

	private static Map<String, Field> getFields(Class clazz) {
		Map<String, Field> cache = fieldsCache.get(clazz);
		if (cache == null) {
			cache = new HashMap<>();
			for (Field field : clazz.getDeclaredFields()) {
				int modifier = field.getModifiers();
				if (Modifier.isStatic(modifier)) {
					continue;
				}

				if (!Modifier.isPublic(modifier)) {
					field.setAccessible(true);
				}

				String colName;
				TableField tableField = field.getAnnotation(TableField.class);
				if (tableField == null) {
					colName = field.getName();
				}
				else {
					colName = tableField.value();
				}
				cache.put(colName, field);
			}
			fieldsCache.put(clazz, cache);
		}
		return cache;
	}

	public static <T, V> T mapToEntity(Map<String, V> from, Class<T> toClass) {
		T toInstance = null;
		try {
			toInstance = toClass.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (toInstance != null) {
			Map<String, Field> fieldsMap = getFields(toClass);
			for (Map.Entry<String, V> entry : from.entrySet()) {
				try {
					setValue(toInstance, fieldsMap.get(entry.getKey()), entry.getValue());
				}
				catch (Exception e) {
//					e.printStackTrace();
				}
			}
		}

		return toInstance;
	}

	public static <T> Map<String, String> entityToMap(T from) {
		Map<String, String> map = new HashMap<>();
		if (from != null) {
			Map<String, Field> fieldsMap = getFields(from.getClass());
			for (Map.Entry<String, Field> entry : fieldsMap.entrySet()) {
				try {
					Object value = entry.getValue().get(from);
					if (value != null) {
						if (value instanceof Date) {
							map.put(entry.getKey(), dateFormat.format(value));
						}
						else {
							map.put(entry.getKey(), value.toString());
						}
					}
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
		}
		return map;
	}

}
