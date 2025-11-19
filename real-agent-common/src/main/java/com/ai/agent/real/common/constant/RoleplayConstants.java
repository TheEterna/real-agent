package com.ai.agent.real.common.constant;

/**
 * 角色扮演相关常量
 */
public class RoleplayConstants {

	/**
	 * 会话状态
	 */
	public static class SessionStatus {

		/** 进行中 */
		public static final int ACTIVE = 1;

		/** 已结束 */
		public static final int ENDED = 2;

		/** 异常 */
		public static final int ERROR = 3;

	}

	/**
	 * 角色状态
	 */
	public static class RoleStatus {

		/** 启用 */
		public static final int ENABLED = 1;

		/** 停用 */
		public static final int DISABLED = 0;

	}

	/**
	 * 会话模式
	 */
	public static class SessionMode {

		/** 文本模式 */
		public static final String TEXT = "text";

		/** 语音模式 */
		public static final String VOICE = "voice";

	}

	/**
	 * 消息类型
	 */
	public static class MessageType {

		/** 用户文本 */
		public static final String USER_TEXT = "user_text";

		/** 助手文本 */
		public static final String ASSISTANT_TEXT = "assistant_text";

		/** 助手语音 */
		public static final String ASSISTANT_AUDIO = "assistant_audio";

		/** 事件 */
		public static final String EVENT = "event";

		/** 工具调用 */
		public static final String TOOL_CALL = "tool_call";

		/** 工具结果 */
		public static final String TOOL_RESULT = "tool_result";

	}

	/**
	 * 消息角色
	 */
	public static class MessageRole {

		/** 用户 */
		public static final String USER = "user";

		/** 助手 */
		public static final String ASSISTANT = "assistant";

		/** 系统 */
		public static final String SYSTEM = "system";

	}

}
