# Session模块说明

## 概述

Session模块实现了用户会话的持久化管理功能，包括会话的创建、查询、更新和删除操作。该模块遵循项目的DDD架构设计，分为entity、repository、service和controller四层。

## 数据库表结构

```sql
CREATE TABLE context.sessions
(
    id         uuid default uuid_generate_v7() not null,
    title      varchar(50),
    type       varchar(20),
    user_id    uuid,
    start_time timestamp with time zone
);

comment on table context.sessions is '会话表';

comment on column context.sessions.id is '主键';

comment on column context.sessions.title is ' 会话的标题';

comment on column context.sessions.type is '会话的类型';

comment on column context.sessions.user_id is '所属用户ID';

comment on column context.sessions.start_time is '会话创建时间';

alter table context.sessions
    owner to postgres;

create index sessions_user_id_index
    on context.sessions (user_id);

create index sessions_start_time_index
    on context.sessions (start_time);
```

## 模块结构

### Entity层
- `Session.java`: 会话实体类，对应数据库表结构

### Repository层
- `SessionRepository.java`: 会话数据访问接口，提供基本的CRUD操作和自定义查询方法

### Service层
- `SessionService.java`: 会话业务逻辑实现类
- `ISessionService.java`: 会话服务接口，定义了业务方法的契约

### Controller层
- `SessionController.java`: 会话REST API控制器，提供HTTP接口

## API接口

### 创建会话
```
POST /api/sessions
```

### 获取当前用户的所有会话
```
GET /api/sessions
```

### 根据ID获取会话
```
GET /api/sessions/{id}
```

### 更新会话
```
PUT /api/sessions/{id}
```

### 删除会话
```
DELETE /api/sessions/{id}
```

## 安全性

所有API接口都通过UserContextHolder获取当前用户信息，确保用户只能访问属于自己的会话数据。

## 测试

提供了SessionService的单元测试，使用Mockito框架进行模拟测试。