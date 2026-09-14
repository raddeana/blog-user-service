# Spring Cloud 重构计划（单体接入 Nacos）

## Context（背景）

当前项目是 Spring Boot 3.5.7 单体应用（5 个业务模块：users/roles/permissions/userroles/rolepermissions），使用 MySQL + MyBatis Plus 持久化。用户要求用 Spring Cloud 重构，明确选择：**Spring Cloud Alibaba (Nacos)** + **整个项目作为一个微服务（不拆分）** + **共享 blog_db** + **完整重构一次到位**。

即：不拆分业务、不加 Gateway、不做多模块，只将现有单体接入 Nacos 注册中心 + 配置中心，使其成为 Spring Cloud 生态中的一个微服务。业务代码零改动。

## 版本选型（已通过官方文档确认）

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.7（现有） | 不变 |
| Spring Cloud | 2025.0.0 | 适配 Boot 3.5.x |
| Spring Cloud Alibaba | 2025.0.0.0 | 适配 Spring Cloud 2025.0.0 |
| Nacos Server | 3.0.3 | 本机 standalone 模式 |

## 实现步骤

### 步骤 1：pom.xml 引入 Spring Cloud + SCA BOM 与依赖

修改 [pom.xml](file:///d:/github/blog-user-services/pom.xml)：

`<properties>` 增加：
```xml
<spring-cloud.version>2025.0.0</spring-cloud.version>
<spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>
```

`<dependencyManagement>` 增加（放在现有 dependencies 之前）：
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

`<dependencies>` 增加：
```xml
<!-- Nacos 服务注册发现 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<!-- Nacos 配置中心 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

### 步骤 2：application.properties 改为 application.yml + Nacos 配置

删除 [application.properties](file:///d:/github/blog-user-services/src/main/resources/application.properties)，新建 [application.yml](file:///d:/github/blog-user-services/src/main/resources/application.yml)：

```yaml
spring:
  application:
    name: blog-service
  profiles:
    active: dev
  # Nacos 配置中心：从 Nacos 拉取 blog-service-dev.yaml
  config:
    import:
      - optional:nacos:blog-service-${spring.profiles.active}.yaml
  cloud:
    nacos:
      # Nacos 服务端地址
      server-addr: localhost:8848
      # 注册发现
      discovery:
        namespace: public
        group: DEFAULT_GROUP
      # 配置中心
      config:
        namespace: public
        group: DEFAULT_GROUP
        file-extension: yaml
        # 关闭 Quickstart 自动加载提示（可选）
        enabled: true

# 本地兜底配置（Nacos 中同名配置会覆盖此处）
server:
  port: 8081

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

# 数据源（可放 Nacos 配置中心统一管理，此处兜底）
spring.datasource:
  type: com.alibaba.druid.pool.DruidDataSource
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://localhost:3306/blog_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
  username: root
  password:

# SQL 初始化（启动自动建表）
spring.sql.init:
  mode: always
  schema-locations: classpath:schema.sql
```

> 说明：Spring Cloud 2025.0.x 用 `spring.config.import` 替代旧版 bootstrap.yml，无需额外引入 spring-cloud-starter-bootstrap。

### 步骤 3：启动类加 @EnableDiscoveryClient

修改 [BlogServicesApplication.java](file:///d:/github/blog-user-services/src/main/java/com/blog/services/BlogServicesApplication.java)：

```java
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.blog.services.**.mappers")
public class BlogServicesApplication {
```

### 步骤 4：Nacos 配置中心创建配置（手动一次性操作）

启动 Nacos 后，在 Nacos 控制台创建 Data ID = `blog-service-dev.yaml`，Group = `DEFAULT_GROUP`，内容为数据源 + MyBatis 配置（即 application.yml 中兜底的那部分，可动态刷新）：

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

> 这样数据源配置集中到 Nacos 管理，修改后可热刷新无需重启。application.yml 仅保留 Nacos 连接 + 本地兜底。

### 步骤 5：更新 README

[README.md](file:///d:/github/blog-user-services/README.md) 补充 Spring Cloud 章节：
- 版本矩阵表
- Nacos 启动命令（standalone）
- Nacos 配置创建说明
- 服务启动方式（注册到 Nacos）
- 验证：Nacos 控制台服务列表可见 blog-service

## 不做的事

- 不拆分业务模块（用户明确要求整个项目作为一个微服务）
- 不加 Spring Cloud Gateway（单服务无需网关）
- 不做 Maven 多模块重构（保持单 pom）
- 不引入 OpenFeign / Sentinel（无跨服务调用）
- 不改动任何业务代码（5 个模块的 Entity/Mapper/ServiceImpl/Controller/VO/DTO 全部保留）
- 不改动单元测试（Mockito mock Mapper，不依赖 Spring 上下文）

## 验证方式

1. 启动本机 Nacos 3.0.3：`startup.cmd -m standalone`（Windows）
2. Nacos 控制台（http://localhost:8848/nacos，默认 nacos/nacos）创建 `blog-service-dev.yaml` 配置
3. `mvn compile` 编译通过
4. `mvn spring-boot:run` 启动服务，日志应出现 `Nacos Discovery: register` + 数据源初始化
5. Nacos 控制台 → 服务管理 → 服务列表，应可见 `blog-service` 实例（IP:8081）
6. `curl http://localhost:8081/api/roles` 正常返回（业务接口不变）
7. 在 Nacos 修改数据源配置，验证动态刷新（日志可见配置变更）
8. `mvn test` 单元测试仍全部通过
