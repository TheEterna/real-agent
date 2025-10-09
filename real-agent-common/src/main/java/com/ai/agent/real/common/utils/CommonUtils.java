package com.ai.agent.real.common.utils;

import java.util.*;

/**
 * @author han
 * @time 2025/9/10 14:47
 */

public class CommonUtils {

	public static String getTraceId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	public static String getNodeId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

}
