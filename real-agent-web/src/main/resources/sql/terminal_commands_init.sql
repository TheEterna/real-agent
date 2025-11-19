-- ===================================================================
-- Real Agent 终端命令系统数据库初始化脚本
-- 版本: 1.0.0
-- 作者: Real Agent Team
-- 日期: 2025-01-23
-- ===================================================================

-- ===================================================================
-- 1. 创建终端命令配置表
-- ===================================================================
use `real-agent`;
CREATE TABLE IF NOT EXISTS terminal_commands (
    id VARCHAR(64) PRIMARY KEY COMMENT '命令ID',
    name VARCHAR(64) NOT NULL UNIQUE COMMENT '命令名称',
    aliases JSON COMMENT '别名列表',
    description TEXT NOT NULL COMMENT '命令描述',
    `usage` VARCHAR(256) NOT NULL COMMENT '使用方法',
    examples JSON COMMENT '使用示例',
    category ENUM('system', 'ai', 'file', 'project', 'connection') NOT NULL COMMENT '命令类别',
    permission ENUM('public', 'user', 'admin', 'system') NOT NULL COMMENT '权限级别',
    needs_backend BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否需要后端处理',
    needs_connection BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否需要服务器连接',
    parameters JSON NOT NULL COMMENT '参数定义',
    handler VARCHAR(128) COMMENT '处理器类名',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    hidden BOOLEAN DEFAULT FALSE COMMENT '是否隐藏',
    deprecated BOOLEAN DEFAULT FALSE COMMENT '是否已弃用',
    version VARCHAR(32) COMMENT '版本号',
    tags JSON COMMENT '标签',
    related_commands JSON COMMENT '相关命令',
    metadata JSON COMMENT '扩展元数据',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_name (name),
    INDEX idx_category (category),
    INDEX idx_enabled (enabled),
    INDEX idx_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='终端命令配置表';

-- ===================================================================
-- 2. 创建终端命令历史表
-- ===================================================================
CREATE TABLE IF NOT EXISTS terminal_history (
    id VARCHAR(64) PRIMARY KEY COMMENT '历史记录ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    command_name VARCHAR(64) NOT NULL COMMENT '命令名称',
    original_command TEXT NOT NULL COMMENT '原始命令',
    parsed_command JSON NOT NULL COMMENT '解析后的命令',
    context JSON COMMENT '执行上下文',
    result JSON COMMENT '执行结果',
    execution_time INT COMMENT '执行时间(毫秒)',
    exit_code INT COMMENT '退出码',
    error_message TEXT COMMENT '错误信息',
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    INDEX idx_session (session_id),
    INDEX idx_user (user_id),
    INDEX idx_command (command_name),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (command_name) REFERENCES terminal_commands(name) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='终端命令执行历史表';

-- ===================================================================
-- 3. 创建终端会话表
-- ===================================================================
CREATE TABLE IF NOT EXISTS terminal_sessions (
    session_id VARCHAR(64) PRIMARY KEY COMMENT '会话ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活动时间',
    status ENUM('active', 'closed', 'timeout') NOT NULL DEFAULT 'active' COMMENT '会话状态',
    context JSON COMMENT '会话上下文',
    metadata JSON COMMENT '扩展元数据',
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_last_activity (last_activity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='终端会话表';

-- ===================================================================
-- 4. 创建命令统计表
-- ===================================================================
CREATE TABLE IF NOT EXISTS terminal_command_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
    command_name VARCHAR(64) NOT NULL COMMENT '命令名称',
    date DATE NOT NULL COMMENT '统计日期',
    execution_count INT NOT NULL DEFAULT 0 COMMENT '执行次数',
    success_count INT NOT NULL DEFAULT 0 COMMENT '成功次数',
    error_count INT NOT NULL DEFAULT 0 COMMENT '失败次数',
    avg_execution_time FLOAT COMMENT '平均执行时间(毫秒)',
    total_execution_time BIGINT COMMENT '总执行时间(毫秒)',
    UNIQUE KEY uk_command_date (command_name, date),
    INDEX idx_command (command_name),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='命令统计表';

-- ===================================================================
-- 5. 插入默认命令配置
-- ===================================================================

-- 5.1 系统控制命令
INSERT INTO terminal_commands (id, name, aliases, description, `usage`, examples, category, permission, needs_backend, needs_connection, parameters, handler, enabled, version) VALUES
('cmd-help', 'help', '["h", "?"]', '显示帮助信息', 'help [command]', '["help", "help connect"]', 'system', 'public', FALSE, FALSE, '[{"name":"command","type":"string","required":false,"description":"要查看帮助的命令名称"}]', 'HelpCommandHandler', TRUE, '1.0.0'),
('cmd-clear', 'clear', '["cls"]', '清空终端屏幕', 'clear', '["clear", "cls"]', 'system', 'public', FALSE, FALSE, '[]', 'ClearCommandHandler', TRUE, '1.0.0'),
('cmd-exit', 'exit', '["quit", "q"]', '退出终端', 'exit', '["exit", "quit"]', 'system', 'public', FALSE, FALSE, '[]', 'ExitCommandHandler', TRUE, '1.0.0'),
('cmd-history', 'history', '[]', '显示命令历史', 'history [-n number]', '["history", "history -n 10"]', 'system', 'public', FALSE, FALSE, '[{"name":"number","type":"number","required":false,"shortFlag":"-n","longFlag":"--number","description":"显示最近的N条命令","defaultValue":20}]', 'HistoryCommandHandler', TRUE, '1.0.0');

-- 5.2 AI交互命令
INSERT INTO terminal_commands (id, name, aliases, description, `usage`, examples, category, permission, needs_backend, needs_connection, parameters, handler, enabled, version) VALUES
('cmd-chat', 'chat', '["ask"]', '与AI助手对话', 'chat <message>', '["chat 你好", "chat 帮我写代码"]', 'ai', 'user', TRUE, FALSE, '[{"name":"message","type":"string","required":true,"description":"要发送给AI的消息"}]', 'ChatCommandHandler', TRUE, '1.0.0'),
('cmd-plan', 'plan', '[]', '让AI制定计划', 'plan <task> [--detail]', '["plan 学习Vue3", "plan 开发网站 --detail"]', 'ai', 'user', TRUE, FALSE, '[{"name":"task","type":"string","required":true,"description":"要制定计划的任务"},{"name":"detail","type":"boolean","required":false,"longFlag":"--detail","description":"生成详细计划","defaultValue":false}]', 'PlanCommandHandler', TRUE, '1.0.0');

-- 5.3 连接管理命令
INSERT INTO terminal_commands (id, name, aliases, description, `usage`, examples, category, permission, needs_backend, needs_connection, parameters, handler, enabled, version) VALUES
('cmd-connect', 'connect', '[]', '连接到远程服务器', 'connect <server> [--port port] [--user username]', '["connect example.com", "connect 192.168.1.100 --port 2222 --user admin"]', 'connection', 'user', TRUE, FALSE, '[{"name":"server","type":"string","required":true,"description":"服务器地址或名称"},{"name":"port","type":"number","required":false,"longFlag":"--port","description":"SSH端口号","defaultValue":22},{"name":"user","type":"string","required":false,"longFlag":"--user","description":"用户名"}]', 'ConnectCommandHandler', TRUE, '1.0.0'),
('cmd-disconnect', 'disconnect', '[]', '断开服务器连接', 'disconnect', '["disconnect"]', 'connection', 'user', TRUE, TRUE, '[]', 'DisconnectCommandHandler', TRUE, '1.0.0');

-- 5.4 文件操作命令
INSERT INTO terminal_commands (id, name, aliases, description, `usage`, examples, category, permission, needs_backend, needs_connection, parameters, handler, enabled, version) VALUES
('cmd-ls', 'ls', '["dir"]', '列出目录内容', 'ls [path] [-l] [-a]', '["ls", "ls /home", "ls -la"]', 'file', 'user', TRUE, TRUE, '[{"name":"path","type":"path","required":false,"description":"要列出的目录路径","defaultValue":"."},{"name":"long","type":"boolean","required":false,"shortFlag":"-l","description":"详细格式显示","defaultValue":false},{"name":"all","type":"boolean","required":false,"shortFlag":"-a","description":"显示隐藏文件","defaultValue":false}]', 'ListCommandHandler', TRUE, '1.0.0'),
('cmd-pwd', 'pwd', '[]', '显示当前工作目录', 'pwd', '["pwd"]', 'file', 'user', TRUE, TRUE, '[]', 'PwdCommandHandler', TRUE, '1.0.0'),
('cmd-cat', 'cat', '[]', '显示文件内容', 'cat <file>', '["cat readme.txt", "cat /etc/hosts"]', 'file', 'user', TRUE, TRUE, '[{"name":"file","type":"path","required":true,"description":"要查看的文件路径"}]', 'CatCommandHandler', TRUE, '1.0.0');

-- ===================================================================
-- 完成
-- ===================================================================
SELECT '✅ 终端命令系统数据库初始化完成' AS Status;
