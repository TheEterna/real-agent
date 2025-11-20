package com.ai.agent.real.common.utils;

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

}
