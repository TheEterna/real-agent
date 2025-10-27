package com.ai.agent.real.contract.utils;

import com.ai.agent.real.contract.tool.AgentTool;

import java.util.*;

/**
 * @author han
 * @time 2025/10/9 12:50
 */

public class PromptUtils {

	/**
	 * @param prompt rendered prompt
	 * @param availableTools avble tools
	 * @return
	 */
	public static String renderToolList(String prompt, List<AgentTool> availableTools, String tag) {
		// 1. if available tools is empty, return original system prompt directly
		if (availableTools == null || availableTools.isEmpty()) {
			return prompt;
		}

		// 2. 构建工具列表字符串
		StringBuilder stringBuilder = new StringBuilder();

		for (int i = 0; i < availableTools.size(); i++) {
			AgentTool tool = availableTools.get(i);
			// 3. 构建工具列表字符串
			stringBuilder.append(i + 1)
				.append(". ")
				.append(tool.getSpec().getName())
				.append(": ")
				.append(tool.getSpec().getDescription())
				.append("\n")
				.append("参数: \n")
				.append(tool.getSpec().getInputSchema())
				.append("\n");
		}
		String toolsContent = stringBuilder.toString().strip();

		// 3. 查找并替换<Tools>标签内容
		String startTag = "<" + tag + ">";
		String endTag = "</" + tag + ">";

		int startIdx = prompt.indexOf(startTag);

		int endIdx = prompt.indexOf(endTag, startIdx + startTag.length());

		if (startIdx == -1 || endIdx == -1) {
			// 如果找不到标签，直接返回原提示词
			return prompt;
		}

		// extract original content between tags
		String originalContent = prompt.substring(startIdx + startTag.length(), endIdx);

		// combine new content with tools content
		String newContent;
		if (originalContent.isBlank()) {
			newContent = toolsContent;
		}
		else {
			newContent = originalContent + "\n" + toolsContent;
		}

		// 构建最终提示词
		return prompt.substring(0, startIdx + startTag.length()) + newContent + prompt.substring(endIdx);
	}

}
