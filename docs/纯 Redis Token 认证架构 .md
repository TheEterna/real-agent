# 纯 Redis Token 认证架构

## 一、为什么不用 JWT？

### JWT + Redis 方案的问题
1. ❌ **JWT 解析开销** - 每次请求都要解析 JWT
2. ❌ **Token 无法主动失效** - 需要维护黑名单
3. ❌ **管理员踢人下线复杂** - 需要额外的黑名单机制
4. ❌ **过度设计** - 既然用了 Redis，JWT 就是多余的

### 纯 Redis Token 方案的优势
1. ✅ **简单直接** - Token 就是随机字符串
2. ✅ **Token 可以随时删除** - 管理员踢人下线只需删除 Redis Key
3. ✅ **Redis 自动过期** - 不需要手动管理过期时间
4. ✅ **查看在线用户** - 可以轻松查询所有在线用户
5. ✅ **单点登录** - 一个用户只能有一个有效 Token
6. ✅ **性能更好** - 不需要 JWT 解析

---

## 二、架构设计

### Token 生成策略

使用 **SecureRandom + Base64** 生成随机 Token：

```java
public static String generateAccessToken() {
    byte[] randomBytes = new byte[32];
    RANDOM.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
}
```

**生成的 Token 示例**：
```
kJ8vN2mP9qR3sT5uW7xY0zA1bC4dE6fG8hI9jK0lM2nO4pQ6rS8tU0vW2xY4zA6b
```

### Redis 数据结构

#### 1. Token → UserContext 映射
```
Key:   auth:token:{token}
Value: UserContext (JSON)
TTL:   2 hours (Access Token) / 7 days (Refresh Token)
```

**示例**：
```json
{
  "userId": 123,
  "externalId": "user_001",
  "nickname": "张三",
  "avatarUrl": "https://..."
}
```

#### 2. UserId → Token 映射（单点登录）
```
Key:   auth:user:token:{userId}
Value: Token (String)
TTL:   2 hours
```

**用途**：
- 实现单点登录（一个用户只能有一个有效 Token）
- 管理员踢人下线

#### 3. 在线用户列表
```
Key:   auth:online:users:{userId}
Value: {token, loginTime}
TTL:   2 hours
```

**用途**：
- 查看所有在线用户
- 统计在线人数

---

## 三、认证流程

### 1. 登录流程

```
用户登录
  ↓
验证用户名密码（PostgreSQL）
  ↓
生成随机 Token（32字节）
  ↓
存储到 Redis：
  - auth:token:{token} → UserContext
  - auth:user:token:{userId} → token
  - auth:online:users:{userId} → {token, loginTime}
  ↓
返回 Token 给前端
```

**代码示例**：
```java
public Mono<TokenPair> generateTokenPair(User user) {
    String accessToken = TokenGenerator.generateAccessToken();
    String refreshToken = TokenGenerator.generateRefreshToken();
    
    UserContext userContext = UserContext.builder()
        .userId(user.getId())
        .externalId(user.getExternalId())
        .nickname(user.getNickname())
        .avatarUrl(user.getAvatarUrl())
        .build();
    
    return storeAccessToken(accessToken, userContext)
        .then(storeRefreshToken(refreshToken, userContext))
        .then(addToOnlineUsers(user.getId(), accessToken))
        .thenReturn(new TokenPair(accessToken, refreshToken, 7200L));
}
```

### 2. 请求认证流程

```
HTTP 请求（Header: Authorization: Bearer {token}）
  ↓
AuthenticationFilter 拦截
  ↓
提取 Token
  ↓
从 Redis 查询：auth:token:{token}
  ↓
存在？
  ├─ 是 → 获取 UserContext → 注入到 Reactor Context → 继续处理
  └─ 否 → 匿名访问（或返回 401）
```

**代码示例**：
```java
public Mono<UserContext> validateToken(String token) {
    String key = TOKEN_PREFIX + token;
    return redisService.get(key, UserContext.class);
}
```

### 3. 登出流程

```
用户登出
  ↓
从 Redis 获取用户信息
  ↓
删除 Redis Key：
  - auth:token:{token}
  - auth:user:token:{userId}
  - auth:online:users:{userId}
  ↓
登出成功
```

**代码示例**：
```java
public Mono<Void> logout(String token) {
    return validateToken(token).flatMap(userContext -> {
        return redisService.delete(TOKEN_PREFIX + token)
            .then(redisService.delete(USER_TOKEN_PREFIX + userContext.getUserId()))
            .then(redisService.delete(ONLINE_USERS_PREFIX + ":" + userContext.getUserId()));
    }).then();
}
```

### 4. 管理员踢人下线

```
管理员操作：踢用户下线
  ↓
根据 userId 获取 Token
  ↓
删除 Redis Key：
  - auth:token:{token}
  - auth:user:token:{userId}
  - auth:online:users:{userId}
  ↓
用户被强制下线
```

**代码示例**：
```java
public Mono<Void> kickUser(Long userId) {
    return redisService.get(USER_TOKEN_PREFIX + userId, String.class)
        .flatMap(token -> {
            return redisService.delete(TOKEN_PREFIX + token)
                .then(redisService.delete(USER_TOKEN_PREFIX + userId))
                .then(redisService.delete(ONLINE_USERS_PREFIX + ":" + userId));
        }).then();
}
```

---

## 四、核心代码

### 1. TokenGenerator（Token 生成器）

```java
public class TokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    
    public static String generateAccessToken() {
        return generateRandomToken(32);
    }
    
    public static String generateRefreshToken() {
        return generateRandomToken(32);
    }
    
    private static String generateRandomToken(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
```

### 2. TokenService（Token 服务）

```java
@Service
public class TokenService {
    private final RedisService redisService;
    
    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String USER_TOKEN_PREFIX = "auth:user:token:";
    private static final String ONLINE_USERS_PREFIX = "auth:online:users";
    
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofHours(2);
    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);
    
    // 生成 Token
    public Mono<TokenPair> generateTokenPair(User user) { ... }
    
    // 验证 Token
    public Mono<UserContext> validateToken(String token) { ... }
    
    // 刷新 Token
    public Mono<TokenPair> refreshAccessToken(String refreshToken) { ... }
    
    // 登出
    public Mono<Void> logout(String token) { ... }
    
    // 踢人下线
    public Mono<Void> kickUser(Long userId) { ... }
    
    // 获取在线用户
    public Flux<Long> getOnlineUsers() { ... }
}
```

### 3. AuthenticationFilter（认证过滤器）

```java
@Component
public class AuthenticationFilter implements WebFilter {
    private final TokenService tokenService;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = extractToken(exchange.getRequest());
        
        if (token == null) {
            return chain.filter(exchange); // 匿名访问
        }
        
        return tokenService.validateToken(token)
            .flatMap(user -> {
                // 注入用户上下文
                return chain.filter(exchange)
                    .contextWrite(ctx -> UserContextHolder.setUser(ctx, user));
            })
            .switchIfEmpty(chain.filter(exchange)); // Token 无效，匿名访问
    }
    
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

---

## 五、与 JWT 方案对比

| 特性 | JWT + Redis | 纯 Redis Token |
|------|------------|----------------|
| Token 生成 | 复杂（签名、加密） | 简单（随机字符串） |
| Token 验证 | 需要解析 JWT | 直接查 Redis |
| Token 失效 | 需要黑名单 | 直接删除 Key |
| 踢人下线 | 复杂（黑名单） | 简单（删除 Key） |
| 在线用户 | 难以实现 | 轻松实现 |
| 性能 | 较差（解析开销） | 更好（直接查询） |
| 依赖 | JWT 库 + Redis | 仅 Redis |
| 代码复杂度 | 高 | 低 |

---

## 六、安全性

### 1. Token 安全

- ✅ **随机性强** - 使用 SecureRandom 生成 32 字节随机数
- ✅ **不可预测** - Base64 编码后的 Token 无法被猜测
- ✅ **自动过期** - Redis TTL 自动管理过期
- ✅ **可主动失效** - 随时删除 Token

### 2. 传输安全

- ✅ **HTTPS** - 生产环境必须使用 HTTPS
- ✅ **HttpOnly Cookie**（可选）- 防止 XSS 攻击
- ✅ **CORS 配置** - 限制跨域访问

### 3. 存储安全

- ✅ **Redis 密码** - 生产环境 Redis 必须设置密码
- ✅ **Redis 持久化** - 配置 RDB 或 AOF 持久化
- ✅ **Redis 集群** - 高可用部署

---

## 七、管理员功能

### 1. 踢用户下线

```java
@PostMapping("/admin/kick-user/{userId}")
public Mono<Map<String, Object>> kickUser(@PathVariable Long userId) {
    return tokenService.kickUser(userId)
        .thenReturn(Map.of("success", true, "message", "用户已被踢下线"));
}
```

### 2. 查看在线用户

```java
@GetMapping("/admin/online-users")
public Flux<Long> getOnlineUsers() {
    return tokenService.getOnlineUsers();
}
```

### 3. 查看用户登录信息

```java
@GetMapping("/admin/user-login-info/{userId}")
public Mono<Map<String, Object>> getUserLoginInfo(@PathVariable Long userId) {
    return redisService.get(ONLINE_USERS_PREFIX + ":" + userId, Map.class)
        .map(info -> Map.of("success", true, "data", info));
}
```

---

## 八、前端集成

### 1. 登录

```javascript
const response = await axios.post('/api/auth/login', {
  externalId: 'user_001',
  password: '123456'
})

// 存储 Token
localStorage.setItem('accessToken', response.data.data.accessToken)
localStorage.setItem('refreshToken', response.data.data.refreshToken)
```

### 2. 请求携带 Token

```javascript
axios.defaults.headers.common['Authorization'] = `Bearer ${localStorage.getItem('accessToken')}`
```

### 3. Token 过期自动刷新

```javascript
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken')
      const response = await axios.post('/api/auth/refresh', { refreshToken })
      
      const newAccessToken = response.data.data.accessToken
      localStorage.setItem('accessToken', newAccessToken)
      
      // 重试原请求
      error.config.headers.Authorization = `Bearer ${newAccessToken}`
      return axios(error.config)
    }
    return Promise.reject(error)
  }
)
```

---

## 九、性能优化

### 1. Redis 连接池

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
```

### 2. Token 缓存策略

- Access Token: 2 小时
- Refresh Token: 7 天
- 在线用户列表: 2 小时（随 Access Token 过期）

### 3. 批量操作

使用 Redis Pipeline 批量删除 Token：

```java
public Mono<Void> logoutBatch(List<String> tokens) {
    return Flux.fromIterable(tokens)
        .flatMap(token -> redisService.delete(TOKEN_PREFIX + token))
        .then();
}
```

---

## 十、监控和运维

### 1. Redis 监控

```bash
# 查看所有 Token
redis-cli KEYS "auth:token:*"

# 查看在线用户数
redis-cli KEYS "auth:online:users:*" | wc -l

# 查看某个用户的 Token
redis-cli GET "auth:user:token:123"
```

### 2. 清理过期 Token

Redis 会自动清理过期的 Key，无需手动清理。

### 3. 日志记录

```java
@Slf4j
@Service
public class TokenService {
    public Mono<TokenPair> generateTokenPair(User user) {
        log.info("生成 Token: userId={}, externalId={}", user.getId(), user.getExternalId());
        // ...
    }
    
    public Mono<Void> kickUser(Long userId) {
        log.warn("管理员踢用户下线: userId={}", userId);
        // ...
    }
}
```

---

## 十一、总结

### 核心优势

1. ✅ **简单** - 不需要 JWT，代码更简洁
2. ✅ **高效** - 直接查 Redis，性能更好
3. ✅ **灵活** - Token 可以随时删除，管理员可以踢人
4. ✅ **实用** - 支持单点登录、在线用户查询等功能

### 适用场景

- ✅ 已经使用 Redis 的项目
- ✅ 需要管理员踢人功能
- ✅ 需要查看在线用户
- ✅ 需要单点登录控制
- ✅ 追求简单和高效

### 不适用场景

- ❌ 不想依赖 Redis
- ❌ 需要完全无状态（但这种场景很少）
- ❌ 需要跨系统认证（但可以共享 Redis）

---

## 十二、迁移指南

### 从 JWT 迁移到纯 Redis Token

1. **移除 JWT 依赖**
   ```xml
   <!-- 删除 pom.xml 中的 JWT 依赖 -->
   ```

2. **替换 JwtUtil**
   ```java
   // 删除 JwtUtil.java
   // 使用 TokenGenerator.java
   ```

3. **重构 TokenService**
   ```java
   // 移除所有 JWT 相关代码
   // 使用 Redis 直接存储 Token
   ```

4. **更新配置文件**
   ```yaml
   # 删除 JWT 配置
   # jwt:
   #   secret: ...
   ```

5. **测试验证**
   - 登录功能
   - Token 验证
   - 登出功能
   - 管理员踢人

---

**文档版本**：v1.0  
**最后更新**：2025-01-23  
**推荐指数**：⭐⭐⭐⭐⭐
