# OpenFeign 重构计划（单体+Feign接口）

## Context

当前项目是单体 Spring Boot 服务 `blog-service`，5 个业务模块（users/roles/permissions/userroles/rolepermissions）在同一 JVM 内通过直接 Service/Mapper 调用协作。关联服务 `UserRoleServiceImpl` 直接注入 `UserService` + `RoleMapper`，`RolePermissionServiceImpl` 直接注入 `RoleService` + `PermissionMapper`。

目标：引入 OpenFeign，将关联服务对 User/Role/Permission 的直接依赖替换为 Feign HTTP 调用，保持单体运行但为未来拆分微服务做好准备。拆分时只需修改 `@FeignClient(name=...)` 即可，关联服务代码零改动。

## 改动清单

### 1. pom.xml — 添加 OpenFeign 依赖

在 `<dependencies>` 中添加（Spring Cloud 2023.0.1 BOM 已管理版本）：
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### 2. BlogUserServicesApplication.java — 启用 Feign

添加 `@EnableFeignClients` 注解。

### 3. 新建 Feign Client 接口（`com.blog.services.feign` 包）

3 个接口，镜像现有 Controller 端点，返回 `Result<T>` 统一响应：

**UserFeignClient.java**
```java
@FeignClient(name = "blog-service", path = "/api/users")
public interface UserFeignClient {
    @GetMapping("/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);
    // + listUsers, createUser, updateUser, deleteUser
}
```

**RoleFeignClient.java** — 同上，path = "/api/roles"，返回 `Result<RoleDTO>`
**PermissionFeignClient.java** — 同上，path = "/api/permissions"，返回 `Result<PermissionDTO>`

### 4. 重构 UserRoleServiceImpl

- **移除**：`UserService` 注入、`RoleMapper` 注入、`convertToDTO` 方法
- **新增**：`UserFeignClient` + `RoleFeignClient` 注入
- `getUserRoles`：遍历关联记录，通过 `roleFeignClient.getRoleById(roleId)` 获取 `Result<RoleDTO>`，解包取 data
- `assignRoles`：通过 `userFeignClient.getUserById(userId)` 校验用户存在，通过 `roleFeignClient.getRoleById(roleId)` 校验角色存在
- `removeRole`：不变（仅操作 userRoleMapper）

### 5. 重构 RolePermissionServiceImpl

- **移除**：`RoleService` 注入、`PermissionMapper` 注入、`convertToDTO` 方法
- **新增**：`RoleFeignClient` + `PermissionFeignClient` 注入
- `getRolePermissions`：通过 `permissionFeignClient.getPermissionById(id)` 获取 DTO
- `assignPermissions`：通过 `roleFeignClient.getRoleById(roleId)` 校验角色，通过 `permissionFeignClient.getPermissionById(id)` 校验权限
- `removePermission`：不变

### 6. 更新单元测试

- `UserRoleServiceImplTest`：mock `UserFeignClient` + `RoleFeignClient`（替代 `UserService` + `RoleMapper`），返回 `Result.success(dto)` 包装
- `RolePermissionServiceImplTest`：mock `RoleFeignClient` + `PermissionFeignClient`（替代 `RoleService` + `PermissionMapper`）

### 7. 验证

```bash
mvn compile          # 编译通过
mvn test             # 46 个测试全部通过
```

端到端验证（需启动 Nacos + MySQL）：
```bash
mvn spring-boot:run
curl http://localhost:8081/api/users/1/roles -X POST -H "Content-Type: application/json" -d '{"roleIds":[1,2]}'
```

## 不变的部分

- 所有 Controller、VO、DTO、Entity、Mapper、schema.sql 不变
- 3 个基础 Service（UserServiceImpl/RoleServiceImpl/PermissionServiceImpl）不变
- application.yml 不变（Nacos 注册发现已配置，Feign 自动通过服务名路由）
