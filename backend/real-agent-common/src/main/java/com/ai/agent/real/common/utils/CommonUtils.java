package com.ai.agent.real.common.utils;

import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author han
 * @time 2025/9/10 14:47
 */

public class CommonUtils {

	public static String getTraceId(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().replace("-", "");
	}

	public static String getMessageId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * 生成基于 UUID 的 Token（备选方案）
	 */
	public static String generateUuidToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	public static String defaultIfBlank(String str, String defaultStr) {
		return StringUtils.hasText(str) ? str : defaultStr;
	}

	public static Double defaultIfBlank(Double doubleNum, Double defaultDoubleNum) {
		return doubleNum != null ? doubleNum : defaultDoubleNum;
	}

	public static Integer defaultIfBlank(Integer integerNum, Integer defaultIntegerNum) {
		return integerNum != null ? integerNum : defaultIntegerNum;
	}

	public static <T> List<T> defaultIfBlank(List<T> list, List<T> defaultList, Class<T> clazz) {
		return list != null && !list.isEmpty() ? list : defaultList;
	}

	/**
	 * 判断是否是公开路径
	 */
	public static boolean isPublicPath(String path) {
		return path.startsWith("/api/auth/") || path.startsWith("/api/public/") || path.equals("/")
				|| path.startsWith("/actuator/");
	}

}
