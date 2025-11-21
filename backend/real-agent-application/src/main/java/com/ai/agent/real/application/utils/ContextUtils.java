package com.ai.agent.real.application.utils;

import com.ai.agent.real.contract.agent.context.AgentContextAble;
import com.ai.agent.real.contract.model.message.AgentMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 上下文管理器 负责管理对话历史的大小，防止 token 超限
 *
 * @author han
 * @time 2025/11/06
 */
@Slf4j
@Service
public class ContextUtils {

	/**
	 * 最大 token 数量（根据模型调整） qwen-max 支持 8k 上下文
	 */
	private static final int MAX_TOKENS = 8000;

	/**
	 * 为新输出预留的 token 数量
	 */
	private static final int RESERVED_TOKENS = 2000;

	/**
	 * 保留最近消息的数量
	 */
	private static final int KEEP_RECENT_COUNT = 5;

	/**
	 * 管理上下文大小，超限时自动压缩
	 * @param context Agent 上下文
	 */
	public static void manageContextSize(AgentContextAble context) {
		List<AgentMessage> history = context.getMessageHistory();

		if (history == null || history.isEmpty()) {
			return;
		}

		// 计算当前 token 数量
		int totalTokens = calculateTokens(history);
		int availableTokens = MAX_TOKENS - RESERVED_TOKENS;

		if (totalTokens > availableTokens) {
			log.info("上下文超限，开始压缩: 当前 {} tokens, 限制 {} tokens", totalTokens, availableTokens);

			List<AgentMessage> compressedHistory = compressContext(history);
			context.setMessageHistory(compressedHistory);

			int newTokens = calculateTokens(compressedHistory);
			log.info("上下文压缩完成: {} tokens -> {} tokens, 压缩率 {}", totalTokens, newTokens,
					String.format("%.1f%%", (1 - (double) newTokens / totalTokens) * 100));
		}
	}

	/**
	 * 压缩上下文历史 策略：保留系统提示词 + 压缩中间消息 + 保留最近消息
	 * @param history 原始历史消息
	 * @return 压缩后的历史消息
	 */
	private static List<AgentMessage> compressContext(List<AgentMessage> history) {
		List<AgentMessage> result = new ArrayList<>();

		// 1. 保留第一条系统提示词（如果存在）
		if (!history.isEmpty() && history.get(0).getAgentMessageType().equals(AgentMessage.AgentMessageType.SYSTEM)) {
			result.add(history.get(0));
		}

		// 2. 保留最近的 N 条消息
		int startIndex = history.get(0).getAgentMessageType().equals(AgentMessage.AgentMessageType.SYSTEM) ? 1 : 0;
		int recentStartIndex = Math.max(startIndex, history.size() - KEEP_RECENT_COUNT);
		List<AgentMessage> recentMessages = history.subList(recentStartIndex, history.size());

		// 3. 如果中间有消息需要压缩
		if (recentStartIndex > startIndex) {
			List<AgentMessage> middleMessages = history.subList(startIndex, recentStartIndex);

			// 压缩中间消息为摘要
			String summary = summarizeMessages(middleMessages);
			result.add(AgentMessage.system("【上下文摘要】\n" + summary + "\n\n（为节省 token，部分历史消息已压缩为摘要）"));
		}

		// 4. 添加最近的消息
		result.addAll(recentMessages);

		return result;
	}

	/**
	 * 将一组消息压缩为摘要
	 * @param messages 要压缩的消息列表
	 * @return 摘要文本
	 */
	private static String summarizeMessages(List<AgentMessage> messages) {
		StringBuilder summary = new StringBuilder();

		// 统计信息
		long userMessages = messages.stream()
			.filter(m -> AgentMessage.AgentMessageType.USER.equals(m.getAgentMessageType()))
			.count();
		long assistantMessages = messages.stream()
			.filter(m -> AgentMessage.AgentMessageType.USER.equals(m.getAgentMessageType()))
			.count();
		long toolMessages = messages.stream()
			.filter(m -> AgentMessage.AgentMessageType.TOOL.equals(m.getAgentMessageType()))
			.count();

		summary.append(
				String.format("本轮对话包含 %d 条用户消息、%d 条助手回复、%d 次工具调用。\n\n", userMessages, assistantMessages, toolMessages));

		// 提取关键信息：用户的主要问题和助手的主要回答
		List<String> keyPoints = new ArrayList<>();

		for (int i = 0; i < messages.size(); i++) {
			AgentMessage msg = messages.get(i);

			if (AgentMessage.AgentMessageType.USER.equals(msg.getAgentMessageType())) {
				// 保留用户问题的前100个字符
				String question = truncateText(msg.getText(), 100);
				keyPoints.add("用户: " + question);
			}
			else if (AgentMessage.AgentMessageType.ASSISTANT.equals(msg.getAgentMessageType())) {
				// 保留助手回答的前150个字符
				String answer = truncateText(msg.getText(), 150);
				keyPoints.add("助手: " + answer);
			}
		}

		// 只保留前 5 个关键点
		if (keyPoints.size() > 5) {
			keyPoints = keyPoints.subList(0, 5);
			summary.append("主要交互记录（前5条）:\n");
		}
		else {
			summary.append("主要交互记录:\n");
		}

		summary.append(keyPoints.stream().collect(Collectors.joining("\n")));

		return summary.toString();
	}

	/**
	 * 截断文本到指定长度
	 * @param text 原始文本
	 * @param maxLength 最大长度
	 * @return 截断后的文本
	 */
	private static String truncateText(String text, int maxLength) {
		if (text == null || text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength) + "...";
	}

	/**
	 * 计算消息列表的 token 数量（估算） 简化算法：中文字符按 1.5 token 计算，英文单词按 1 token 计算
	 * @param messages 消息列表
	 * @return 估算的 token 数量
	 */
	private static int calculateTokens(List<AgentMessage> messages) {
		int totalTokens = 0;

		for (AgentMessage message : messages) {
			String text = message.getText();
			if (text == null)
				continue;

			// 统计中文字符数（简化：所有非 ASCII 字符视为中文）
			int chineseChars = 0;
			int otherChars = 0;

			for (char c : text.toCharArray()) {
				if (c > 127) {
					chineseChars++;
				}
				else if (!Character.isWhitespace(c)) {
					otherChars++;
				}
			}

			// 估算 token：中文字符 * 1.5 + 英文字符 / 4（平均单词长度）
			totalTokens += (int) (chineseChars * 1.5 + otherChars / 4.0);

			// 每条消息额外增加 4 个 token（role、name 等元数据）
			totalTokens += 4;
		}

		return totalTokens;
	}

	/**
	 * 检查上下文是否即将超限
	 * @param context Agent 上下文
	 * @return 是否接近限制（超过 80%）
	 */
	public static boolean isContextNearLimit(AgentContextAble context) {
		List<AgentMessage> history = context.getMessageHistory();
		if (history == null || history.isEmpty()) {
			return false;
		}

		int totalTokens = calculateTokens(history);
		int threshold = (int) ((MAX_TOKENS - RESERVED_TOKENS) * 0.8);

		return totalTokens > threshold;
	}

	/**
	 * 获取当前上下文使用情况
	 * @param context Agent 上下文
	 * @return 格式化的使用情况字符串
	 */
	public static String getContextUsage(AgentContextAble context) {
		List<AgentMessage> history = context.getMessageHistory();
		if (history == null || history.isEmpty()) {
			return "0 / " + (MAX_TOKENS - RESERVED_TOKENS) + " tokens (0%)";
		}

		int totalTokens = calculateTokens(history);
		int available = MAX_TOKENS - RESERVED_TOKENS;
		double percentage = (double) totalTokens / available * 100;

		return String.format("%d / %d tokens (%.1f%%)", totalTokens, available, percentage);
	}

}
