package com.ai.agent.real.common.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户上下文
 *
 * @author: han
 * @time: 2025/10/23 13:13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 用户 ID
	 */
	private Long userId;

	/**
	 * 外部 ID（登录账号）
	 */
	private String externalId;

	/**
	 * 昵称
	 */
	private String nickname;

	/**
	 * 头像 URL
	 */
	private String avatarUrl;

	/**
	 * 扩展属性
	 */
	@Builder.Default
	private Map<String, Object> attributes = new HashMap<>();

	/**
	 * 是否已认证
	 */
	public boolean isAuthenticated() {
		return userId != null && userId > 0;
	}

	/**
	 * 是否匿名用户
	 */
	public boolean isAnonymous() {
		return !isAuthenticated();
	}

	/**
	 * 设置扩展属性
	 */
	public void setAttribute(String key, Object value) {
		if (attributes == null) {
			attributes = new HashMap<>();
		}
		attributes.put(key, value);
	}

	/**
	 * 获取扩展属性
	 */
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String key) {
		if (attributes == null) {
			return null;
		}
		return (T) attributes.get(key);
	}

}
