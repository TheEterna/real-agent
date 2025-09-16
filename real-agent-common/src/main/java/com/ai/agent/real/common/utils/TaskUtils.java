package com.ai.agent.real.common.utils;

/**
 * @author han
 * @time 2025/9/9 0:49
 */

public class TaskUtils {

    /**
     * 检查任务是否完成
     */
    public static boolean isTaskComplete(String thought) {
        return thought.toLowerCase().contains("任务完成") ||
                thought.toLowerCase().contains("完成") ||
                thought.toLowerCase().contains("finish");
    }

}
