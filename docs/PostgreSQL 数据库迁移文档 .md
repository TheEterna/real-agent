# PostgreSQL 数据库迁移文档

## 迁移概述

本项目已从 MySQL 迁移到 PostgreSQL，主要变更包括：
1. 引入 PostgreSQL Schema 概念
2. 更新实体类的 `@Table` 注解
3. 更新 Repository 中的自定义 SQL 查询
4. 移除 MySQL 特有语法（如反引号）

## Schema 设计

### app_user Schema
用于存储用户相关数据

**表列表**：
- `app_user.users` - 用户表

### playground Schema
用于存储角色扮演相关数据

**表列表**：
- `playground.roleplay_roles` - 角色扮演角色表
- `playground.roleplay_sessions` - 角色扮演会话表
- `playground.roleplay_session_messages` - 角色扮演会话消息表

## 表名映射

| 原 MySQL 表名 | 新 PostgreSQL 表名 | 说明 |
|--------------|-------------------|------|
| `users` | `app_user.users` | 用户表 |
| `playground_roleplay_roles` | `playground.roleplay_roles` | 角色表 |
| `playground_roleplay_sessions` | `playground.roleplay_sessions` | 会话表 |
| `playground_roleplay_session_messages` | `playground.roleplay_session_messages` | 消息表 |

## 代码变更详情

### 1. 实体类 @Table 注解更新

#### User.java
```java
// 修改前
@Table("users")

// 修改后
@Table(name = "users", schema = "app_user")
```

#### PlaygroundRoleplayRole.java
```java
// 修改前
@Table("playground_roleplay_roles")

// 修改后
@Table(name = "roleplay_roles", schema = "playground")
```

#### PlaygroundRoleplaySession.java
```java
// 修改前
@Table("playground_roleplay_sessions")

// 修改后
@Table(name = "roleplay_sessions", schema = "playground")
```

#### PlaygroundRoleplaySessionMessage.java
```java
// 修改前
@Table("playground_roleplay_session_messages")

// 修改后
@Table(name = "roleplay_session_messages", schema = "playground")
```

### 2. Repository SQL 查询更新

#### UserRepository.java
```java
// 修改前
@Query("SELECT COUNT(*) > 0 FROM users WHERE external_id = :externalId")

// 修改后
@Query("SELECT COUNT(*) > 0 FROM app_user.users WHERE external_id = :externalId")
```

#### PlaygroundRoleplayRoleRepository.java
```java
// 修改前
@Query("SELECT * FROM `playground_roleplay_roles` WHERE status = 1 ORDER BY created_at DESC")
@Query("SELECT * FROM playground_roleplay_roles WHERE status = :status ORDER BY created_at DESC")

// 修改后
@Query("SELECT * FROM playground.roleplay_roles WHERE status = 1 ORDER BY created_at DESC")
@Query("SELECT * FROM playground.roleplay_roles WHERE status = :status ORDER BY created_at DESC")
```

**注意**：移除了 MySQL 反引号 `` ` ``

#### PlaygroundRoleplaySessionRepository.java
```java
// 修改前
@Query("SELECT COUNT(*) > 0 FROM playground_roleplay_sessions WHERE session_code = :sessionCode")
@Query("SELECT * FROM playground_roleplay_sessions WHERE user_id = :userId ORDER BY created_at DESC")
@Query("SELECT * FROM playground_roleplay_sessions WHERE user_id = :userId ORDER BY created_at ASC")
@Query("SELECT * FROM playground_roleplay_sessions WHERE user_id = :userId AND status = 1 ORDER BY created_at DESC")
@Query("SELECT * FROM playground_roleplay_sessions WHERE role_id = :roleId ORDER BY created_at DESC")

// 修改后
@Query("SELECT COUNT(*) > 0 FROM playground.roleplay_sessions WHERE session_code = :sessionCode")
@Query("SELECT * FROM playground.roleplay_sessions WHERE user_id = :userId ORDER BY created_at DESC")
@Query("SELECT * FROM playground.roleplay_sessions WHERE user_id = :userId ORDER BY created_at ASC")
@Query("SELECT * FROM playground.roleplay_sessions WHERE user_id = :userId AND status = 1 ORDER BY created_at DESC")
@Query("SELECT * FROM playground.roleplay_sessions WHERE role_id = :roleId ORDER BY created_at DESC")
```

#### PlaygroundRoleplaySessionMessageRepository.java
```java
// 修改前
@Query("SELECT * FROM playground_roleplay_session_messages WHERE session_id = :sessionId ORDER BY created_at")
@Query("SELECT * FROM playground_roleplay_session_messages WHERE session_id = :sessionId AND message_type = :messageType ORDER BY created_at")
@Query("SELECT COUNT(*) FROM playground_roleplay_session_messages WHERE session_id = :sessionId")
@Query("SELECT * FROM playground_roleplay_session_messages WHERE session_id = :sessionId ORDER BY created_at ASC LIMIT :limit OFFSET :offset")

// 修改后
@Query("SELECT * FROM playground.roleplay_session_messages WHERE session_id = :sessionId ORDER BY created_at")
@Query("SELECT * FROM playground.roleplay_session_messages WHERE session_id = :sessionId AND message_type = :messageType ORDER BY created_at")
@Query("SELECT COUNT(*) FROM playground.roleplay_session_messages WHERE session_id = :sessionId")
@Query("SELECT * FROM playground.roleplay_session_messages WHERE session_id = :sessionId ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
```

## 修改的文件列表

### 实体类（Entity）
1. `src/main/java/com/ai/agent/real/domain/entity/user/User.java`
2. `src/main/java/com/ai/agent/real/domain/entity/roleplay/PlaygroundRoleplayRole.java`
3. `src/main/java/com/ai/agent/real/domain/entity/roleplay/PlaygroundRoleplaySession.java`
4. `src/main/java/com/ai/agent/real/domain/entity/roleplay/PlaygroundRoleplaySessionMessage.java`

### 仓储接口（Repository）
1. `src/main/java/com/ai/agent/real/domain/repository/user/UserRepository.java`
2. `src/main/java/com/ai/agent/real/domain/repository/roleplay/PlaygroundRoleplayRoleRepository.java`
3. `src/main/java/com/ai/agent/real/domain/repository/roleplay/PlaygroundRoleplaySessionRepository.java`
4. `src/main/java/com/ai/agent/real/domain/repository/roleplay/PlaygroundRoleplaySessionMessageRepository.java`

## MySQL vs PostgreSQL 语法差异

### 1. 反引号
- **MySQL**: 使用反引号 `` ` `` 包裹表名和字段名
- **PostgreSQL**: 使用双引号 `"` 或不使用（推荐）

```sql
-- MySQL
SELECT * FROM `users` WHERE `external_id` = 'xxx'

-- PostgreSQL
SELECT * FROM users WHERE external_id = 'xxx'
-- 或
SELECT * FROM "users" WHERE "external_id" = 'xxx'
```

### 2. Schema 支持
- **MySQL**: 使用 Database 概念，表名直接引用
- **PostgreSQL**: 使用 Schema 概念，表名格式为 `schema.table`

```sql
-- MySQL
SELECT * FROM users

-- PostgreSQL
SELECT * FROM app_user.users
```

### 3. 自增主键
- **MySQL**: `AUTO_INCREMENT`
- **PostgreSQL**: `SERIAL` 或 `GENERATED ALWAYS AS IDENTITY`

### 4. 布尔类型
- **MySQL**: `TINYINT(1)` 或 `BOOLEAN`
- **PostgreSQL**: `BOOLEAN` (真正的布尔类型)

## 注意事项

### 1. Spring Data R2DBC 配置
确保在 `application.yml` 或 `application.properties` 中配置了正确的 PostgreSQL 连接信息：

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/database_name
    username: postgres
    password: your_password
```

### 2. Schema 权限
确保数据库用户对 `app_user` 和 `playground` schema 有足够的权限：

```sql
GRANT ALL PRIVILEGES ON SCHEMA app_user TO your_user;
GRANT ALL PRIVILEGES ON SCHEMA playground TO your_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA app_user TO your_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA playground TO your_user;
```

### 3. 序列（Sequence）权限
如果使用自增主键，需要授予序列权限：

```sql
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA app_user TO your_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA playground TO your_user;
```

### 4. 枚举类型映射
PostgreSQL 支持原生枚举类型，但 Spring Data R2DBC 会自动处理 Java 枚举到字符串的转换。

当前实现：
- `VoiceEnum` → 存储为字符串
- `MessageType` → 存储为字符串
- `MessageRole` → 存储为字符串

### 5. JSON 字段
如果表中有 JSON 字段，PostgreSQL 提供了 `JSON` 和 `JSONB` 类型：
- `JSON`: 存储原始文本
- `JSONB`: 二进制格式，支持索引和查询

建议使用 `JSONB` 类型以获得更好的性能。

## 测试验证

### 1. 单元测试
运行所有 Repository 测试，确保 CRUD 操作正常：

```bash
mvn test -Dtest=*Repository*
```

### 2. 集成测试
启动应用程序，验证以下功能：
- ✅ 用户创建和查询
- ✅ 角色列表查询
- ✅ 会话创建和查询
- ✅ 消息保存和查询

### 3. SQL 验证
在 PostgreSQL 中手动执行查询，验证表名和 Schema 是否正确：

```sql
-- 查询用户
SELECT * FROM app_user.users LIMIT 10;

-- 查询角色
SELECT * FROM playground.roleplay_roles WHERE status = 1;

-- 查询会话
SELECT * FROM playground.roleplay_sessions WHERE user_id = 1;

-- 查询消息
SELECT * FROM playground.roleplay_session_messages WHERE session_id = 1;
```

## 回滚方案

如果需要回滚到 MySQL，执行以下步骤：

1. 恢复实体类的 `@Table` 注解
2. 恢复 Repository 中的 SQL 查询
3. 更新数据库连接配置
4. 重新部署应用

建议在迁移前做好数据备份。

## 后续优化建议

1. **使用 Flyway 或 Liquibase** 进行数据库版本管理
2. **创建索引** 优化查询性能
3. **使用 JSONB** 替代字符串存储 JSON 数据
4. **配置连接池** 优化数据库连接管理
5. **启用查询日志** 便于调试和性能分析

## 相关文档

- [PostgreSQL 官方文档](https://www.postgresql.org/docs/)
- [Spring Data R2DBC 文档](https://spring.io/projects/spring-data-r2dbc)
- [R2DBC PostgreSQL Driver](https://github.com/pgjdbc/r2dbc-postgresql)
