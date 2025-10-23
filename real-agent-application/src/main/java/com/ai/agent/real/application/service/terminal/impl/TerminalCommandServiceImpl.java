package com.ai.agent.real.application.service.terminal.impl;

import com.ai.agent.real.contract.service.terminal.TerminalCommandService;
import com.ai.agent.real.domain.entity.TerminalCommand;
import com.ai.agent.real.domain.repository.TerminalCommandRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 终端命令配置服务实现
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Service
public class TerminalCommandServiceImpl implements TerminalCommandService {

	private final TerminalCommandRepository commandRepository;

	public TerminalCommandServiceImpl(TerminalCommandRepository commandRepository) {
		this.commandRepository = commandRepository;
	}

	@Override
	public Flux<TerminalCommand> getAllCommands() {
		return commandRepository.findAll();
	}

	@Override
	public Flux<TerminalCommand> getVisibleCommands() {
		return commandRepository.findVisibleCommands();
	}

	@Override
	public Mono<TerminalCommand> getCommandByName(String name) {
		return commandRepository.findByName(name.toLowerCase());
	}

	@Override
	public Flux<TerminalCommand> getCommandsByCategory(String category) {
		return commandRepository.findByCategory(category);
	}

	@Override
	public Mono<TerminalCommand> createCommand(TerminalCommand command) {
		// 设置ID和时间戳
		if (command.getId() == null || command.getId().isEmpty()) {
			command.setId("cmd-" + UUID.randomUUID().toString());
		}
		command.setCreatedAt(LocalDateTime.now());
		command.setUpdatedAt(LocalDateTime.now());

		// 默认值
		if (command.getEnabled() == null) {
			command.setEnabled(true);
		}
		if (command.getHidden() == null) {
			command.setHidden(false);
		}
		if (command.getDeprecated() == null) {
			command.setDeprecated(false);
		}

		return commandRepository.save(command);
	}

	@Override
	public Mono<TerminalCommand> updateCommand(String name, TerminalCommand command) {
		return commandRepository.findByName(name.toLowerCase()).flatMap(existing -> {
			// 保留ID和创建时间
			command.setId(existing.getId());
			command.setCreatedAt(existing.getCreatedAt());
			command.setUpdatedAt(LocalDateTime.now());
			return commandRepository.save(command);
		});
	}

	@Override
	public Mono<Void> deleteCommand(String name) {
		return commandRepository.findByName(name.toLowerCase())
			.flatMap(command -> commandRepository.deleteById(command.getId()));
	}

	@Override
	public Mono<TerminalCommand> toggleCommand(String name, boolean enabled) {
		return commandRepository.findByName(name.toLowerCase()).flatMap(command -> {
			command.setEnabled(enabled);
			command.setUpdatedAt(LocalDateTime.now());
			return commandRepository.save(command);
		});
	}

}
