-- ===================================================================
-- Real Agent 终端命令系统数据库初始化脚本 (PostgreSQL)
-- 版本: 1.0.0
-- 作者: Real Agent Team
-- 日期: 2025-01-23
-- ===================================================================

-- ===================================================================
-- 0. 创建 plugin schema (如果不存在)
-- ===================================================================
CREATE SCHEMA IF NOT EXISTS plugin;

-- ===================================================================
-- 1. 创建终端命令配置表
-- ===================================================================
CREATE TABLE IF NOT EXISTS plugin.terminal_commands (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    aliases JSONB,
    description TEXT NOT NULL,
    usage VARCHAR(256) NOT NULL,
    examples JSONB,
    category VARCHAR(32) NOT NULL CHECK (category IN ('system', 'ai', 'file', 'project', 'connection')),
    permission VARCHAR(32) NOT NULL CHECK (permission IN ('public', 'user', 'admin', 'system')),
    needs_backend BOOLEAN NOT NULL DEFAULT FALSE,
    needs_connection BOOLEAN NOT NULL DEFAULT FALSE,
    parameters JSONB NOT NULL,
    handler VARCHAR(128),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    hidden BOOLEAN DEFAULT FALSE,
    deprecated BOOLEAN DEFAULT FALSE,
    version VARCHAR(32),
    tags JSONB,
    related_commands JSONB,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_terminal_commands_name ON plugin.terminal_commands(name);
CREATE INDEX IF NOT EXISTS idx_terminal_commands_category ON plugin.terminal_commands(category);
CREATE INDEX IF NOT EXISTS idx_terminal_commands_enabled ON plugin.terminal_commands(enabled);
CREATE INDEX IF NOT EXISTS idx_terminal_commands_permission ON plugin.terminal_commands(permission);

-- 创建注释
COMMENT ON TABLE plugin.terminal_commands IS '终端命令配置表';
COMMENT ON COLUMN plugin.terminal_commands.id IS '命令ID';
COMMENT ON COLUMN plugin.terminal_commands.name IS '命令名称';
COMMENT ON COLUMN plugin.terminal_commands.aliases IS '别名列表';
COMMENT ON COLUMN plugin.terminal_commands.description IS '命令描述';
COMMENT ON COLUMN plugin.terminal_commands.usage IS '使用方法';
COMMENT ON COLUMN plugin.terminal_commands.examples IS '使用示例';
COMMENT ON COLUMN plugin.terminal_commands.category IS '命令类别';
COMMENT ON COLUMN plugin.terminal_commands.permission IS '权限级别';
COMMENT ON COLUMN plugin.terminal_commands.needs_backend IS '是否需要后端处理';
COMMENT ON COLUMN plugin.terminal_commands.needs_connection IS '是否需要服务器连接';
COMMENT ON COLUMN plugin.terminal_commands.parameters IS '参数定义';
COMMENT ON COLUMN plugin.terminal_commands.handler IS '处理器类名';
COMMENT ON COLUMN plugin.terminal_commands.enabled IS '是否启用';
COMMENT ON COLUMN plugin.terminal_commands.hidden IS '是否隐藏';
COMMENT ON COLUMN plugin.terminal_commands.deprecated IS '是否已弃用';
COMMENT ON COLUMN plugin.terminal_commands.version IS '版本号';
COMMENT ON COLUMN plugin.terminal_commands.tags IS '标签';
COMMENT ON COLUMN plugin.terminal_commands.related_commands IS '相关命令';
COMMENT ON COLUMN plugin.terminal_commands.metadata IS '扩展元数据';
COMMENT ON COLUMN plugin.terminal_commands.created_at IS '创建时间';
COMMENT ON COLUMN plugin.terminal_commands.updated_at IS '更新时间';

-- ===================================================================
-- 2. 创建终端命令历史表
-- ===================================================================
CREATE TABLE IF NOT EXISTS plugin.terminal_history (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    command_name VARCHAR(64) NOT NULL,
    original_command TEXT NOT NULL,
    parsed_command JSONB NOT NULL,
    context JSONB,
    result JSONB,
    execution_time INTEGER,
    exit_code INTEGER,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_terminal_history_session ON plugin.terminal_history(session_id);
CREATE INDEX IF NOT EXISTS idx_terminal_history_user ON plugin.terminal_history(user_id);
CREATE INDEX IF NOT EXISTS idx_terminal_history_command ON plugin.terminal_history(command_name);
CREATE INDEX IF NOT EXISTS idx_terminal_history_timestamp ON plugin.terminal_history(timestamp);

-- 创建注释
COMMENT ON TABLE plugin.terminal_history IS '终端命令执行历史表';
COMMENT ON COLUMN plugin.terminal_history.id IS '历史记录ID';
COMMENT ON COLUMN plugin.terminal_history.session_id IS '会话ID';
COMMENT ON COLUMN plugin.terminal_history.user_id IS '用户ID';
COMMENT ON COLUMN plugin.terminal_history.command_name IS '命令名称';
COMMENT ON COLUMN plugin.terminal_history.original_command IS '原始命令';
COMMENT ON COLUMN plugin.terminal_history.parsed_command IS '解析后的命令';
COMMENT ON COLUMN plugin.terminal_history.context IS '执行上下文';
COMMENT ON COLUMN plugin.terminal_history.result IS '执行结果';
COMMENT ON COLUMN plugin.terminal_history.execution_time IS '执行时间(毫秒)';
COMMENT ON COLUMN plugin.terminal_history.exit_code IS '退出码';
COMMENT ON COLUMN plugin.terminal_history.error_message IS '错误信息';
COMMENT ON COLUMN plugin.terminal_history.timestamp IS '执行时间';

-- ===================================================================
-- 3. 创建终端会话表
-- ===================================================================
CREATE TABLE IF NOT EXISTS plugin.terminal_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'closed', 'timeout')),
    context JSONB,
    metadata JSONB
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_terminal_sessions_user ON plugin.terminal_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_terminal_sessions_status ON plugin.terminal_sessions(status);
CREATE INDEX IF NOT EXISTS idx_terminal_sessions_last_activity ON plugin.terminal_sessions(last_activity);

-- 创建注释
COMMENT ON TABLE plugin.terminal_sessions IS '终端会话表';
COMMENT ON COLUMN plugin.terminal_sessions.session_id IS '会话ID';
COMMENT ON COLUMN plugin.terminal_sessions.user_id IS '用户ID';
COMMENT ON COLUMN plugin.terminal_sessions.start_time IS '开始时间';
COMMENT ON COLUMN plugin.terminal_sessions.last_activity IS '最后活动时间';
COMMENT ON COLUMN plugin.terminal_sessions.status IS '会话状态';
COMMENT ON COLUMN plugin.terminal_sessions.context IS '会话上下文';
COMMENT ON COLUMN plugin.terminal_sessions.metadata IS '扩展元数据';

-- ===================================================================
-- 4. 创建命令统计表
-- ===================================================================
CREATE TABLE IF NOT EXISTS plugin.terminal_command_stats (
    id BIGSERIAL PRIMARY KEY,
    command_name VARCHAR(64) NOT NULL,
    date DATE NOT NULL,
    execution_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    avg_execution_time FLOAT,
    total_execution_time BIGINT,
    UNIQUE (command_name, date)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_terminal_command_stats_command ON plugin.terminal_command_stats(command_name);
CREATE INDEX IF NOT EXISTS idx_terminal_command_stats_date ON plugin.terminal_command_stats(date);

-- 创建注释
COMMENT ON TABLE plugin.terminal_command_stats IS '命令统计表';
COMMENT ON COLUMN plugin.terminal_command_stats.id IS '统计ID';
COMMENT ON COLUMN plugin.terminal_command_stats.command_name IS '命令名称';
COMMENT ON COLUMN plugin.terminal_command_stats.date IS '统计日期';
COMMENT ON COLUMN plugin.terminal_command_stats.execution_count IS '执行次数';
COMMENT ON COLUMN plugin.terminal_command_stats.success_count IS '成功次数';
COMMENT ON COLUMN plugin.terminal_command_stats.error_count IS '失败次数';
COMMENT ON COLUMN plugin.terminal_command_stats.avg_execution_time IS '平均执行时间(毫秒)';
COMMENT ON COLUMN plugin.terminal_command_stats.total_execution_time IS '总执行时间(毫秒)';

-- ===================================================================
-- 5. 插入默认命令配置
-- ===================================================================

-- 5.1 系统控制命令
INSERT INTO plugin.terminal_commands (id, name, aliases, description, usage, examples, category, permission, needs_backend, needs_connection, parameters, handler, enabled, version) VALUES
('cmd-help', 'help', '["h", "?"]'::jsonb, '显示帮助信息', 'help [command]', '["help", "help connect"]'::jsonb, 'system', 'public', FALSE, FALSE, '[{"name":"command","type":"string","required":false,"description":"要查看帮助的命令名称"}]'::jsonb, 'HelpCommandHandler', TRUE, '1.0.0'),
('cmd-clear', 'clear', '["cls"]'::jsonb, '清空终端屏幕', 'clear', '["clear", "cls"]'::jsonb, 'system', 'public', FALSE, FALSE, '[]'::jsonb, 'ClearCommandHandler', TRUE, '1.0.0'),
('cmd-exit', 'exit', '["quit", "q"]'::jsonb, '退出终端', 'exit', '["exit", "quit"]'::jsonb, 'system', 'public', FALSE, FALSE, '[]'::jsonb, 'ExitCommandHandler', TRUE, '1.0.0'),
('cmd-history', 'history', '[]'::jsonb, '显示命令历史', 'history [-n number]', '["history", "history -n 10"]'::jsonb, 'system', 'public', FALSE, FALSE, '[{"name":"number","type":"number","required":false,"shortFlag":"-n","longFlag":"--number","description":"显示最近的N条命令","defaultValue":20}]'::jsonb, 'HistoryCommandHandler', TRUE, '1.0.0')
ON CONFLICT (id) DO NOTHING;

-- 5.2 AI交互命令
INSERT INTO plugin.terminal_commands (id, name, aliases, description, usage, examples, category, permission, needs_backend, needs_connection, parameters, handler, enabled, version) VALUES
('cmd-chat', 'chat', '["ask"]'::jsonb, '与AI助手对话', 'chat <message>', '["chat 你好", "chat 帮我写代码"]'::jsonb, 'ai', 'user', TRUE, FALSE, '[{"name":"message","type":"string","required":true,"description":"要发送给AI的消息"}]'::jsonb, 'ChatCommandHandler', TRUE, '1.0.0'),
('cmd-plan', 'plan', '[]'::jsonb, '让AI制定计划', 'plan <task> [--detail]', '["plan 学习Vue3", "plan 开发网站 --detail"]'::jsonb, 'ai', 'user', TRUE, FALSE, '[{"name":"task","type":"string","required":true,"description":"要制定计划的任务"},{"name":"detail","type":"boolean","required":false,"longFlag":"--detail","description":"生成详细计划","defaultValue":false}]'::jsonb, 'PlanCommandHandler', TRUE, '1.0.0')
ON CONFLICT (id) DO NOTHING;

-- 5.3 连接管理命令
INSERT INTO plugin.terminal_commands (id, name, aliases, description, usage, examples, category, permission, needs_backend, needs_connection, parameters, handler, enabled, version) VALUES
('cmd-connect', 'connect', '[]'::jsonb, '连接到远程服务器', 'connect <server> [--port port] [--user username]', '["connect example.com", "connect 192.168.1.100 --port 2222 --user admin"]'::jsonb, 'connection', 'user', TRUE, FALSE, '[{"name":"server","type":"string","required":true,"description":"服务器地址或名称"},{"name":"port","type":"number","required":false,"longFlag":"--port","description":"SSH端口号","defaultValue":22},{"name":"user","type":"string","required":false,"longFlag":"--user","description":"用户名"}]'::jsonb, 'ConnectCommandHandler', TRUE, '1.0.0'),
('cmd-disconnect', 'disconnect', '[]'::jsonb, '断开服务器连接', 'disconnect', '["disconnect"]'::jsonb, 'connection', 'user', TRUE, TRUE, '[]'::jsonb, 'DisconnectCommandHandler', TRUE, '1.0.0'),
('cmd-reconnect', 'reconnect', '[]'::jsonb, '重新连接服务器', 'reconnect', '["reconnect"]'::jsonb, 'connection', 'user', TRUE, FALSE, '[]'::jsonb, 'ReconnectCommandHandler', TRUE, '1.0.0')
ON CONFLICT (id) DO NOTHING;

-- 5.4 文件操作命令
INSERT INTO plugin.terminal_commands (id, name, aliases, description, usage, examples, category, permission, needs_backend, needs_connection, parameters, handler, enabled, version) VALUES
('cmd-ls', 'ls', '["dir"]'::jsonb, '列出目录内容', 'ls [path] [-l] [-a]', '["ls", "ls /home", "ls -la"]'::jsonb, 'file', 'user', TRUE, TRUE, '[{"name":"path","type":"path","required":false,"description":"要列出的目录路径","defaultValue":"."},{"name":"long","type":"boolean","required":false,"shortFlag":"-l","description":"详细格式显示","defaultValue":false},{"name":"all","type":"boolean","required":false,"shortFlag":"-a","description":"显示隐藏文件","defaultValue":false}]'::jsonb, 'ListCommandHandler', TRUE, '1.0.0'),
('cmd-pwd', 'pwd', '[]'::jsonb, '显示当前工作目录', 'pwd', '["pwd"]'::jsonb, 'file', 'user', TRUE, TRUE, '[]'::jsonb, 'PwdCommandHandler', TRUE, '1.0.0'),
('cmd-cat', 'cat', '[]'::jsonb, '显示文件内容', 'cat <file>', '["cat readme.txt", "cat /etc/hosts"]'::jsonb, 'file', 'user', TRUE, TRUE, '[{"name":"file","type":"path","required":true,"description":"要查看的文件路径"}]'::jsonb, 'CatCommandHandler', TRUE, '1.0.0')
ON CONFLICT (id) DO NOTHING;

-- ===================================================================
-- 6. 创建更新时间触发器
-- ===================================================================
CREATE OR REPLACE FUNCTION plugin.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_terminal_commands_updated_at BEFORE UPDATE ON plugin.terminal_commands
FOR EACH ROW EXECUTE FUNCTION plugin.update_updated_at_column();

-- ===================================================================
-- 完成
-- ===================================================================
SELECT '✅ 终端命令系统数据库初始化完成 (PostgreSQL)' AS status;
