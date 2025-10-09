package com.ai.agent.real.application.service.Impl;

import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplayRole;
import com.ai.agent.real.domain.entity.roleplay.PlaygroundRoleplayRole.*;
import com.ai.agent.real.domain.repository.roleplay.PlaygroundRoleplayRoleRepository;
import com.ai.agent.real.application.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 角色扮演角色服务层 (R2DBC 响应式)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaygroundRoleplayRoleServiceImpl
		implements com.ai.agent.real.application.service.PlaygroundRoleplayRoleService {

	private final PlaygroundRoleplayRoleRepository roleRepository;

	/**
	 * 创建角色
	 */
	@Override
	public Mono<PlaygroundRoleplayRole> createRole(RoleCreateRequest request) {
		// 参数验证
		if (request.getVoice() == null) {
			return Mono.error(new IllegalArgumentException("角色音色不能为空"));
		}

		String name = request.getName();
		if (name.isBlank()) {
			return Mono.error(new IllegalArgumentException("角色名称不能为空"));
		}
		if (name.length() > 100) {
			return Mono.error(new IllegalArgumentException("角色名称不能超过100个字符"));
		}

		VoiceEnum voice = request.getVoice();
		String description = request.getDescription();
		String avatarUrl = request.getAvatarUrl();
		Integer status = request.getStatus();

		return Mono.defer(() -> {
			PlaygroundRoleplayRole role = PlaygroundRoleplayRole.builder()
				.voice(voice)
				.name(name)
				.description(description)
				.avatarUrl(avatarUrl)
				.status(status)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
			return roleRepository.save(role);
		})
			.doOnSuccess(savedRole -> log.info("创建角色成功: id={}, voice={}, name={}, avatarUrl={}", savedRole.getId(),
					savedRole.getVoice(), savedRole.getName(), savedRole.getAvatarUrl()))
			.doOnError(error -> log.error("创建角色失败: voice={}, name={}", voice, name, error))
			.onErrorResume(IllegalArgumentException.class, Mono::error);
	}

	/**
	 * 更新角色
	 */
	@Override
	public Mono<PlaygroundRoleplayRole> updateRole(Long id, RoleCreateRequest request) {
		if (request.getVoice() == null) {
			return Mono.error(new IllegalArgumentException("角色音色不能为空"));
		}
		String name = request.getName();
		if (name.isBlank()) {
			return Mono.error(new IllegalArgumentException("角色名称不能为空"));
		}
		if (name.length() > 100) {
			return Mono.error(new IllegalArgumentException("角色名称不能超过100个字符"));
		}
		VoiceEnum voice = request.getVoice();
		String description = request.getDescription();
		String avatarUrl = request.getAvatarUrl();
		Integer status = request.getStatus();
		return roleRepository.findById(id)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("角色不存在: " + id)))
			.map(existingRole -> {
				existingRole.setVoice(voice);
				existingRole.setName(name);
				existingRole.setDescription(description);
				existingRole.setAvatarUrl(avatarUrl);
				existingRole.setUpdatedAt(LocalDateTime.now());
				existingRole.setNew(false); // 更新操作
				return existingRole;
			})
			.flatMap(roleRepository::save)
			.doOnSuccess(updatedRole -> log.debug("更新角色成功: id={}, voice={}, name={}", updatedRole.getId(),
					updatedRole.getVoice(), updatedRole.getName()))
			.doOnError(error -> log.error("更新角色失败: id={}", id, error));
	}

	/**
	 * 删除角色
	 */
	@Override
	public Mono<Void> deleteRole(Long id) {
		return roleRepository.existsById(id).flatMap(exists -> {
			if (!exists) {
				return Mono.error(new IllegalArgumentException("角色不存在: " + id));
			}
			return roleRepository.deleteById(id);
		})
			.doOnSuccess(unused -> log.info("删除角色成功: id={}", id))
			.doOnError(error -> log.error("删除角色失败: id={}", id, error));
	}

	/**
	 * 查找所有角色
	 */
	@Override
	public Flux<PlaygroundRoleplayRole> findAllRoles() {
		return roleRepository.findAll();
	}

	/**
	 * 查找启用的角色
	 */
	@Override
	public Flux<PlaygroundRoleplayRole> findActiveRoles() {
		return roleRepository.findActiveRoles();
	}

	/**
	 * 启用/禁用角色
	 */
	@Override
	public Mono<PlaygroundRoleplayRole> toggleRoleStatus(Long id) {
		return roleRepository.findById(id)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("角色不存在: " + id)))
			.map(role -> {
				role.setStatus(role.getStatus());
				role.setUpdatedAt(LocalDateTime.now());
				role.setNew(false);
				return role;
			})
			.flatMap(roleRepository::save)
			.doOnSuccess(
					updatedRole -> log.info("切换角色状态成功: id={}, status={}", updatedRole.getId(), updatedRole.getStatus()))
			.doOnError(error -> log.error("切换角色状态失败: id={}", id, error));
	}

	/**
	 * 根据ID查找角色
	 */
	@Override
	public Mono<PlaygroundRoleplayRole> findById(Long id) {
		return roleRepository.findById(id)
			.switchIfEmpty(Mono.error(new IllegalArgumentException("角色不存在: " + id)))
			.map(role -> {
				// 如果创建时间小于一周，设置 isNew 为 true
				if (role.getCreatedAt().isAfter(LocalDateTime.now().minusWeeks(1))) {
					role.setNew(true);
				}
				else {
					role.setNew(false);
				}
				return role;
			});
	}

}
