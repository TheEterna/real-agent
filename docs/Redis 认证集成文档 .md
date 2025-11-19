# Redis 认证集成文档

## 一、概述

本文档说明如何在项目中集成 Redis 进行用户认证和会话管理。

### 技术栈
- **Redis**: 缓存和会话存储
- **JWT**: 无状态令牌认证
- **BCrypt**: 密码加密
- **WebFlux**: 响应式 Web 框架
- **PostgreSQL**: 用户数据持久化

---

## 二、架构设计

### 认证流程

```
┌─────────────┐
│   用户登录   │
└──────┬──────┘
       ↓
┌──────────────────────────┐
│  验证用户名密码（PostgreSQL）│
└──────┬───────────────────┘
       ↓
┌──────────────────────────┐
│  生成 JWT Token          │
└──────┬───────────────────┘
       ↓
┌──────────────────────────┐
│  存储 Token 到 Redis      │
│  - Token → UserContext   │
│  - UserId → Token        │
└──────┬───────────────────┘
       ↓
┌──────────────────────────┐
│  返回 Token 给前端        │
└──────────────────────────┘
```

### 请求认证流程

```
┌─────────────────────┐
│  HTTP 请求（带 Token）│
└──────┬──────────────┘
       ↓
┌──────────────────────────┐
│  AuthenticationFilter    │
└──────┬───────────────────┘
       ↓
┌──────────────────────────┐
│  从 Redis 验证 Token      │
└──────┬───────────────────┘
       ↓
┌──────────────────────────┐
│  注入用户上下文到 Reactor  │
└──────┬───────────────────┘
       ↓
┌──────────────────────────┐
│  业务逻辑处理             │
└──────────────────────────┘
```

---

## 三、已创建的文件

### 1. 通用层（real-agent-common）

#### Redis 服务
- **`redis/RedisService.java`** - Redis 响应式操作封装
  - 提供 get/set/delete/expire 等基础操作
  - 支持泛型类型转换
  - 完全响应式

#### 认证工具类
- **`auth/JwtUtil.java`** - JWT 令牌工具
  - 生成访问令牌（Access Token）
  - 生成刷新令牌（Refresh Token）
  - 验证和解析令牌
  
- **`auth/UserContext.java`** - 用户上下文对象
  - 存储用户基本信息
  - 支持扩展属性
  
- **`auth/UserContextHolder.java`** - 用户上下文持有者
  - 响应式获取当前用户
  - 与 Reactor Context 集成
  
- **`auth/PasswordUtil.java`** - 密码加密工具
  - BCrypt 加密
  - 密码验证

### 2. 应用层（real-agent-application）

#### 认证服务
- **`service/auth/TokenService.java`** - Token 管理服务
  - 生成 Token 对
  - 验证 Token
  - 刷新 Token
  - Token 黑名单管理
  
- **`service/auth/AuthService.java`** - 认证服务
  - 用户登录
  - 用户注册
  - 刷新令牌
  - 登出

### 3. Web 层（real-agent-web）

#### 配置类
- **`config/RedisConfig.java`** - Redis 配置
  - ReactiveRedisTemplate Bean
  - JSON 序列化配置

#### 过滤器
- **`filter/AuthenticationFilter.java`** - 认证过滤器
  - 拦截所有请求
  - 提取和验证 Token
  - 注入用户上下文

#### 控制器
- **`controller/auth/AuthController.java`** - 认证控制器
  - POST /api/auth/login - 登录
  - POST /api/auth/register - 注册
  - POST /api/auth/refresh - 刷新令牌
  - POST /api/auth/logout - 登出
  - GET /api/auth/me - 获取当前用户信息

### 4. 领域层（real-agent-domain）

#### 实体更新
- **`entity/user/User.java`** - 用户实体（已更新）
  - 添加 `passwordHash` 字段
  - 添加 `status` 字段

---

## 四、配置说明

### 1. Redis 配置（application-local.yml）

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:           # 如果 Redis 有密码，填写在这里
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

### 2. JWT 配置（application-local.yml）

```yaml
jwt:
  secret: real-agent-jwt-secret-key-for-authentication-must-be-at-least-256-bits
  access-token-expiration: 7200    # 2 小时
  refresh-token-expiration: 604800  # 7 天
```

### 3. PostgreSQL 配置（application-local.yml）

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/real-agent
    username: postgres
    password: your_password
```

---

## 五、数据库迁移

### 执行 SQL 脚本

```sql
-- 1. 添加密码哈希字段
ALTER TABLE app_user.users 
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- 2. 添加状态字段
ALTER TABLE app_user.users 
ADD COLUMN IF NOT EXISTS status INTEGER DEFAULT 1;

-- 3. 创建索引
CREATE INDEX IF NOT EXISTS idx_users_status ON app_user.users(status);
```

详细 SQL 见：`docs/SQL_USER_TABLE_MIGRATION.sql`

---

## 六、Redis 数据结构

### 1. Token 存储

**Key**: `auth:token:{token}`  
**Value**: `UserContext` 对象（JSON）  
**TTL**: 
- Access Token: 2 小时
- Refresh Token: 7 天

```json
{
  "userId": 123,
  "externalId": "user_001",
  "nickname": "张三",
  "avatarUrl": "https://..."
}
```

### 2. 用户 Token 映射

**Key**: `auth:user:token:{userId}`  
**Value**: 最新的 Access Token  
**TTL**: 2 小时

用途：单点登录控制，一个用户只能有一个有效的 Token

### 3. Token 黑名单

**Key**: `auth:blacklist:{token}`  
**Value**: `true`  
**TTL**: Token 剩余有效时间

用途：登出时将 Token 加入黑名单

---

## 七、API 接口

### 1. 登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "externalId": "test_user",
  "password": "123456"
}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200,
    "user": {
      "userId": 1,
      "externalId": "test_user",
      "nickname": "测试用户",
      "avatarUrl": "https://..."
    }
  }
}
```

### 2. 注册

```http
POST /api/auth/register
Content-Type: application/json

{
  "externalId": "new_user",
  "password": "123456",
  "nickname": "新用户",
  "avatarUrl": "https://..."
}
```

### 3. 刷新 Token

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### 4. 登出

```http
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 5. 获取当前用户信息

```http
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

---

## 八、使用示例

### 1. 在业务代码中获取当前用户

```java
@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    
    @GetMapping
    public Flux<SessionDTO> getUserSessions() {
        // 获取当前用户 ID
        return UserContextHolder.getUserId()
            .flatMapMany(userId -> sessionService.findByUserId(userId))
            .switchIfEmpty(Flux.error(new UnauthorizedException("请先登录")));
    }
    
    @GetMapping("/public")
    public Flux<SessionDTO> getPublicSessions() {
        // 可选认证：支持匿名和登录用户
        return UserContextHolder.getUserId()
            .flatMapMany(userId -> sessionService.findByUserId(userId))
            .switchIfEmpty(sessionService.findPublicSessions());
    }
}
```

### 2. 要求用户必须登录

```java
@GetMapping("/private")
public Mono<UserDTO> getPrivateData() {
    return UserContextHolder.requireAuthenticated()
        .map(user -> new UserDTO(user.getUserId(), user.getNickname()));
}
```

### 3. 判断用户是否已认证

```java
@GetMapping("/status")
public Mono<Map<String, Object>> getStatus() {
    return UserContextHolder.isAuthenticated()
        .map(authenticated -> Map.of("authenticated", authenticated));
}
```

---

## 九、部署步骤

### 1. 启动 Redis

```bash
# Docker 方式
docker run -d --name redis -p 6379:6379 redis:latest

# 或使用 Redis 配置文件
docker run -d --name redis -p 6379:6379 -v /path/to/redis.conf:/usr/local/etc/redis/redis.conf redis:latest redis-server /usr/local/etc/redis/redis.conf
```

### 2. 执行数据库迁移

```bash
psql -U postgres -d real-agent -f docs/SQL_USER_TABLE_MIGRATION.sql
```

### 3. 更新配置文件

修改 `application-local.yml`：
- Redis 连接信息
- PostgreSQL 连接信息
- JWT 密钥（生产环境必须修改）

### 4. 启动应用

```bash
mvn clean install
mvn spring-boot:run -pl real-agent-web
```

### 5. 测试认证

```bash
# 注册用户
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"externalId":"test","password":"123456","nickname":"测试"}'

# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"externalId":"test","password":"123456"}'

# 获取用户信息
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 十、安全建议

### 1. 生产环境配置

- ✅ **修改 JWT 密钥**：使用强随机密钥（至少 256 位）
- ✅ **启用 HTTPS**：所有认证接口必须使用 HTTPS
- ✅ **Redis 密码**：生产环境 Redis 必须设置密码
- ✅ **限流**：登录接口添加限流保护
- ✅ **验证码**：连续失败后要求验证码

### 2. Token 安全

- ✅ **短期有效**：Access Token 有效期不超过 2 小时
- ✅ **刷新机制**：使用 Refresh Token 刷新 Access Token
- ✅ **黑名单**：登出时将 Token 加入黑名单
- ✅ **单点登录**：一个用户只能有一个有效 Token

### 3. 密码安全

- ✅ **BCrypt 加密**：使用 BCrypt 算法加密密码
- ✅ **密码强度**：要求密码长度至少 6 位
- ✅ **防暴力破解**：限制登录尝试次数

---

## 十一、故障排查

### 1. Redis 连接失败

**问题**：`Unable to connect to Redis`

**解决**：
- 检查 Redis 是否启动：`redis-cli ping`
- 检查端口是否正确：默认 6379
- 检查防火墙规则

### 2. Token 验证失败

**问题**：`Token validation failed`

**解决**：
- 检查 JWT 密钥是否一致
- 检查 Token 是否过期
- 检查 Token 是否在黑名单中

### 3. 用户登录失败

**问题**：`Authentication failed`

**解决**：
- 检查数据库连接
- 检查用户是否存在
- 检查密码是否正确
- 检查用户状态是否正常

---

## 十二、性能优化

### 1. Redis 连接池

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 16    # 增加最大连接数
          max-idle: 8
          min-idle: 2
```

### 2. Token 缓存策略

- Access Token 缓存 2 小时
- Refresh Token 缓存 7 天
- 用户信息缓存 1 小时

### 3. 数据库索引

```sql
CREATE INDEX idx_users_external_id ON app_user.users(external_id);
CREATE INDEX idx_users_status ON app_user.users(status);
```

---

## 十三、监控和日志

### 1. Redis 监控

```bash
# 查看 Redis 信息
redis-cli INFO

# 查看所有 Key
redis-cli KEYS "auth:*"

# 查看 Key 过期时间
redis-cli TTL "auth:token:xxx"
```

### 2. 应用日志

```yaml
logging:
  level:
    com.ai.agent.real.application.service.auth: DEBUG
    com.ai.agent.real.web.filter: DEBUG
```

---

## 十四、总结

### 核心优势

1. ✅ **无状态认证**：JWT Token，支持分布式部署
2. ✅ **高性能**：Redis 缓存，减少数据库查询
3. ✅ **响应式**：完全兼容 WebFlux 架构
4. ✅ **安全可靠**：BCrypt 加密 + Token 黑名单
5. ✅ **易于扩展**：支持单点登录、多设备管理

### 下一步计划

1. 添加验证码功能
2. 实现第三方登录（OAuth2）
3. 添加权限控制（RBAC）
4. 实现多设备管理
5. 添加审计日志

---

**文档版本**：v1.0  
**最后更新**：2025-01-23
