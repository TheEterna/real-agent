package com.ai.agent.real.application.tool;

import com.ai.agent.real.contract.protocol.*;

import java.util.*;

/**
 * @author han
 * @time 2025/9/18 20:15
 */

public interface ToolExecutionService {
    ToolExecuteResult execute(String toolName, Map<String, Object> args);
}
