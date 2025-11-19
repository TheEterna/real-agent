package com.ai.agent.real.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * JSON 转换工具类
 */
@Slf4j
public class JsonUtils {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Map 转 JSON 字符串
	 */
	public static String mapToJsonString(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return "{}";
		}
		try {
			return objectMapper.writeValueAsString(map);
		}
		catch (JsonProcessingException e) {
			log.error("Map转JSON字符串失败", e);
			return "{}";
		}
	}

	/**
	 * JSON 字符串转 Map
	 */
	public static Map<String, Object> jsonStringToMap(String jsonString) {
		if (jsonString == null || jsonString.trim().isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
			});
		}
		catch (JsonProcessingException e) {
			log.error("JSON字符串转Map失败: {}", jsonString, e);
			return Collections.emptyMap();
		}
	}

	/**
	 * 对象转 JSON 字符串
	 */
	public static String objectToJsonString(Object obj) {
		if (obj == null) {
			return "{}";
		}
		try {
			return objectMapper.writeValueAsString(obj);
		}
		catch (JsonProcessingException e) {
			log.error("对象转JSON字符串失败", e);
			return "{}";
		}
	}

	/**
	 * JSON 字符串转对象
	 */
	public static <T> T jsonStringToObject(String jsonString, Class<T> clazz) {
		if (jsonString == null || jsonString.trim().isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(jsonString, clazz);
		}
		catch (JsonProcessingException e) {
			log.error("JSON字符串转对象失败: {}", jsonString, e);
			return null;
		}
	}

}
