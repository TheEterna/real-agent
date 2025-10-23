package com.ai.agent.real.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 终端命令配置实体
 *
 * @author Real Agent Team
 * @since 2025-01-23
 */
@Table("terminal_commands")
public class TerminalCommand {

	@Id
	private String id;

	@Column("name")
	private String name;

	@Column("aliases")
	private String aliases; // JSON格式: ["ask", "chat"]

	@Column("description")
	private String description;

	@Column("usage")
	private String usage;

	@Column("examples")
	private String examples; // JSON格式: ["example1", "example2"]

	@Column("category")
	private String category; // system/ai/file/project/connection

	@Column("permission")
	private String permission; // public/user/admin/system

	@Column("needs_backend")
	private Boolean needsBackend;

	@Column("needs_connection")
	private Boolean needsConnection;

	@Column("parameters")
	private String parameters; // JSON格式: [{name, type, required, ...}]

	@Column("handler")
	private String handler; // 处理器类名

	@Column("enabled")
	private Boolean enabled;

	@Column("hidden")
	private Boolean hidden;

	@Column("deprecated")
	private Boolean deprecated;

	@Column("version")
	private String version;

	@Column("tags")
	private String tags; // JSON格式: ["popular", "ai"]

	@Column("related_commands")
	private String relatedCommands; // JSON格式: ["plan", "analyze"]

	@Column("metadata")
	private String metadata; // JSON格式，扩展字段

	@Column("created_at")
	private LocalDateTime createdAt;

	@Column("updated_at")
	private LocalDateTime updatedAt;

	// Constructors
	public TerminalCommand() {
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAliases() {
		return aliases;
	}

	public void setAliases(String aliases) {
		this.aliases = aliases;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUsage() {
		return usage;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}

	public String getExamples() {
		return examples;
	}

	public void setExamples(String examples) {
		this.examples = examples;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public Boolean getNeedsBackend() {
		return needsBackend;
	}

	public void setNeedsBackend(Boolean needsBackend) {
		this.needsBackend = needsBackend;
	}

	public Boolean getNeedsConnection() {
		return needsConnection;
	}

	public void setNeedsConnection(Boolean needsConnection) {
		this.needsConnection = needsConnection;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getHandler() {
		return handler;
	}

	public void setHandler(String handler) {
		this.handler = handler;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public Boolean getDeprecated() {
		return deprecated;
	}

	public void setDeprecated(Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getRelatedCommands() {
		return relatedCommands;
	}

	public void setRelatedCommands(String relatedCommands) {
		this.relatedCommands = relatedCommands;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}
