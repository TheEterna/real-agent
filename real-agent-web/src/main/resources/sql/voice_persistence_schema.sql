-- ============================================================================
-- Voice Call Persistence Schema (MySQL InnoDB, utf8mb4)
-- 语音通话持久化数据库设计
-- 
-- 设计复述：
-- 1) 所有"文字内容"统一存储在 app_message 表（挂载到 app_session）。
-- 2) 语音或其他自定义内容，存储在 voice_message 表（挂载到 voice_session）。
--    当前阶段"语音内容不落库"，仅保留该表结构作为将来扩展的占位。
-- 3) 语音通话期间，会为同一原始会话（app_session）创建一个 voice_session 用于语音侧数据挂载。
-- ============================================================================

-- 应用会话表
-- 存储用户与系统交互的会话基本信息
CREATE TABLE IF NOT EXISTS app_session (
  id VARCHAR(36) NOT NULL COMMENT '会话唯一标识（UUID）',
  role_id VARCHAR(64) NULL COMMENT '角色ID，标识当前会话所属的用户角色',
  created_at DATETIME(6) NOT NULL COMMENT '会话创建时间（精确到微秒）',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用会话表';

-- --------------------------------------------------------------------------
-- 文本消息表：统一挂载到 app_session
-- direction: USER / AI （消息方向：用户发送 / AI回复）
-- type: TEXT（消息类型：文本，预留将来扩展其他类型）
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息唯一标识（自增ID）',
  app_session_id VARCHAR(36) NOT NULL COMMENT '关联的应用会话ID',
  direction VARCHAR(8) NOT NULL COMMENT '消息方向（USER：用户发送，AI：AI回复）',
  type VARCHAR(16) NOT NULL COMMENT '消息类型（TEXT：文本消息，预留其他类型）',
  content TEXT NULL COMMENT '消息内容（文本内容）',
  created_at DATETIME(6) NOT NULL COMMENT '消息创建时间（精确到微秒）',
  PRIMARY KEY (id),
  KEY idx_app_message_session (app_session_id),
  CONSTRAINT fk_app_message_session FOREIGN KEY (app_session_id)
    REFERENCES app_session(id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文本消息表';

-- 语音会话表
-- 存储语音通话的会话信息，与应用会话一一对应
CREATE TABLE IF NOT EXISTS voice_session (
  id VARCHAR(36) NOT NULL COMMENT '语音会话唯一标识（UUID）',
  app_session_id VARCHAR(36) NOT NULL COMMENT '关联的应用会话ID',
  server_session_id VARCHAR(64) NULL COMMENT '服务器端会话ID，用于对接语音服务',
  created_at DATETIME(6) NOT NULL COMMENT '语音会话创建时间（精确到微秒）',
  PRIMARY KEY (id),
  KEY idx_voice_session_app (app_session_id),
  CONSTRAINT fk_voice_session_app FOREIGN KEY (app_session_id)
    REFERENCES app_session(id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音会话表';

-- --------------------------------------------------------------------------
-- 语音/自定义内容表：挂载到 voice_session
-- 当前阶段不落库，仅保留表结构用于将来扩展
-- direction/type 字段保留，便于未来扩展（例如 AUDIO/IMAGE 等）
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS voice_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '语音消息唯一标识（自增ID）',
  voice_session_id VARCHAR(36) NOT NULL COMMENT '关联的语音会话ID',
  direction VARCHAR(8) NOT NULL COMMENT '消息方向（USER：用户发送，AI：AI回复）',
  type VARCHAR(16) NOT NULL COMMENT '消息类型（预留：AUDIO：音频，IMAGE：图片等）',
  content TEXT NULL COMMENT '消息内容（预留字段，当前未使用）',
  created_at DATETIME(6) NOT NULL COMMENT '消息创建时间（精确到微秒）',
  PRIMARY KEY (id),
  KEY idx_voice_message_session (voice_session_id),
  CONSTRAINT fk_voice_message_session FOREIGN KEY (voice_session_id)
    REFERENCES voice_session(id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音消息表（预留扩展）';