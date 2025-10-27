package com.ai.agent.real.common.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import io.r2dbc.postgresql.codec.Json;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 终端命令配置实体
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Data
@Table(name = "terminal_commands", schema = "plugin")
public class TerminalCommand {

	@Id
	private String id;

	@Column("name")
	private String name;

	@Column("description")
	private String description;

	/**
	 * 参数列表（命令的操作目标，如文件路径、字符串等）
	 */
	@Column("arguments")
	private List<String> arguments;

	@Column("usage")
	private String usage;

	@Column("category")
	private String category; // system/ai/file/project/connection

	@Column("needs_backend")
	private Boolean needsBackend;

	/**
	 * 选项参数（键值对） TODO 后面统一规范，尽量使用 POJO
	 */
	@Column("options")
	private Json options; // JSON格式: [{name, type, required, ...}]

	@Column("handler")
	private String handler; // 处理器类名

	@Column("enabled")
	private Boolean enabled;

	@Column("created_time")
	private LocalDateTime createdTime;

	@Column("updated_time")
	private LocalDateTime updatedTime;

	// Constructors
	public TerminalCommand() {
	}

}
