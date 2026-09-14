## blog-user-services
provide users, roles and permissions services

### 服务概览

本服务基于 RBAC 模型提供用户、角色、权限及关联管理，模块划分如下：

| 模块 | 职责 |
|------|------|
| `users` | 用户 CRUD |
| `roles` | 角色 CRUD |
| `permissions` | 权限 CRUD |
| `userroles` | 用户 ↔ 角色关联（分配 / 查询 / 移除） |
| `rolepermissions` | 角色 ↔ 权限关联（分配 / 查询 / 移除） |

存储层使用 MyBatis Plus + MySQL，应用启动时自动执行 `classpath:schema.sql` 幂等建表。

### API 一览

#### 用户 `/api/users`
- `GET    /api/users`            查询用户列表
- `GET    /api/users/{id}`      查询单个用户
- `POST   /api/users`           创建用户
- `PUT    /api/users/{id}`      更新用户
- `DELETE /api/users/{id}`      删除用户

#### 角色 `/api/roles`
- `GET    /api/roles`                       查询角色列表
- `GET    /api/roles/{id}`                  查询单个角色
- `POST   /api/roles`                       创建角色
- `PUT    /api/roles/{id}`                  更新角色
- `DELETE /api/roles/{id}`                  删除角色

#### 权限 `/api/permissions`
- `GET    /api/permissions`          查询权限列表
- `GET    /api/permissions/{id}`     查询单个权限
- `POST   /api/permissions`          创建权限
- `PUT    /api/permissions/{id}`     更新权限
- `DELETE /api/permissions/{id}`     删除权限

#### 用户-角色关联 `/api/users/{userId}/roles`
- `GET    /api/users/{userId}/roles`              查询用户的角色列表
- `POST   /api/users/{userId}/roles`             给用户分配角色（批量），body: `{"roleIds":[1,2]}`
- `DELETE /api/users/{userId}/roles/{roleId}`    移除用户的单个角色

#### 角色-权限关联 `/api/roles/{roleId}/permissions`
- `GET    /api/roles/{roleId}/permissions`                    查询角色的权限列表
- `POST   /api/roles/{roleId}/permissions`                   给角色分配权限（批量），body: `{"permissionIds":[1,2]}`
- `DELETE /api/roles/{roleId}/permissions/{permissionId}`    移除角色的单个权限

### 数据库准备

数据源连接 `jdbc:mysql://localhost:3306/blog_db`（root 空密码，见 `application.properties`）。

首次启动前需先创建库（建表由应用启动时自动执行 `schema.sql`）：

```sql
CREATE DATABASE IF NOT EXISTS blog_db DEFAULT CHARACTER SET utf8mb4;
```

### 运行测试

执行所有单元测试（Mockito mock Mapper 层，不依赖数据库）：

```bash
mvn test
```

执行指定测试类：

```bash
mvn test -Dtest=UserServiceImplTest,RoleServiceImplTest,PermissionServiceImplTest,UserRoleServiceImplTest,RolePermissionServiceImplTest
```

测试覆盖 5 个 ServiceImpl 的全部 CRUD 与关联逻辑边界场景，共 44 个用例。

### 启动服务

编译并启动（默认端口 8081）：

```bash
mvn spring-boot:run
```

或打包后运行：

```bash
mvn clean package
java -jar target/services-0.0.1-SNAPSHOT.war
```

启动成功后访问 API 示例：

```bash
# 创建角色
curl -X POST http://localhost:8080/api/roles \
  -H "Content-Type: application/json" \
  -d '{"roleName":"管理员","roleCode":"ROLE_ADMIN","description":"系统管理员"}'

# 给角色分配权限
curl -X POST http://localhost:8080/api/roles/1/permissions \
  -H "Content-Type: application/json" \
  -d '{"permissionIds":[1,2]}'
```

Postman 测试集合位于 `postman/` 目录，可直接导入使用。

### Spring Cloud（Nacos 微服务）

本项目基于 Spring Cloud Alibaba 接入 Nacos，作为单个微服务（`blog-service`）注册到 Nacos 注册中心，数据源等配置由 Nacos 配置中心统一管理。

#### 版本矩阵

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17 (LTS) | 基础运行环境 |
| Spring Boot | 3.2.4 | 基础框架 |
| Spring Cloud | 2023.0.1 | 适配 Boot 3.2.x |
| Spring Cloud Alibaba | 2023.0.1.0 | 适配 Spring Cloud 2023.0.1 |
| Nacos Server | 2.3.2 | 注册中心 + 配置中心 |

#### 启动 Nacos

```bash
# Windows standalone 模式
startup.cmd -m standalone

# 默认地址 http://localhost:8848/nacos，账号/密码 nacos/nacos
```

#### 创建 Nacos 配置

在 Nacos 控制台 → 配置管理 → 配置列表，创建配置：
- **Data ID**：`blog-service-dev.yaml`
- **Group**：`DEFAULT_GROUP`
- **内容**：数据源 + MyBatis Plus + SQL 初始化配置（见下方）

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/blog_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
```

> `application.yml` 中 `spring.config.import: optional:nacos:blog-service-dev.yaml` 会从 Nacos 拉取此配置。`optional:` 前缀表示 Nacos 不可用时不阻断启动（用本地兜底配置）。修改 Nacos 中的配置可动态刷新，无需重启服务。

#### 启动微服务

```bash
mvn spring-boot:run
```

启动后日志应出现 `Nacos Discovery: register` 注册信息。Nacos 控制台 → 服务管理 → 服务列表可见 `blog-service` 实例（端口 8081）。

```bash
# 验证业务接口（端口改为 8081）
curl http://localhost:8081/api/roles
```

### 依赖
- spring-boot-starter-web
- spring-cloud-starter-alibaba-nacos-discovery
- spring-cloud-starter-alibaba-nacos-config
- fastjson
- commons-lang3
- mybatis-plus-boot-starter
- druid-spring-boot-3-starter
- mysql-connector-j
- spring-boot-starter-tomcat
- junit
- ...