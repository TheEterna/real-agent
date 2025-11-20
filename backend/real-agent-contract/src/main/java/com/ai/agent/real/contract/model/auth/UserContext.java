package com.ai.agent.real.contract.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

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
	private UUID userId;

	/**
	 * 外部 ID（登录账号）
	 */
	private String externalId;

	/**
	 * IP 地址（用于设备绑定验证）
	 */
	private String ipAddress;

	/**
	 * 设备信息 (User-Agent，用于设备绑定验证)
	 */
	private String deviceInfo;

	/**
	 * 是否已认证
	 */
	public boolean isAuthenticated() {
		return userId != null;
	}

	/**
	 * 是否匿名用户
	 */
	public boolean isAnonymous() {
		return !isAuthenticated();
	}

}
