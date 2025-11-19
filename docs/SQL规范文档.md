PostgreSQL 中数据库对象（数据库、Schema、表、字段等）的命名规范虽无强制语法约束，但遵循一致的命名规则能提升代码可读性、维护性，避免潜在冲突（如与关键字重名）。以下是业界通用的命名规范，结合 PostgreSQL 特性整理：


### 一、通用基础规则（所有对象适用）
1. **避免关键字**  
   不使用 PostgreSQL 关键字（如 `user`、`table`、`select`、`order` 等）作为名称，若必须使用，需用双引号 `""` 包裹（不推荐，易引发语法问题）。  
   - 错误示例：`CREATE TABLE user (...);`（`user` 是关键字）  
   - 正确示例：`CREATE TABLE "user" (...);`（不推荐）或 `CREATE TABLE app_user (...);`（推荐）。

2. **大小写敏感处理**  
   PostgreSQL 对未加双引号的名称**默认转为小写**（大小写不敏感），加双引号则严格区分大小写（不推荐，易导致混淆）。  
   - 建议统一使用小写，避免双引号。例如：`mydb` 而非 `MyDB` 或 `"MyDB"`。

3. **长度限制**  
   名称最长为 63 个字符（超过会被截断），建议简洁明了（如不超过 30 字符），避免过长名称（如 `this_is_a_very_long_table_name_for_user_info`）。

4. **字符范围**  
   允许使用：字母（`a-z`）、数字（`0-9`）、下划线（`_`），且**不能以数字开头**。  
   - 错误示例：`123_table`、`user-name`（含连字符 `-`，会被解析为减号）。  
   - 正确示例：`table_123`、`user_info`。


### 二、数据库（Database）命名规范
- **用途优先**：以业务或项目名称命名，体现数据库的核心功能。  
  - 示例：`ecommerce`（电商系统）、`blog_platform`（博客平台）、`user_center`（用户中心）。  
- **避免缩写**：除非是广为人知的缩写（如 `erp`、`crm`），否则用完整单词。  
  - 不推荐：`ec_db`（缩写不明确）；推荐：`ecommerce_db`（可选加 `_db` 后缀，非必需）。  
- **命名风格**：全小写 + 下划线分隔（snake_case）。  


### 三、Schema 命名规范
Schema 用于隔离数据库内的对象（如区分不同模块、团队或环境），命名需体现其“命名空间”的作用：  
- **按模块/功能划分**：以业务模块命名，如 `user`（用户相关）、`order`（订单相关）、`product`（商品相关）。  
- **按团队/角色划分**：多团队协作时，用团队名区分，如 `marketing`（市场团队）、`backend`（后端团队）。  
- **按环境划分**：测试/生产环境在同一数据库时（不推荐，建议用独立数据库），可加前缀，如 `dev_`、`test_`、`prod_`。  
- **命名风格**：全小写 + 下划线分隔，避免过长（如 `user` 而非 `user_management_schema`）。  
- 特殊说明：默认 `public` Schema 建议仅用于临时测试，正式环境尽量使用自定义 Schema 隔离对象。  


### 四、表（Table）命名规范
- **实体名称**：以表存储的核心实体命名，用名词复数形式（体现“集合”概念）。  
  - 示例：`users`（用户表）、`orders`（订单表）、`products`（商品表）。  
- **关联表命名**：多对多关系的关联表，用两个主表名称的复数形式 + 下划线连接，按字母顺序排列（避免混乱）。  
  - 示例：`products_categories`（商品-分类关联表）、`users_roles`（用户-角色关联表）。  
- **前缀区分**：如需区分表类型（如临时表、历史表），可加前缀：  
  - 临时表：`tmp_` 前缀，如 `tmp_user_import`（导入临时表）。  
  - 历史表：`his_` 前缀，如 `his_orders`（订单历史表）。  
- **命名风格**：全小写 + 下划线分隔，避免缩写（除非明确），如 `user_addresses` 而非 `usr_addr`。  


### 五、字段（Column）命名规范
- **属性描述**：以字段存储的属性命名，用名词单数形式。  
  - 示例：`id`（主键）、`name`（名称）、`create_time`（创建时间）。  
- **主键命名**：统一用 `id`（表内唯一），或 `表名_id`（关联字段，如 `user_id` 表示关联 `users` 表的主键）。  
- **时间字段**：统一用 `create_time`（创建时间）、`update_time`（更新时间），避免 `created_at`、`updated_on` 等混合风格。  
- **布尔字段**：以 `is_` 前缀开头，明确表示“是否”，如 `is_active`（是否激活）、`is_deleted`（是否删除）。  
- **避免冗余**：字段名不重复表名信息，如 `users` 表中用 `name` 而非 `user_name`。  
- **命名风格**：全小写 + 下划线分隔，如 `phone_number` 而非 `phoneNumber`（驼峰式不推荐）。  


### 六、其他对象命名规范
1. **索引（Index）**  
   - 格式：`idx_表名_字段名`（单字段索引）或 `idx_表名_字段1_字段2`（复合索引）。  
   - 示例：`idx_users_email`（`users` 表 `email` 字段索引）、`idx_orders_user_id_create_time`（复合索引）。  

2. **函数/存储过程（Function/Procedure）**  
   - 格式：`动作_对象_描述`，用动词开头，明确功能。  
   - 示例：`get_user_by_id`（通过 ID 获取用户）、`calculate_order_total`（计算订单总额）。  

3. **视图（View）**  
   - 格式：`v_表名` 或 `v_功能描述`，加 `v_` 前缀区分表和视图。  
   - 示例：`v_active_users`（活跃用户视图）、`v_order_details`（订单详情视图）。  

4. **序列（Sequence）**  
   - 格式：`表名_字段名_seq`，明确关联的表和字段。  
   - 示例：`users_id_seq`（`users` 表 `id` 字段的序列）。  


### 七、总结
PostgreSQL 命名的核心原则是：**清晰、一致、无歧义**。推荐统一使用 **snake_case（全小写 + 下划线分隔）**，避免关键字和大小写混淆，按对象类型（数据库、Schema、表等）制定针对性规则。这样既能提升团队协作效率，也能减少因命名引发的语法错误（如关键字冲突）。

例如，一个规范的电商数据库结构可能如下：  
- 数据库：`ecommerce`  
- Schema：`user`、`order`、`product`  
- 表（`user` Schema）：`users`、`user_addresses`  
- 字段（`users` 表）：`id`、`username`、`email`、`create_time`、`is_active`  
- 索引：`idx_users_email`、`idx_user_addresses_user_id`