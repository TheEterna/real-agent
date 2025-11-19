package com.ai.agent.real.application.plugin.terminal;

import com.ai.agent.real.contract.plugin.terminal.ICommandService;
import com.ai.agent.real.domain.entity.plguin.TerminalCommand;
import com.ai.agent.real.domain.repository.ITerminalCommandRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * @author han
 * @time 2025/10/27 23:08
 */
public class CommandService implements ICommandService {

	private final ITerminalCommandRepository commandRepository;

	public CommandService(ITerminalCommandRepository commandRepository) {
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
	public Mono<TerminalCommand> updateCommand(String name, TerminalCommand command) {
		return commandRepository.findByName(name.toLowerCase()).flatMap(existing -> {
			// 保留ID和创建时间
			command.setId(existing.getId());
			command.setCreatedTime(existing.getCreatedTime());
			command.setUpdatedTime(LocalDateTime.now());
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
			command.setUpdatedTime(LocalDateTime.now());
			return commandRepository.save(command);
		});
	}

}
