package com.ai.agent.real.common.utils;

import java.util.UUID;

/**
 * UUID 工具类
 */
public class UuidUtils {
    
    /**
     * 生成32位UUID（去除横线）
     */
    public static String generate32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成标准UUID
     */
    public static String generateStandard() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 生成会话编码
     */
    public static String generateSessionCode() {
        // Ensure total length fits typical VARCHAR(32): 5 (prefix) + 27 = 32
        return "sess_" + generate32().substring(0, 27);
    }
}
